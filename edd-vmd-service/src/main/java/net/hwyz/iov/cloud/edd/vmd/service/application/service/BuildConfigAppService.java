package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BuildConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BuildConfigAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBuildConfigRepository;
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

    private final VehBuildConfigRepository vehBuildConfigRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询生产配置信息
     *
     * @param platformCode 车辆平台代码
     * @param seriesCode   车系代码
     * @param modelCode    车型代码
     * @param code         生产配置代码
     * @param name         生产配置名称
     * @param beginTime    开始时间
     * @param endTime      结束时间
     * @return 生产配置列表
     */
    public List<BuildConfigVo> search(String platformCode, String seriesCode, String modelCode, String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformCode);
        map.put("seriesCode", seriesCode);
        map.put("modelCode", modelCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<BuildConfig> buildConfigList = vehBuildConfigRepository.selectByMap(map);
        return PageUtil.convert(buildConfigList, BuildConfigAssembler.INSTANCE::fromDomain);
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
        BuildConfig buildConfig = getBuildConfigByCode(code);
        return !ObjUtil.isNotNull(buildConfig) || buildConfig.getId().longValue() == buildConfigId.longValue();
    }

    /**
     * 检查生产配置下是否存在车辆
     *
     * @param buildConfigId 生产配置ID
     * @return 结果
     */
    public Boolean checkBuildConfigVehicleExist(Long buildConfigId) {
        BuildConfig buildConfig = vehBuildConfigRepository.selectById(buildConfigId);
        Map<String, Object> map = new HashMap<>();
        map.put("buildConfigCode", buildConfig.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取生产配置信息
     *
     * @param id 主键ID
     * @return 生产配置信息
     */
    public BuildConfigVo getBuildConfigById(Long id) {
        return BuildConfigAssembler.INSTANCE.fromDomain(vehBuildConfigRepository.selectById(id));
    }

    /**
     * 根据生产配置代码获取生产配置信息
     *
     * @param code 生产配置代码
     * @return 生产配置领域对象
     */
    public BuildConfig getBuildConfigByCode(String code) {
        return vehBuildConfigRepository.selectByCode(code);
    }

    /**
     * 新增生产配置
     *
     * @param buildConfigVo 生产配置信息
     * @param userId        操作用户ID
     * @return 结果
     */
    public int createBuildConfig(BuildConfigVo buildConfigVo, String userId) {
        BuildConfig buildConfig = BuildConfigAssembler.INSTANCE.toDomain(buildConfigVo);
        buildConfig.setCreateBy(userId);
        return vehBuildConfigRepository.insert(buildConfig);
    }

    /**
     * 修改生产配置
     *
     * @param buildConfigVo 生产配置信息
     * @param userId        操作用户ID
     * @return 结果
     */
    public int modifyBuildConfig(BuildConfigVo buildConfigVo, String userId) {
        BuildConfig buildConfig = BuildConfigAssembler.INSTANCE.toDomain(buildConfigVo);
        buildConfig.setModifyBy(userId);
        return vehBuildConfigRepository.update(buildConfig);
    }

    /**
     * 批量删除生产配置
     *
     * @param ids 生产配置ID数组
     * @return 结果
     */
    public int deleteBuildConfigByIds(Long[] ids) {
        return vehBuildConfigRepository.batchPhysicalDelete(ids);
    }

}
