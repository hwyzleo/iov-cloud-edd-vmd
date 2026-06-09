package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.PlantAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PlantCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PlantDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PlantQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehPlantRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产工厂应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantAppService {

    private final VehPlantRepository vehPlantRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询生产工厂信息
     *
     * @param query 查询 DTO
     * @return 生产工厂列表
     */
    public List<PlantDto> search(PlantQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<Plant> plantList = vehPlantRepository.selectByMap(map);
        return PageUtil.convert(plantList, PlantAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查工厂代码是否唯一
     *
     * @param plantId 工厂ID
     * @param code    工厂代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long plantId, String code) {
        if (ObjUtil.isNull(plantId)) {
            plantId = -1L;
        }
        Plant plant = vehPlantRepository.selectByCode(code);
        return !ObjUtil.isNotNull(plant) || plant.getId().longValue() == plantId.longValue();
    }

    /**
     * 检查工厂下是否存在车辆
     *
     * @param plantId 工厂ID
     * @return 结果
     */
    public Boolean checkPlantVehicleExist(Long plantId) {
        Plant plant = vehPlantRepository.selectById(plantId);
        Map<String, Object> map = new HashMap<>();
        map.put("plantCode", plant.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取生产工厂信息
     *
     * @param id 主键ID
     * @return 生产工厂 DTO
     */
    public PlantDto getPlantById(Long id) {
        return PlantAssembler.INSTANCE.fromDomain(vehPlantRepository.selectById(id));
    }

    /**
     * 根据生产工厂代码获取生产工厂信息
     *
     * @param code 生产工厂代码
     * @return 生产工厂领域对象
     */
    public Plant getPlantByCode(String code) {
        return vehPlantRepository.selectByCode(code);
    }

    /**
     * 新增生产工厂
     *
     * @param plantCmd 生产工厂信息 CMD
     * @param userId   操作用户ID
     * @return 结果
     */
    public int createPlant(PlantCmd plantCmd, String userId) {
        Plant plant = PlantAssembler.INSTANCE.toDomain(plantCmd);
        return vehPlantRepository.insert(plant);
    }

    /**
     * 修改生产工厂
     *
     * @param plantCmd 生产工厂信息 CMD
     * @param userId   操作用户ID
     * @return 结果
     */
    public int modifyPlant(PlantCmd plantCmd, String userId) {
        Plant plant = PlantAssembler.INSTANCE.toDomain(plantCmd);
        return vehPlantRepository.update(plant);
    }

    /**
     * 批量删除生产工厂
     *
     * @param ids 生产工厂ID数组
     * @return 结果
     */
    public int deletePlantByIds(Long[] ids) {
        return vehPlantRepository.batchPhysicalDelete(ids);
    }

    /**
     * 根据外部引用ID获取生产工厂信息
     *
     * @param externalRefId 外部引用ID
     * @return 生产工厂领域对象
     */
    public Plant getPlantByExternalRefId(String externalRefId) {
        return vehPlantRepository.selectByExternalRefId(externalRefId);
    }

    /**
     * 根据数据来源统计生产工厂数量
     *
     * @param source 数据来源
     * @return 数量
     */
    public int countBySource(String source) {
        return vehPlantRepository.countBySource(source);
    }

}
