package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigurationAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigurationOptionCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationOptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.ConfigurationQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationOptionCodeCmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationAppService {

    private final MdmConfigurationRepository mdmConfigurationRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final OptionFamilyAppService optionFamilyAppService;

    public List<ConfigurationDto> search(ConfigurationQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", query.getPlatformCode());
        map.put("carLineCode", query.getCarLineCode());
        map.put("modelCode", query.getModelCode());
        map.put("variantCode", query.getVariantCode());
        map.put("baseModelCode", query.getBaseModelCode());
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<Configuration> configurationList = mdmConfigurationRepository.selectByMap(map);
        return PageUtil.convert(configurationList, ConfigurationAssembler.INSTANCE::fromDomain);
    }

    public List<ConfigurationDto> getConfigurationListByVariantCode(String variantCode) {
        List<Configuration> configurationList = mdmConfigurationRepository.selectByExample(Configuration.builder()
                .variantCode(variantCode)
                .enable(true)
                .build());
        return PageUtil.convert(configurationList, ConfigurationAssembler.INSTANCE::fromDomain);
    }

    @Deprecated
    public List<ConfigurationDto> getConfigurationListByBaseModelCode(String baseModelCode) {
        List<Configuration> configurationList = mdmConfigurationRepository.selectByExample(Configuration.builder()
                .variantCode(baseModelCode)
                .enable(true)
                .build());
        return PageUtil.convert(configurationList, ConfigurationAssembler.INSTANCE::fromDomain);
    }

    public ConfigurationDto getConfigurationByCode(String code) {
        Configuration configuration = mdmConfigurationRepository.selectByCode(code);
        return ConfigurationAssembler.INSTANCE.fromDomain(configuration);
    }

    public Configuration getConfigurationEntityByCode(String code) {
        return mdmConfigurationRepository.selectByCode(code);
    }

    public Boolean checkCodeUnique(Long configurationId, String code) {
        if (ObjUtil.isNull(configurationId)) {
            configurationId = -1L;
        }
        Configuration configuration = getConfigurationEntityByCode(code);
        return !ObjUtil.isNotNull(configuration) || configuration.getId().longValue() == configurationId.longValue();
    }

    public Boolean checkConfigurationVehicleExist(Long configurationId) {
        Configuration configuration = mdmConfigurationRepository.selectById(configurationId);
        Map<String, Object> map = new HashMap<>();
        map.put("configurationCode", configuration.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    public ConfigurationDto getConfigurationById(Long id) {
        return ConfigurationAssembler.INSTANCE.fromDomain(mdmConfigurationRepository.selectById(id));
    }

    public int createConfiguration(ConfigurationCmd configurationCmd, String userId) {
        Configuration configuration = ConfigurationAssembler.INSTANCE.toDomain(configurationCmd);
        return mdmConfigurationRepository.insert(configuration);
    }

    public int modifyConfiguration(ConfigurationCmd configurationCmd, String userId) {
        Configuration configuration = ConfigurationAssembler.INSTANCE.toDomain(configurationCmd);
        return mdmConfigurationRepository.update(configuration);
    }

    public int deleteConfigurationByIds(Long[] ids) {
        return mdmConfigurationRepository.batchPhysicalDelete(ids);
    }

    public List<ConfigurationOptionCodeDto> searchOptionCode(String configurationCode, String optionFamilyCode) {
        ConfigurationOptionCode example = ConfigurationOptionCode.builder()
                .configurationCode(configurationCode)
                .optionFamilyCode(optionFamilyCode)
                .build();
        List<ConfigurationOptionCode> list = mdmConfigurationRepository.selectOptionCodeByExample(example);
        List<ConfigurationOptionCodeDto> dtoList = PageUtil.convert(list, ConfigurationOptionCodeAssembler.INSTANCE::fromDomain);
        dtoList.forEach(dto -> {
            OptionFamilyDto optionFamily = optionFamilyAppService.getOptionFamilyByCode(dto.getOptionFamilyCode());
            if (optionFamily != null) {
                dto.setOptionFamilyName(optionFamily.getName());
            }
            if (dto.getOptionCode() != null) {
                dto.setOptionName(new String[dto.getOptionCode().length]);
                int i = 0;
                for (String code : dto.getOptionCode()) {
                    OptionCodeDto optionCode = optionFamilyAppService.getOptionCodeByCode(code);
                    if (optionCode != null) {
                        dto.getOptionName()[i] = optionCode.getName();
                    }
                    i++;
                }
            }
        });
        return dtoList;
    }

    public ConfigurationOptionCodeDto getConfigurationOptionCodeById(Long id) {
        List<ConfigurationOptionCode> list = mdmConfigurationRepository.selectOptionCodeByExample(ConfigurationOptionCode.builder().id(id).build());
        return list.isEmpty() ? null : ConfigurationOptionCodeAssembler.INSTANCE.fromDomain(list.get(0));
    }

    public Boolean checkOptionCodeUnique(Long id, String configurationCode, String optionFamilyCode) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        List<ConfigurationOptionCode> list = mdmConfigurationRepository.selectOptionCodeByExample(ConfigurationOptionCode.builder()
                .configurationCode(configurationCode)
                .optionFamilyCode(optionFamilyCode)
                .build());
        return list.isEmpty() || list.get(0).getId().longValue() == id.longValue();
    }

    public int createConfigurationOptionCode(ConfigurationOptionCodeCmd optionCodeCmd) {
        ConfigurationOptionCode optionCode = ConfigurationOptionCodeAssembler.INSTANCE.toDomain(optionCodeCmd);
        return mdmConfigurationRepository.batchInsertOptionCode(List.of(optionCode));
    }

    public int modifyConfigurationOptionCode(ConfigurationOptionCodeCmd optionCodeCmd) {
        ConfigurationOptionCode optionCode = ConfigurationOptionCodeAssembler.INSTANCE.toDomain(optionCodeCmd);
        return mdmConfigurationRepository.updateOptionCode(optionCode);
    }

    public int deleteConfigurationOptionCodeByIds(Long[] ids) {
        return mdmConfigurationRepository.batchPhysicalDeleteOptionCode(ids);
    }

}
