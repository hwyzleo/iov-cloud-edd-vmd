package net.hwyz.iov.cloud.edd.vmd.service.application;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PlatformVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.PlatformMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.*;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.*;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehPlatformDo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    private final VehModelDao vehModelDao;
    private final VehSeriesDao vehSeriesDao;
    private final VehPlatformDao vehPlatformDao;
    private final VehBasicInfoDao vehBasicInfoDao;
    private final VehBaseModelDao vehBasicModelDao;
    private final VehBuildConfigDao vehBuildConfigDao;

    /**
     * 查询车辆平台信息
     *
     * @param code      车辆平台代码
     * @param name      车辆平台名称
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆平台列表
     */
    public List<PlatformVo> search(String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdVehPlatformDo> vmdVehPlatformDoList = vehPlatformDao.selectPoByMap(map);
        return PageUtil.convert(vmdVehPlatformDoList, PlatformMapper.INSTANCE::fromDo);
    }

    /**
     * 检查车辆平台代码是否唯一
     *
     * @param platformId 车辆平台ID
     * @param code       车辆平台代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long platformId, String code) {
        if (ObjUtil.isNull(platformId)) {
            platformId = -1L;
        }
        VmdVehPlatformDo platformPo = getPlatformByCode(code);
        return !ObjUtil.isNotNull(platformPo) || platformPo.getId().longValue() == platformId.longValue();
    }

    /**
     * 检查车辆平台下是否存在车系
     *
     * @param platformId 车辆平台ID
     * @return 结果
     */
    public Boolean checkPlatformSeriesExist(Long platformId) {
        VmdVehPlatformDo platformPo = getPlatformById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformPo.getCode());
        return vehSeriesDao.countPoByMap(map) > 0;
    }

    /**
     * 检查车辆平台下是否存在车型
     *
     * @param platformId 车辆平台ID
     * @return 结果
     */
    public Boolean checkPlatformModelExist(Long platformId) {
        VmdVehPlatformDo platformPo = getPlatformById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformPo.getCode());
        return vehModelDao.countPoByMap(map) > 0;
    }

    /**
     * 检查车辆平台下是否存在基础车型
     *
     * @param platformId 车辆平台ID
     * @return 结果
     */
    public Boolean checkPlatformBasicModelExist(Long platformId) {
        VmdVehPlatformDo platformPo = getPlatformById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformPo.getCode());
        return vehBasicModelDao.countPoByMap(map) > 0;
    }

    /**
     * 检查车辆平台下是否存在车型配置
     *
     * @param platformId 车辆平台ID
     * @return 结果
     */
    public Boolean checkPlatformModelConfigExist(Long platformId) {
        VmdVehPlatformDo platformPo = getPlatformById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformPo.getCode());
        return vehBuildConfigDao.countPoByMap(map) > 0;
    }

    /**
     * 检查车辆平台下是否存在车辆
     *
     * @param platformId 车辆平台ID
     * @return 结果
     */
    public Boolean checkPlatformVehicleExist(Long platformId) {
        VmdVehPlatformDo platformPo = getPlatformById(platformId);
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformPo.getCode());
        return vehBasicInfoDao.countPoByMap(map) > 0;
    }

    /**
     * 根据主键ID获取车辆平台信息
     *
     * @param id 主键ID
     * @return 车辆平台信息
     */
    public VmdVehPlatformDo getPlatformById(Long id) {
        return vehPlatformDao.selectPoById(id);
    }

    /**
     * 根据车辆平台代码获取车辆平台信息
     *
     * @param code 车辆平台代码
     * @return 车辆平台信息
     */
    public VmdVehPlatformDo getPlatformByCode(String code) {
        return vehPlatformDao.selectPoByCode(code);
    }

    /**
     * 新增车辆平台
     *
     * @param platform 车辆平台信息
     * @return 结果
     */
    public int createPlatform(VmdVehPlatformDo platform) {
        return vehPlatformDao.insertPo(platform);
    }

    /**
     * 修改车辆平台
     *
     * @param platform 车辆平台信息
     * @return 结果
     */
    public int modifyPlatform(VmdVehPlatformDo platform) {
        return vehPlatformDao.updatePo(platform);
    }

    /**
     * 批量删除车辆平台
     *
     * @param ids 车辆平台ID数组
     * @return 结果
     */
    public int deletePlatformByIds(Long[] ids) {
        return vehPlatformDao.batchPhysicalDeletePo(ids);
    }

}
