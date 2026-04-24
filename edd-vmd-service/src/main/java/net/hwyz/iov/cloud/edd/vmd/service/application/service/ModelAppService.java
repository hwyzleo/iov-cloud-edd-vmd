package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ModelAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBaseModelMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBasicInfoMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBuildConfigMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehModelMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehModelPo;
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

    private final VehModelMapper vehModelMapper;
    private final VehBasicInfoMapper vehBasicInfoMapper;
    private final VehBaseModelMapper vehBasicModelMapper;
    private final VehBuildConfigMapper vehBuildConfigMapper;

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
        List<VehModelPo> vehModelPoList = vehModelMapper.selectPoByMap(map);
        return PageUtil.convert(vehModelPoList, ModelAssembler.INSTANCE::fromPo);
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
        VehModelPo modelPo = getModelByCode(code);
        return !ObjUtil.isNotNull(modelPo) || modelPo.getId().longValue() == modelId.longValue();
    }

    /**
     * 检查车型下是否存在基础车型
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelBasicModelExist(Long modelId) {
        VehModelPo modelPo = getModelById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", modelPo.getCode());
        return vehBasicModelMapper.countPoByMap(map) > 0;
    }

    /**
     * 检查车型下是否存在车型配置
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelModelConfigExist(Long modelId) {
        VehModelPo modelPo = getModelById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", modelPo.getCode());
        return vehBuildConfigMapper.countPoByMap(map) > 0;
    }

    /**
     * 检查车型下是否存在车辆
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelVehicleExist(Long modelId) {
        VehModelPo modelPo = getModelById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", modelPo.getCode());
        return vehBasicInfoMapper.countPoByMap(map) > 0;
    }

    /**
     * 根据主键ID获取车型信息
     *
     * @param id 主键ID
     * @return 车型信息
     */
    public VehModelPo getModelById(Long id) {
        return vehModelMapper.selectPoById(id);
    }

    /**
     * 根据车型代码获取车型信息
     *
     * @param code 车型代码
     * @return 车型信息
     */
    public VehModelPo getModelByCode(String code) {
        return vehModelMapper.selectPoByCode(code);
    }

    /**
     * 新增车型
     *
     * @param model 车型信息
     * @return 结果
     */
    public int createModel(VehModelPo model) {
        return vehModelMapper.insertPo(model);
    }

    /**
     * 修改车型
     *
     * @param model 车型信息
     * @return 结果
     */
    public int modifyModel(VehModelPo model) {
        return vehModelMapper.updatePo(model);
    }

    /**
     * 批量删除车型
     *
     * @param ids 车型ID数组
     * @return 结果
     */
    public int deleteModelByIds(Long[] ids) {
        return vehModelMapper.batchPhysicalDeletePo(ids);
    }

}
