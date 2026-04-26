package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PlatformVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.PlatformAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.*;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAppService {

    private final VehPlatformRepository vehPlatformRepository;
    private final VehSeriesRepository vehSeriesRepository;
    private final VehModelRepository vehModelRepository;
    private final VehBaseModelRepository vehBaseModelRepository;
    private final VehBuildConfigRepository vehBuildConfigRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询平台信息
     *
     * @param code      平台代码
     * @param name      平台名称
     * @param beginTime 开始时间
     * @param endTime    结束时间
     * @return 平台列表
     */
    public List<PlatformVo> search(String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<Platform> platformList = vehPlatformRepository.selectByMap(map);
        return PageUtil.convert(platformList, PlatformAssembler.INSTANCE::fromDomain);
    }

    /**
     * 获取所有平台信息
     *
     * @return 平台信息列表
     */
    public List<PlatformVo> listAll() {
        List<Platform> platformList = vehPlatformRepository.selectByMap(new HashMap<>());
        return PlatformAssembler.INSTANCE.fromDomainList(platformList);
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
        Platform platform = getPlatformByCode(code);
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
     * 检查平台下是否存在车型
     *
     * @param platformId 平台ID
     * @return 结果
     */
    public Boolean checkPlatformModelExist(Long platformId) {
        Platform platform = vehPlatformRepository.selectById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platform.getCode());
        return vehModelRepository.countByMap(map) > 0;
    }

    /**
     * 检查平台下是否存在基础车型
     *
     * @param platformId 平台ID
     * @return 结果
     */
    public Boolean checkPlatformBasicModelExist(Long platformId) {
        Platform platform = vehPlatformRepository.selectById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platform.getCode());
        return vehBaseModelRepository.countByMap(map) > 0;
    }

    /**
     * 检查平台下是否存在车型配置
     *
     * @param platformId 平台ID
     * @return 结果
     */
    public Boolean checkPlatformModelConfigExist(Long platformId) {
        Platform platform = vehPlatformRepository.selectById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platform.getCode());
        return vehBuildConfigRepository.countByMap(map) > 0;
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
     * 根据主键ID获取平台信息
     *
     * @param id 主键ID
     * @return platform信息
     */
    public PlatformVo getPlatformById(Long id) {
        return PlatformAssembler.INSTANCE.fromDomain(vehPlatformRepository.selectById(id));
    }

    /**
     * 根据平台代码获取平台信息
     *
     * @param code 平台代码
     * @return 平台领域对象
     */
    public Platform getPlatformByCode(String code) {
        return vehPlatformRepository.selectByCode(code);
    }

    /**
     * 新增平台
     *
     * @param platformVo 平台信息
     * @param userId     操作用户ID
     * @return 结果
     */
    public int createPlatform(PlatformVo platformVo, String userId) {
        Platform platform = PlatformAssembler.INSTANCE.toDomain(platformVo);
        return vehPlatformRepository.insert(platform);
    }

    /**
     * 修改平台
     *
     * @param platformVo 平台信息
     * @param userId     操作用户ID
     * @return 结果
     */
    public int modifyPlatform(PlatformVo platformVo, String userId) {
        Platform platform = PlatformAssembler.INSTANCE.toDomain(platformVo);
        return vehPlatformRepository.update(platform);
    }

    /**
     * 批量删除平台
     *
     * @param ids 平台ID数组
     * @return 结果
     */
    public int deletePlatformByIds(Long[] ids) {
        return vehPlatformRepository.batchPhysicalDelete(ids);
    }

}
