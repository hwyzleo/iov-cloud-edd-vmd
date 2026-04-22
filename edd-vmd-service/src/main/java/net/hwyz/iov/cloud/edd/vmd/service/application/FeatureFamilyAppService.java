package net.hwyz.iov.cloud.edd.vmd.service.application;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureFamilyVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.FeatureFamilyMapper;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehFeatureCodeDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehFeatureFamilyDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehFeatureCodeDo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehFeatureFamilyDo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆特征族应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFamilyAppService {

    private final VehFeatureCodeDao vehFeatureCodeDao;
    private final VehFeatureFamilyDao vehFeatureFamilyDao;

    /**
     * 查询车辆特征族信息
     *
     * @param code      车辆特征族代码
     * @param name      车辆特征族名称
     * @param type      车辆特征族类型
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆特征族列表
     */
    public List<FeatureFamilyVo> search(String code, String name, String type, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("type", type);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdVehFeatureFamilyDo> vmdVehFeatureFamilyDoList = vehFeatureFamilyDao.selectPoByMap(map);
        return PageUtil.convert(vmdVehFeatureFamilyDoList, FeatureFamilyMapper.INSTANCE::fromDo);
    }

    /**
     * 查询车辆特征值信息
     *
     * @param familyId  车辆特征族ID
     * @param code      车辆特征值代码
     * @param name      车辆特征值名称
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆特征值列表
     */
    public List<VmdVehFeatureCodeDo> searchFeatureCode(Long familyId, String familyCode, String code, String name, Date beginTime, Date endTime) {
        if (familyId != null && familyCode == null) {
            VmdVehFeatureFamilyDo featureFamily = getFeatureFamilyById(familyId);
            familyCode = featureFamily.getCode();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("familyCode", familyCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return vehFeatureCodeDao.selectPoByMap(map);
    }

    /**
     * 检查车辆特征族代码是否唯一
     *
     * @param featureFamilyId 车辆特征族ID
     * @param code            车辆特征族代码
     * @return 结果
     */
    public Boolean checkFamilyCodeUnique(Long featureFamilyId, String code) {
        if (ObjUtil.isNull(featureFamilyId)) {
            featureFamilyId = -1L;
        }
        VmdVehFeatureFamilyDo featureFamilyPo = getFeatureFamilyByCode(code);
        return !ObjUtil.isNotNull(featureFamilyPo) || featureFamilyPo.getId().longValue() == featureFamilyId.longValue();
    }

    /**
     * 检查车辆特征值代码是否唯一
     *
     * @param featureCodeId 车辆特征值ID
     * @param code          车辆特征值代码
     * @return 结果
     */
    public Boolean checkFeatureCodeUnique(Long featureCodeId, String code) {
        if (ObjUtil.isNull(featureCodeId)) {
            featureCodeId = -1L;
        }
        VmdVehFeatureCodeDo featureCodePo = getFeatureCodeByCode(code);
        return !ObjUtil.isNotNull(featureCodePo) || featureCodePo.getId().longValue() == featureCodeId.longValue();
    }

    /**
     * 根据主键ID获取车辆特征族信息
     *
     * @param id 主键ID
     * @return 车辆特征族信息
     */
    public VmdVehFeatureFamilyDo getFeatureFamilyById(Long id) {
        return vehFeatureFamilyDao.selectPoById(id);
    }

    /**
     * 根据主键ID获取车辆特征值信息
     *
     * @param familyId 车辆特征族ID
     * @param id       主键ID
     * @return 车辆特征族信息
     */
    public VmdVehFeatureCodeDo getFeatureCodeById(Long familyId, Long id) {
        VmdVehFeatureFamilyDo featureFamily = getFeatureFamilyById(familyId);
        if (featureFamily == null) {
            return null;
        }
        VmdVehFeatureCodeDo featureCode = vehFeatureCodeDao.selectPoById(id);
        if (featureCode == null) {
            return null;
        }
        if (!featureCode.getFamilyCode().equals(featureFamily.getCode())) {
            return null;
        }
        return featureCode;
    }

    /**
     * 根据车辆特征族代码获取车辆特征族信息
     *
     * @param code 车辆特征族代码
     * @return 车辆特征族信息
     */
    public VmdVehFeatureFamilyDo getFeatureFamilyByCode(String code) {
        return vehFeatureFamilyDao.selectPoByCode(code);
    }

    /**
     * 根据车辆特征值代码获取车辆特征值信息
     *
     * @param code 车辆特征值代码
     * @return 车辆特征值信息
     */
    public VmdVehFeatureCodeDo getFeatureCodeByCode(String code) {
        return vehFeatureCodeDao.selectPoByCode(code);
    }

    /**
     * 新增车辆特征族
     *
     * @param featureFamily 车辆特征族信息
     * @return 结果
     */
    public int createFeatureFamily(VmdVehFeatureFamilyDo featureFamily) {
        return vehFeatureFamilyDao.insertPo(featureFamily);
    }

    /**
     * 新增车辆特征值
     *
     * @param familyId    车辆特征族ID
     * @param featureCode 车辆特征值信息
     * @return 结果
     */
    public int createFeatureCode(Long familyId, VmdVehFeatureCodeDo featureCode) {
        featureCode.setFamilyCode(getFeatureFamilyById(familyId).getCode());
        return vehFeatureCodeDao.insertPo(featureCode);
    }

    /**
     * 修改车辆特征族
     *
     * @param featureFamily 车辆特征族信息
     * @return 结果
     */
    public int modifyFeatureFamily(VmdVehFeatureFamilyDo featureFamily) {
        return vehFeatureFamilyDao.updatePo(featureFamily);
    }

    /**
     * 修改车辆特征族
     *
     * @param familyId    车辆特征族ID
     * @param featureCode 车辆特征值信息
     * @return 结果
     */
    public int modifyFeatureCode(Long familyId, VmdVehFeatureCodeDo featureCode) {
        return vehFeatureCodeDao.updatePo(featureCode);
    }

    /**
     * 批量删除车辆特征族
     *
     * @param ids 车辆特征族ID数组
     * @return 结果
     */
    public int deleteFeatureFamilyByIds(Long[] ids) {
        return vehFeatureFamilyDao.batchPhysicalDeletePo(ids);
    }

    /**
     * 批量删除车辆特征族
     *
     * @param familyId 车辆特征族ID
     * @param ids      车辆特征值ID数组
     * @return 结果
     */
    public int deleteFeatureCodeByIds(Long familyId, Long[] ids) {
        return vehFeatureCodeDao.batchPhysicalDeletePo(ids);
    }

}
