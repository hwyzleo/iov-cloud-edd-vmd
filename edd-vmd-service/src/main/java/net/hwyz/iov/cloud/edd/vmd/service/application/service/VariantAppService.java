package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptFeatureAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BaseModelFeatureCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VariantAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BaseModelFeatureCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VariantQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BaseModelFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.FeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.FeatureFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VariantDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModelFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
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
    private final FeatureFamilyAppService featureFamilyAppService;

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

    // ==================== 版本特征 ====================

    /**
     * 查询版本特征关系信息
     *
     * @param variantCode 版本编码
     * @param familyCode  特征族编码
     * @return 版本特征关系列表
     */
    public List<BaseModelFeatureCodeDto> searchFeatureCode(String variantCode, String familyCode) {
        BaseModelFeatureCode example = BaseModelFeatureCode.builder()
                .variantCode(variantCode)
                .familyCode(familyCode)
                .build();
        List<BaseModelFeatureCode> list = vehVariantRepository.selectFeatureCodeByExample(example);
        List<BaseModelFeatureCodeDto> dtoList = PageUtil.convert(list, BaseModelFeatureCodeAssembler.INSTANCE::fromDomain);
        dtoList.forEach(dto -> {
            FeatureFamilyDto featureFamily = featureFamilyAppService.getFeatureFamilyByCode(dto.getFamilyCode());
            if (featureFamily != null) {
                dto.setFamilyName(featureFamily.getName());
            }
            if (dto.getFeatureCode() != null) {
                dto.setFeatureName(new String[dto.getFeatureCode().length]);
                int i = 0;
                for (String code : dto.getFeatureCode()) {
                    FeatureCodeDto featureCode = featureFamilyAppService.getFeatureCodeByCode(code);
                    if (featureCode != null) {
                        dto.getFeatureName()[i] = featureCode.getName();
                    }
                    i++;
                }
            }
        });
        return dtoList;
    }

    /**
     * 根据主键ID获取版本特征关系信息
     *
     * @param id 主键ID
     * @return 版本特征关系信息
     */
    public BaseModelFeatureCodeDto getVariantFeatureCodeById(Long id) {
        List<BaseModelFeatureCode> list = vehVariantRepository.selectFeatureCodeByExample(BaseModelFeatureCode.builder().id(id).build());
        return list.isEmpty() ? null : BaseModelFeatureCodeAssembler.INSTANCE.fromDomain(list.get(0));
    }

    /**
     * 检查版本特征关系是否唯一
     *
     * @param id           主键ID
     * @param variantCode  版本编码
     * @param familyCode   特征族编码
     * @return 结果
     */
    public Boolean checkFeatureCodeUnique(Long id, String variantCode, String familyCode) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        List<BaseModelFeatureCode> list = vehVariantRepository.selectFeatureCodeByExample(BaseModelFeatureCode.builder()
                .variantCode(variantCode)
                .familyCode(familyCode)
                .build());
        return list.isEmpty() || list.get(0).getId().longValue() == id.longValue();
    }

    /**
     * 新增版本特征关系
     *
     * @param featureCodeCmd 版本特征关系信息
     * @return 结果
     */
    public int createVariantFeatureCode(BaseModelFeatureCodeCmd featureCodeCmd) {
        BaseModelFeatureCode featureCode = BaseModelFeatureCodeAssembler.INSTANCE.toDomain(featureCodeCmd);
        return vehVariantRepository.batchInsertFeatureCode(List.of(featureCode));
    }

    /**
     * 修改版本特征关系
     *
     * @param featureCodeCmd 版本特征关系信息
     * @return 结果
     */
    public int modifyVariantFeatureCode(BaseModelFeatureCodeCmd featureCodeCmd) {
        BaseModelFeatureCode featureCode = BaseModelFeatureCodeAssembler.INSTANCE.toDomain(featureCodeCmd);
        return vehVariantRepository.updateFeatureCode(featureCode);
    }

    /**
     * 批量删除版本特征关系
     *
     * @param ids 主键ID数组
     * @return 结果
     */
    public int deleteVariantFeatureCodeByIds(Long[] ids) {
        return vehVariantRepository.batchPhysicalDeleteFeatureCode(ids);
    }

}
