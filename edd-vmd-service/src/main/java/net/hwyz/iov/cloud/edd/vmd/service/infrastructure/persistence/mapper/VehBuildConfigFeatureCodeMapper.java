package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBuildConfigFeatureCodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VehBuildConfigFeatureCodeMapper extends BaseDao<VehBuildConfigFeatureCodePo, Long> {

    VehBuildConfigFeatureCodePo selectPoByBuildConfigCodeAndFamilyCode(String buildConfigCode, String familyCode);

}