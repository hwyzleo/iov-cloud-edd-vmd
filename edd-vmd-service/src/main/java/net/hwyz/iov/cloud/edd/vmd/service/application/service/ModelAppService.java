package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ModelAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBaseModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBuildConfigRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehModelRepository;
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

    private final VehModelRepository vehModelRepository;
    private final VehBaseModelRepository vehBaseModelRepository;
    private final VehBuildConfigRepository vehBuildConfigRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

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
        List<Model> modelList = vehModelRepository.selectByMap(map);
        return PageUtil.convert(modelList, ModelAssembler.INSTANCE::fromDomain);
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
        Model model = getModelByCode(code);
        return !ObjUtil.isNotNull(model) || model.getId().longValue() == modelId.longValue();
    }

    /**
     * 检查车型下是否存在基础车型
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelBasicModelExist(Long modelId) {
        Model model = vehModelRepository.selectById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", model.getCode());
        return vehBaseModelRepository.countByMap(map) > 0;
    }

    /**
     * 检查车型下是否存在车型配置
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelModelConfigExist(Long modelId) {
        Model model = vehModelRepository.selectById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", model.getCode());
        return vehBuildConfigRepository.countByMap(map) > 0;
    }

    /**
     * 检查车型下是否存在车辆
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelVehicleExist(Long modelId) {
        Model model = vehModelRepository.selectById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", model.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取车型信息
     *
     * @param id 主键ID
     * @return 车型信息
     */
    public ModelVo getModelById(Long id) {
        return ModelAssembler.INSTANCE.fromDomain(vehModelRepository.selectById(id));
    }

    /**
     * 根据车型代码获取车型信息
     *
     * @param code 车型代码
     * @return 车型领域对象
     */
    public Model getModelByCode(String code) {
        return vehModelRepository.selectByCode(code);
    }

    /**
     * 新增车型
     *
     * @param modelVo 车型信息
     * @param userId  操作用户ID
     * @return 结果
     */
    public int createModel(ModelVo modelVo, String userId) {
        Model model = ModelAssembler.INSTANCE.toDomain(modelVo);
        return vehModelRepository.insert(model);
    }

    /**
     * 修改车型
     *
     * @param modelVo 车型信息
     * @param userId  操作用户ID
     * @return 结果
     */
    public int modifyModel(ModelVo modelVo, String userId) {
        Model model = ModelAssembler.INSTANCE.toDomain(modelVo);
        return vehModelRepository.update(model);
    }

    /**
     * 批量删除车型
     *
     * @param ids 车型ID数组
     * @return 结果
     */
    public int deleteModelByIds(Long[] ids) {
        return vehModelRepository.batchPhysicalDelete(ids);
    }

}
