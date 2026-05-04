package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfigFeatureCode;

import java.util.List;
import java.util.Map;

public interface VehBuildConfigRepository {

    List<BuildConfig> selectByMap(Map<String, Object> map);

    int countByMap(Map<String, Object> map);

    BuildConfig selectById(Long id);

    BuildConfig selectByCode(String code);

    int insert(BuildConfig buildConfig);

    int update(BuildConfig buildConfig);

    int batchPhysicalDelete(Long[] ids);

    List<BuildConfig> selectByExample(BuildConfig example);

    List<BuildConfigFeatureCode> selectFeatureCodeByExample(BuildConfigFeatureCode example);

    int batchInsertFeatureCode(List<BuildConfigFeatureCode> featureCodeList);

    int updateFeatureCode(BuildConfigFeatureCode featureCode);

    int batchPhysicalDeleteFeatureCode(Long[] ids);

}