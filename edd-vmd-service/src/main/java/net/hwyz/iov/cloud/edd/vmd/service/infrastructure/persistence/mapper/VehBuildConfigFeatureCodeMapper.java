package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBuildConfigFeatureCodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface VehBuildConfigFeatureCodeMapper extends BaseDao<VehBuildConfigFeatureCodePo, Long> {

    VehBuildConfigFeatureCodePo selectPoByBuildConfigCodeAndFamilyCode(String buildConfigCode, String familyCode);

    List<String> selectBuildConfigCodeByFeatureCodeMap(Map<String, Object> params);

}