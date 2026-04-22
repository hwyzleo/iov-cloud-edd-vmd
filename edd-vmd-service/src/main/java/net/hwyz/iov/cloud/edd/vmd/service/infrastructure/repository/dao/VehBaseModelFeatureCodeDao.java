package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBaseModelFeatureCodeDo;
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
public interface VehBaseModelFeatureCodeDao extends BaseDao<VmdVehBaseModelFeatureCodeDo, Long> {

    /**
     * 根据基础车型代码和特征族代码查询
     *
     * @param baseModelCode 基础车型代码
     * @param familyCode    特征族代码
     * @return 基础车型特征值关系
     */
    VmdVehBaseModelFeatureCodeDo selectPoByBaseModelCodeAndFamilyCode(String baseModelCode, String familyCode);

}
