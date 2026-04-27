package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BuildConfigAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BuildConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BuildConfigQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBuildConfigRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

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
     * @param query 查询 DTO
     * @return 生产配置列表
     */
    public List<BuildConfigDto> search(BuildConfigQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", query.getPlatformCode());
        map.put("seriesCode", query.getSeriesCode());
        map.put("modelCode", query.getModelCode());
        map.put("baseModelCode", query.getBaseModelCode());
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
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
     * @return 生产配置 DTO
     */
    public BuildConfigDto getBuildConfigById(Long id) {
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
     * @param buildConfigDto 生产配置信息 DTO
     * @param userId        操作用户ID
     * @return 结果
     */
    public int createBuildConfig(BuildConfigDto buildConfigDto, String userId) {
        BuildConfig buildConfig = BuildConfigAssembler.INSTANCE.toDomain(buildConfigDto);
        return vehBuildConfigRepository.insert(buildConfig);
    }

    /**
     * 修改生产配置
     *
     * @param buildConfigDto 生产配置信息 DTO
     * @param userId        操作用户ID
     * @return 结果
     */
    public int modifyBuildConfig(BuildConfigDto buildConfigDto, String userId) {
        BuildConfig buildConfig = BuildConfigAssembler.INSTANCE.toDomain(buildConfigDto);
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
