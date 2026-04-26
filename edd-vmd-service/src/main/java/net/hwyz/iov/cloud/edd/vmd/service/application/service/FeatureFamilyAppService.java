package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureFamilyVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.FeatureCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.FeatureFamilyAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehFeatureFamilyRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆特征应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFamilyAppService {

    private final VehFeatureFamilyRepository vehFeatureFamilyRepository;

    // ==================== 特征族 ====================

    /**
     * 查询特征族信息
     *
     * @param code      特征族代码
     * @param name      特征族名称
     * @param type      特征族类型
     * @param beginTime 开始时间
     * @param endTime    结束时间
     * @return 特征族列表
     */
    public List<FeatureFamilyVo> search(String code, String name, String type, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("type", type);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<FeatureFamily> featureFamilyList = vehFeatureFamilyRepository.selectByMap(map);
        return PageUtil.convert(featureFamilyList, FeatureFamilyAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查特征族代码是否唯一
     *
     * @param featureFamilyId 特征族ID
     * @param code            特征族代码
     * @return 结果
     */
    public Boolean checkFamilyCodeUnique(Long featureFamilyId, String code) {
        if (ObjUtil.isNull(featureFamilyId)) {
            featureFamilyId = -1L;
        }
        FeatureFamily featureFamily = vehFeatureFamilyRepository.selectByCode(code);
        return !ObjUtil.isNotNull(featureFamily) || featureFamily.getId().longValue() == featureFamilyId.longValue();
    }

    /**
     * 检查特征值代码是否唯一
     *
     * @param featureCodeId 特征值ID
     * @param code          特征值代码
     * @return 结果
     */
    public Boolean checkFeatureCodeUnique(Long featureCodeId, String code) {
        if (ObjUtil.isNull(featureCodeId)) {
            featureCodeId = -1L;
        }
        FeatureCode featureCode = vehFeatureFamilyRepository.selectFeatureCodeByCode(code);
        return !ObjUtil.isNotNull(featureCode) || featureCode.getId().longValue() == featureCodeId.longValue();
    }

    /**
     * 根据主键ID获取特征族信息
     *
     * @param id 主键ID
     * @return 特征族信息
     */
    public FeatureFamilyVo getFeatureFamilyById(Long id) {
        return FeatureFamilyAssembler.INSTANCE.fromDomain(vehFeatureFamilyRepository.selectById(id));
    }

    /**
     * 根据特征族代码获取特征族信息
     *
     * @param code 特征族代码
     * @return 特征族信息
     */
    public FeatureFamilyVo getFeatureFamilyByCode(String code) {
        return FeatureFamilyAssembler.INSTANCE.fromDomain(vehFeatureFamilyRepository.selectByCode(code));
    }

    /**
     * 新增特征族
     *
     * @param featureFamily 特征族信息
     * @param userId        操作用户ID
     * @return 结果
     */
    public int createFeatureFamily(FeatureFamilyVo featureFamily, String userId) {
        FeatureFamily featureFamilyDomain = FeatureFamilyAssembler.INSTANCE.toDomain(featureFamily);
        return vehFeatureFamilyRepository.insert(featureFamilyDomain);
    }

    /**
     * 修改特征族
     *
     * @param featureFamily 特征族信息
     * @param userId        操作用户ID
     * @return 结果
     */
    public int modifyFeatureFamily(FeatureFamilyVo featureFamily, String userId) {
        FeatureFamily featureFamilyDomain = FeatureFamilyAssembler.INSTANCE.toDomain(featureFamily);
        return vehFeatureFamilyRepository.update(featureFamilyDomain);
    }

    /**
     * 批量删除特征族
     *
     * @param ids 特征族ID数组
     * @return 结果
     */
    public int deleteFeatureFamilyByIds(Long[] ids) {
        return vehFeatureFamilyRepository.batchPhysicalDelete(ids);
    }

    // ==================== 特征值 ====================

    /**
     * 查询特征值信息
     *
     * @param featureFamilyId 特征族ID
     * @param familyCode      特征族代码
     * @param name            特征值名称
     * @param featureCode     特征值代码
     * @param beginTime       开始时间
     * @param endTime         结束时间
     * @return 特征值列表
     */
    public List<FeatureCodeVo> searchFeatureCode(Long featureFamilyId, String familyCode, String name, String featureCode, Date beginTime, Date endTime) {
        // 实现查询逻辑
        if (ObjUtil.isNotNull(featureFamilyId)) {
            FeatureFamily featureFamily = vehFeatureFamilyRepository.selectById(featureFamilyId);
            if (featureFamily != null) {
                familyCode = featureFamily.getCode();
            }
        }
        List<FeatureCode> list = vehFeatureFamilyRepository.selectFeatureCodeByFamilyCode(familyCode);
        return PageUtil.convert(list, FeatureCodeAssembler.INSTANCE::fromDomain);
    }

    /**
     * 根据主键ID获取特征值信息
     *
     * @param featureFamilyId 特征族ID
     * @param id              特征值ID
     * @return 特征值信息
     */
    public FeatureCodeVo getFeatureCodeById(Long featureFamilyId, Long id) {
        FeatureCode featureCode = vehFeatureFamilyRepository.selectFeatureCodeById(id);
        return FeatureCodeAssembler.INSTANCE.fromDomain(featureCode);
    }

    /**
     * 根据特征值代码获取特征值信息
     *
     * @param code 特征值代码
     * @return 特征值信息
     */
    public FeatureCodeVo getFeatureCodeByCode(String code) {
        return FeatureCodeAssembler.INSTANCE.fromDomain(vehFeatureFamilyRepository.selectFeatureCodeByCode(code));
    }

    /**
     * 新增特征值
     *
     * @param familyId    特征族ID
     * @param featureCode 特征值信息
     * @param userId      操作用户ID
     * @return 结果
     */
    public int createFeatureCode(Long familyId, FeatureCodeVo featureCode, String userId) {
        FeatureCode featureCodeDomain = FeatureCodeAssembler.INSTANCE.toDomain(featureCode);
        featureCodeDomain.setFamilyCode(vehFeatureFamilyRepository.selectById(familyId).getCode());
        return vehFeatureFamilyRepository.insertFeatureCode(featureCodeDomain);
    }

    /**
     * 修改特征值
     *
     * @param featureFamilyId 特征族ID
     * @param featureCode     特征值信息
     * @param userId          操作用户ID
     * @return 结果
     */
    public int modifyFeatureCode(Long featureFamilyId, FeatureCodeVo featureCode, String userId) {
        FeatureCode featureCodeDomain = FeatureCodeAssembler.INSTANCE.toDomain(featureCode);
        return vehFeatureFamilyRepository.updateFeatureCode(featureCodeDomain);
    }

    /**
     * 批量删除特征值
     *
     * @param featureFamilyId 特征族ID
     * @param ids             特征值ID数组
     * @return 结果
     */
    public int deleteFeatureCodeByIds(Long featureFamilyId, Long[] ids) {
        return vehFeatureFamilyRepository.batchPhysicalDeleteFeatureCode(ids);
    }

}
