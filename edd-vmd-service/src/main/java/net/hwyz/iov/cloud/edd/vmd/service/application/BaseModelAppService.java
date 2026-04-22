package net.hwyz.iov.cloud.edd.vmd.service.application;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelFeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.BaseModelFeatureCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.BaseModelMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBaseModelDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBaseModelFeatureCodeDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBasicInfoDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBuildConfigDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBaseModelDo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBaseModelFeatureCodeDo;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础车型应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaseModelAppService {

    private final VehBasicInfoDao vehBasicInfoDao;
    private final VehBaseModelDao vehBaseModelDao;
    private final VehBuildConfigDao vehBuildConfigDao;
    private final VehBaseModelFeatureCodeDao vehBaseModelFeatureCodeDao;

    /**
     * 查询基础车型信息
     *
     * @param platformCode 车辆平台代码
     * @param seriesCode   车系代码
     * @param modelCode    车型代码
     * @param code         基础车型代码
     * @param name         基础车型名称
     * @param beginTime    开始时间
     * @param endTime      结束时间
     * @return 基础车型列表
     */
    public List<BaseModelVo> search(String platformCode, String seriesCode, String modelCode, String code, String name,
                                    Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformCode);
        map.put("seriesCode", seriesCode);
        map.put("modelCode", modelCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdVehBaseModelDo> vmdVehBaseModelDoList = vehBaseModelDao.selectPoByMap(map);
        return PageUtil.convert(vmdVehBaseModelDoList, BaseModelMapper.INSTANCE::fromDo);
    }

    /**
     * 查询基础车型信息
     *
     * @param baseModelCode 基础车型代码
     * @param familyCode    特征族代码
     * @param beginTime     开始时间
     * @param endTime       结束时间
     * @return 基础车型列表
     */
    public List<BaseModelFeatureCodeVo> searchFeatureCode(String baseModelCode, String familyCode, Date beginTime,
                                                          Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("baseModelCode", baseModelCode);
        map.put("familyCode", familyCode);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return BaseModelFeatureCodeMapper.INSTANCE.fromDoList(vehBaseModelFeatureCodeDao.selectPoByMap(map));
    }

    /**
     * 检查基础车型代码是否唯一
     *
     * @param baseModelId 基础车型ID
     * @param code        基础车型代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long baseModelId, String code) {
        if (ObjUtil.isNull(baseModelId)) {
            baseModelId = -1L;
        }
        VmdVehBaseModelDo baseModelPo = getBaseModelByCode(code);
        return !ObjUtil.isNotNull(baseModelPo) || baseModelPo.getId().longValue() == baseModelId.longValue();
    }

    /**
     * 检查基础车型特征值代码是否唯一
     *
     * @param baseModelFeatureCodeId 基础车型特征值ID
     * @param baseModelCode          基础车型代码
     * @param familyCode             特征族代码
     * @return 结果
     */
    public Boolean checkFeatureCodeUnique(Long baseModelFeatureCodeId, String baseModelCode, String familyCode) {
        if (ObjUtil.isNull(baseModelFeatureCodeId)) {
            baseModelFeatureCodeId = -1L;
        }
        VmdVehBaseModelFeatureCodeDo baseModelFeatureCodePo = getBaseModelFeatureCodeByCode(baseModelCode, familyCode);
        return !ObjUtil.isNotNull(baseModelFeatureCodePo) || baseModelFeatureCodePo.getId().longValue() == baseModelFeatureCodeId.longValue();
    }

    /**
     * 检查基础车型下是否存在生产配置
     *
     * @param baseModelId 基础车型ID
     * @return 结果
     */
    public Boolean checkBaseModelBuildConfigExist(Long baseModelId) {
        VmdVehBaseModelDo baseModelPo = getBaseModelById(baseModelId);
        Map<String, Object> map = new HashMap<>();
        map.put("baseModelCode", baseModelPo.getCode());
        return vehBuildConfigDao.countPoByMap(map) > 0;
    }

    /**
     * 检查基础车型下是否存在车辆
     *
     * @param baseModelId 基础车型ID
     * @return 结果
     */
    public Boolean checkBaseModelVehicleExist(Long baseModelId) {
        VmdVehBaseModelDo baseModelPo = getBaseModelById(baseModelId);
        Map<String, Object> map = new HashMap<>();
        map.put("baseModelCode", baseModelPo.getCode());
        return vehBasicInfoDao.countPoByMap(map) > 0;
    }

    /**
     * 根据主键ID获取基础车型信息
     *
     * @param id 主键ID
     * @return 基础车型信息
     */
    public VmdVehBaseModelDo getBaseModelById(Long id) {
        return vehBaseModelDao.selectPoById(id);
    }

    /**
     * 根据主键ID获取基础车型信息
     *
     * @param id 主键ID
     * @return 基础车型信息
     */
    public BaseModelFeatureCodeVo getBaseModelFeatureCodeById(Long id) {
        return BaseModelFeatureCodeMapper.INSTANCE.fromDo(vehBaseModelFeatureCodeDao.selectPoById(id));
    }

    /**
     * 根据基础车型代码获取基础车型信息
     *
     * @param code 基础车型代码
     * @return 基础车型信息
     */
    public VmdVehBaseModelDo getBaseModelByCode(String code) {
        return vehBaseModelDao.selectPoByCode(code);
    }

    /**
     * 根据基础车型代码获取基础车型信息
     *
     * @param baseModelCode 基础车型代码
     * @param familyCode    特征族代码
     * @return 基础车型特征值信息
     */
    public VmdVehBaseModelFeatureCodeDo getBaseModelFeatureCodeByCode(String baseModelCode, String familyCode) {
        return vehBaseModelFeatureCodeDao.selectPoByBaseModelCodeAndFamilyCode(baseModelCode, familyCode);
    }

    /**
     * 新增基础车型
     *
     * @param baseModel 基础车型信息
     * @return 结果
     */
    public int createBasicModel(VmdVehBaseModelDo baseModel) {
        return vehBaseModelDao.insertPo(baseModel);
    }

    /**
     * 新增基础车型
     *
     * @param baseModelFeatureCode 基础车型特征值
     * @return 结果
     */
    public int createBasicModelFeatureCode(VmdVehBaseModelFeatureCodeDo baseModelFeatureCode) {
        return vehBaseModelFeatureCodeDao.insertPo(baseModelFeatureCode);
    }

    /**
     * 修改基础车型
     *
     * @param baseModel 基础车型信息
     * @return 结果
     */
    public int modifyBasicModel(VmdVehBaseModelDo baseModel) {
        return vehBaseModelDao.updatePo(baseModel);
    }

    /**
     * 修改基础车型特征值
     *
     * @param baseModelFeatureCode 基础车型特征值
     * @return 结果
     */
    public int modifyBaseModelFeatureCode(VmdVehBaseModelFeatureCodeDo baseModelFeatureCode) {
        return vehBaseModelFeatureCodeDao.updatePo(baseModelFeatureCode);
    }

    /**
     * 批量删除基础车型
     *
     * @param ids 基础车型ID数组
     * @return 结果
     */
    public int deleteBasicModelByIds(Long[] ids) {
        return vehBaseModelDao.batchPhysicalDeletePo(ids);
    }

    /**
     * 批量删除基础车型
     *
     * @param ids 基础车型ID数组
     * @return 结果
     */
    public int deleteBaseModelFeatureCodeByIds(Long[] ids) {
        return vehBaseModelFeatureCodeDao.batchPhysicalDeletePo(ids);
    }

}
