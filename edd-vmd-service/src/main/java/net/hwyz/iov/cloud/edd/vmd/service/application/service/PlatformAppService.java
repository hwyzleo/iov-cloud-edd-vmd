package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.PlatformAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.PlatformDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.PlatformQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehPlatformRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSeriesRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆平台应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAppService {

    private final VehPlatformRepository vehPlatformRepository;
    private final VehSeriesRepository vehSeriesRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询车辆平台信息
     *
     * @param query 查询 DTO
     * @return 车辆平台列表
     */
    public List<PlatformDto> search(PlatformQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<Platform> platformList = vehPlatformRepository.selectByMap(map);
        return PageUtil.convert(platformList, PlatformAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查平台代码是否唯一
     *
     * @param platformId 平台ID
     * @param code       平台代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long platformId, String code) {
        if (ObjUtil.isNull(platformId)) {
            platformId = -1L;
        }
        Platform platform = vehPlatformRepository.selectByCode(code);
        return !ObjUtil.isNotNull(platform) || platform.getId().longValue() == platformId.longValue();
    }

    /**
     * 检查平台下是否存在车系
     *
     * @param platformId 平台ID
     * @return 结果
     */
    public Boolean checkPlatformSeriesExist(Long platformId) {
        Platform platform = vehPlatformRepository.selectById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platform.getCode());
        return vehSeriesRepository.countByMap(map) > 0;
    }

    /**
     * 检查平台下是否存在车辆
     *
     * @param platformId 平台ID
     * @return 结果
     */
    public Boolean checkPlatformVehicleExist(Long platformId) {
        Platform platform = vehPlatformRepository.selectById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platform.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取车辆平台信息
     *
     * @param id 主键ID
     * @return 车辆平台 DTO
     */
    public PlatformDto getPlatformById(Long id) {
        return PlatformAssembler.INSTANCE.fromDomain(vehPlatformRepository.selectById(id));
    }

    /**
     * 根据车辆平台代码获取车辆平台信息
     *
     * @param code 车辆平台代码
     * @return 车辆平台领域对象
     */
    public Platform getPlatformByCode(String code) {
        return vehPlatformRepository.selectByCode(code);
    }

    /**
     * 新增车辆平台
     *
     * @param platformDto 车辆平台信息 DTO
     * @param userId      操作用户ID
     * @return 结果
     */
    public int createPlatform(PlatformDto platformDto, String userId) {
        Platform platform = PlatformAssembler.INSTANCE.toDomain(platformDto);
        return vehPlatformRepository.insert(platform);
    }

    /**
     * 修改车辆平台
     *
     * @param platformDto 车辆平台信息 DTO
     * @param userId      操作用户ID
     * @return 结果
     */
    public int modifyPlatform(PlatformDto platformDto, String userId) {
        Platform platform = PlatformAssembler.INSTANCE.toDomain(platformDto);
        return vehPlatformRepository.update(platform);
    }

    /**
     * 批量删除车辆平台
     *
     * @param ids 车辆平台ID数组
     * @return 结果
     */
    public int deletePlatformByIds(Long[] ids) {
        return vehPlatformRepository.batchPhysicalDelete(ids);
    }

}
