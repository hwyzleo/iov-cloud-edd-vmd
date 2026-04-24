package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelFeatureCodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆基础车型特征值关系表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-08
 */
@Mapper
public interface VehBaseModelFeatureCodeMapper extends BaseDao<VehBaseModelFeatureCodePo, Long> {

    /**
     * 根据基础车型代码和特征族代码查询
     *
     * @param baseModelCode 基础车型代码
     * @param familyCode    特征族代码
     * @return 基础车型特征值关系
     */
    VehBaseModelFeatureCodePo selectPoByBaseModelCodeAndFamilyCode(String baseModelCode, String familyCode);

}
