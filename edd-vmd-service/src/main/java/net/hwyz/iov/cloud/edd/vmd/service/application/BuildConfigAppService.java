package net.hwyz.iov.cloud.edd.vmd.service.application;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BuildConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.BuildConfigMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBasicInfoDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBuildConfigDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBuildConfigDo;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产配置应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildConfigAppService {

    private final VehBasicInfoDao vehBasicInfoDao;
    private final VehBuildConfigDao vehBuildConfigDao;

    /**
     * 查询车型配置信息
     *
     * @param platformCode 车辆平台代码
     * @param seriesCode   车系代码
     * @param modelCode    车型代码
     * @param code         车型配置代码
     * @param name         车型配置名称
     * @param beginTime    开始时间
     * @param endTime      结束时间
     * @return 车型配置列表
     */
    public List<BuildConfigVo> search(String platformCode, String seriesCode, String modelCode, String code, String name,
                                      Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformCode);
        map.put("seriesCode", seriesCode);
        map.put("modelCode", modelCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdVehBuildConfigDo> vmdVehBuildConfigDoList = vehBuildConfigDao.selectPoByMap(map);
        return PageUtil.convert(vmdVehBuildConfigDoList, BuildConfigMapper.INSTANCE::fromDo);
    }

    /**
     * 检查生产配置代码是否唯一
     *
     * @param buildConfigId 生产配置ID
     * @param code          生产配置代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long buildConfigId, String code) {
        if (ObjUtil.isNull(buildConfigId)) {
            buildConfigId = -1L;
        }
        VmdVehBuildConfigDo buildConfigPo = getBuildConfigByCode(code);
        return !ObjUtil.isNotNull(buildConfigPo) || buildConfigPo.getId().longValue() == buildConfigId.longValue();
    }

    /**
     * 检查生产配置下是否存在车辆
     *
     * @param buildConfigId 车型配置ID
     * @return 结果
     */
    public Boolean checkBuildConfigVehicleExist(Long buildConfigId) {
        VmdVehBuildConfigDo buildConfigPo = getBuildConfigById(buildConfigId);
        Map<String, Object> map = new HashMap<>();
        map.put("buildConfigCode", buildConfigPo.getCode());
        return vehBasicInfoDao.countPoByMap(map) > 0;
    }

    /**
     * 根据主键ID获取生产配置信息
     *
     * @param id 主键ID
     * @return 生产配置信息
     */
    public VmdVehBuildConfigDo getBuildConfigById(Long id) {
        return vehBuildConfigDao.selectPoById(id);
    }

    /**
     * 根据生产配置代码获取生产配置信息
     *
     * @param code 生产配置代码
     * @return 生产配置信息
     */
    public VmdVehBuildConfigDo getBuildConfigByCode(String code) {
        return vehBuildConfigDao.selectPoByCode(code);
    }

    /**
     * 新增生产配置
     *
     * @param buildConfig 生产配置信息
     * @return 结果
     */
    public int createBuildConfig(VmdVehBuildConfigDo buildConfig) {
        return vehBuildConfigDao.insertPo(buildConfig);
    }

    /**
     * 修改生产配置
     *
     * @param buildConfig 生产配置信息
     * @return 结果
     */
    public int modifyBuildConfig(VmdVehBuildConfigDo buildConfig) {
        return vehBuildConfigDao.updatePo(buildConfig);
    }

    /**
     * 批量删除生产配置
     *
     * @param ids 生产配置ID数组
     * @return 结果
     */
    public int deleteBuildConfigByIds(Long[] ids) {
        return vehBuildConfigDao.batchPhysicalDeletePo(ids);
    }

}
