package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ModelAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ModelDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ModelQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBaseModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehModelRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

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
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询车型信息
     *
     * @param query 查询 DTO
     * @return 车型列表
     */
    public List<ModelDto> search(ModelQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", query.getPlatformCode());
        map.put("seriesCode", query.getSeriesCode());
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
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
        Model model = vehModelRepository.selectByCode(code);
        return !ObjUtil.isNotNull(model) || model.getId().longValue() == modelId.longValue();
    }

    /**
     * 检查车型下是否存在基础车型
     *
     * @param modelId 车型ID
     * @return 结果
     */
    public Boolean checkModelBaseModelExist(Long modelId) {
        Model model = vehModelRepository.selectById(modelId);
        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", model.getCode());
        return vehBaseModelRepository.countByMap(map) > 0;
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
     * @return 车型 DTO
     */
    public ModelDto getModelById(Long id) {
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
     * @param modelDto 车型信息 DTO
     * @param userId   操作用户ID
     * @return 结果
     */
    public int createModel(ModelDto modelDto, String userId) {
        Model model = ModelAssembler.INSTANCE.toDomain(modelDto);
        return vehModelRepository.insert(model);
    }

    /**
     * 修改车型
     *
     * @param modelDto 车型信息 DTO
     * @param userId   操作用户ID
     * @return 结果
     */
    public int modifyModel(ModelDto modelDto, String userId) {
        Model model = ModelAssembler.INSTANCE.toDomain(modelDto);
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
