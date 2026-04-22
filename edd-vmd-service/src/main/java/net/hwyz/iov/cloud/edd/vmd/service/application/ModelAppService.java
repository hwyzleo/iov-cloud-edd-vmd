package net.hwyz.iov.cloud.edd.vmd.service.application;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.ModelMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBaseModelDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBasicInfoDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBuildConfigDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehModelDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehModelDo;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车型应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelAppService {

    private final VehModelDao vehModelDao;
    private final VehBasicInfoDao vehBasicInfoDao;
    private final VehBaseModelDao vehBasicModelDao;
    private final VehBuildConfigDao vehBuildConfigDao;

    /**
     * 查询车型信息
     *
     * @param platformCode 车辆平台代码
     * @param seriesCode   车系代码
     * @param code         车型代码
     * @param name         车型名称
     * @param beginTime    开始时间
     * @param endTime      结束时间
     * @return 车型列表
     */
    public List<ModelVo> search(String platformCode, String seriesCode, String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformCode);
        map.put("seriesCode", seriesCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdVehModelDo> vmdVehModelDoList = vehModelDao.selectPoByMap(map);
        return PageUtil.convert(vmdVehModelDoList, ModelMapper.INSTANCE::fromDo);
    }

    /**
     * 检查车型代码是否唯一
     *
     * @param modelId 车型ID
     * @param code    车型代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long modelId, String code) {
        if (ObjUtil.isNull(modelId)) {
            modelId = -1L;
        }
        VmdVehModelDo modelPo = getModelByCode(code);
        return !ObjUtil.isNotNull(modelPo) || modelPo.getId().longValue() == modelId.longValue();
    }

    /**
     * 检查车型下是否存在基础车型
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelBasicModelExist(Long modelId) {
        VmdVehModelDo modelPo = getModelById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", modelPo.getCode());
        return vehBasicModelDao.countPoByMap(map) > 0;
    }

    /**
     * 检查车型下是否存在车型配置
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelModelConfigExist(Long modelId) {
        VmdVehModelDo modelPo = getModelById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", modelPo.getCode());
        return vehBuildConfigDao.countPoByMap(map) > 0;
    }

    /**
     * 检查车型下是否存在车辆
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelVehicleExist(Long modelId) {
        VmdVehModelDo modelPo = getModelById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", modelPo.getCode());
        return vehBasicInfoDao.countPoByMap(map) > 0;
    }

    /**
     * 根据主键ID获取车型信息
     *
     * @param id 主键ID
     * @return 车型信息
     */
    public VmdVehModelDo getModelById(Long id) {
        return vehModelDao.selectPoById(id);
    }

    /**
     * 根据车型代码获取车型信息
     *
     * @param code 车型代码
     * @return 车型信息
     */
    public VmdVehModelDo getModelByCode(String code) {
        return vehModelDao.selectPoByCode(code);
    }

    /**
     * 新增车型
     *
     * @param model 车型信息
     * @return 结果
     */
    public int createModel(VmdVehModelDo model) {
        return vehModelDao.insertPo(model);
    }

    /**
     * 修改车型
     *
     * @param model 车型信息
     * @return 结果
     */
    public int modifyModel(VmdVehModelDo model) {
        return vehModelDao.updatePo(model);
    }

    /**
     * 批量删除车型
     *
     * @param ids 车型ID数组
     * @return 结果
     */
    public int deleteModelByIds(Long[] ids) {
        return vehModelDao.batchPhysicalDeletePo(ids);
    }

}
