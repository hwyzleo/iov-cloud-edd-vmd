package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VariantAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VariantOptionCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantOptionCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VariantQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VariantDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VariantOptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VariantOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehVariantRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 版本应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VariantAppService {

    private final VehVariantRepository vehVariantRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VehConfigurationRepository vehConfigurationRepository;
    private final OptionFamilyAppService optionFamilyAppService;

    // ==================== 版本 ====================

    /**
     * 查询版本信息
     *
     * @param query 查询 DTO
     * @return 版本列表
     */
    public List<VariantDto> search(VariantQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", query.getPlatformCode());
        map.put("carLineCode", query.getCarLineCode());
        map.put("modelCode", query.getModelCode());
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<Variant> variantList = vehVariantRepository.selectByMap(map);
        return PageUtil.convert(variantList, VariantAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查版本代码是否唯一
     *
     * @param variantId 版本ID
     * @param code      版本代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long variantId, String code) {
        if (ObjUtil.isNull(variantId)) {
            variantId = -1L;
        }
        Variant variant = getVariantByCode(code);
        return !ObjUtil.isNotNull(variant) || variant.getId().longValue() == variantId.longValue();
    }

    /**
     * 检查版本下是否存在车型配置
     *
     * @param variantId 版本ID
     * @return 结果
     */
    public Boolean checkVariantBuildConfigExist(Long variantId) {
        Variant variant = vehVariantRepository.selectById(variantId);
        Map<String, Object> map = new HashMap<>();
        map.put("variantCode", variant.getCode());
        return vehConfigurationRepository.countByMap(map) > 0;
    }

    /**
     * 检查版本下是否存在车辆
     *
     * @param variantId 版本ID
     * @return 结果
     */
    public Boolean checkVariantVehicleExist(Long variantId) {
        Variant variant = vehVariantRepository.selectById(variantId);
        Map<String, Object> map = new HashMap<>();
        map.put("variantCode", variant.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取版本信息
     *
     * @param id 主键ID
     * @return 版本信息
     */
    public VariantDto getVariantById(Long id) {
        return VariantAssembler.INSTANCE.fromDomain(vehVariantRepository.selectById(id));
    }

    /**
     * 根据版本代码获取版本信息
     *
     * @param code 版本代码
     * @return 版本领域对象
     */
    public Variant getVariantByCode(String code) {
        return vehVariantRepository.selectByCode(code);
    }

    /**
     * 新增版本
     *
     * @param variantCmd 版本信息
     * @return 结果
     */
    public int createVariant(VariantCmd variantCmd) {
        Variant variant = VariantAssembler.INSTANCE.toDomain(variantCmd);
        // 检查是否为 MDM 来源数据
        if (variant.getSource() == SourceType.MDM) {
            throw new ProductDataReadOnlyException("版本", variant.getCode());
        }
        return vehVariantRepository.insert(variant);
    }

    /**
     * 修改版本
     *
     * @param variantCmd 版本信息
     * @return 结果
     */
    public int modifyVariant(VariantCmd variantCmd) {
        Variant variant = vehVariantRepository.selectById(variantCmd.getId());
        // 检查是否为 MDM 来源数据
        if (variant != null && variant.getSource() == SourceType.MDM) {
            throw new ProductDataReadOnlyException("版本", variant.getCode());
        }
        Variant updateVariant = VariantAssembler.INSTANCE.toDomain(variantCmd);
        return vehVariantRepository.update(updateVariant);
    }

    /**
     * 批量删除版本
     *
     * @param ids 版本ID数组
     * @return 结果
     */
    public int deleteVariantByIds(Long[] ids) {
        // 检查是否为 MDM 来源数据
        for (Long id : ids) {
            Variant variant = vehVariantRepository.selectById(id);
            if (variant != null && variant.getSource() == SourceType.MDM) {
                throw new ProductDataReadOnlyException("版本", variant.getCode());
            }
        }
        return vehVariantRepository.batchPhysicalDelete(ids);
    }

    // ==================== 版本选项值 ====================

    /**
     * 查询版本选项值关系信息
     *
     * @param variantCode      版本编码
     * @param optionFamilyCode 选项族编码
     * @return 版本选项值关系列表
     */
    public List<VariantOptionCodeDto> searchOptionCode(String variantCode, String optionFamilyCode) {
        VariantOptionCode example = VariantOptionCode.builder()
                .variantCode(variantCode)
                .optionFamilyCode(optionFamilyCode)
                .build();
        List<VariantOptionCode> list = vehVariantRepository.selectOptionCodeByExample(example);
        List<VariantOptionCodeDto> dtoList = PageUtil.convert(list, VariantOptionCodeAssembler.INSTANCE::fromDomain);
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

    /**
     * 根据主键ID获取版本选项值关系信息
     *
     * @param id 主键ID
     * @return 版本选项值关系信息
     */
    public VariantOptionCodeDto getVariantOptionCodeById(Long id) {
        List<VariantOptionCode> list = vehVariantRepository.selectOptionCodeByExample(VariantOptionCode.builder().id(id).build());
        return list.isEmpty() ? null : VariantOptionCodeAssembler.INSTANCE.fromDomain(list.get(0));
    }

    /**
     * 检查版本选项值关系是否唯一
     *
     * @param id               主键ID
     * @param variantCode      版本编码
     * @param optionFamilyCode 选项族编码
     * @return 结果
     */
    public Boolean checkOptionCodeUnique(Long id, String variantCode, String optionFamilyCode) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        List<VariantOptionCode> list = vehVariantRepository.selectOptionCodeByExample(VariantOptionCode.builder()
                .variantCode(variantCode)
                .optionFamilyCode(optionFamilyCode)
                .build());
        return list.isEmpty() || list.get(0).getId().longValue() == id.longValue();
    }

    /**
     * 新增版本选项值关系
     *
     * @param optionCodeCmd 版本选项值关系信息
     * @return 结果
     */
    public int createVariantOptionCode(VariantOptionCodeCmd optionCodeCmd) {
        VariantOptionCode optionCode = VariantOptionCodeAssembler.INSTANCE.toDomain(optionCodeCmd);
        return vehVariantRepository.batchInsertOptionCode(List.of(optionCode));
    }

    /**
     * 修改版本选项值关系
     *
     * @param optionCodeCmd 版本选项值关系信息
     * @return 结果
     */
    public int modifyVariantOptionCode(VariantOptionCodeCmd optionCodeCmd) {
        VariantOptionCode optionCode = VariantOptionCodeAssembler.INSTANCE.toDomain(optionCodeCmd);
        return vehVariantRepository.updateOptionCode(optionCode);
    }

    /**
     * 批量删除版本选项值关系
     *
     * @param ids 主键ID数组
     * @return 结果
     */
    public int deleteVariantOptionCodeByIds(Long[] ids) {
        return vehVariantRepository.batchPhysicalDeleteOptionCode(ids);
    }

}
