package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 车辆基础信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface VehBasicInfoMapper extends BaseDao<VehBasicInfoPo, Long> {

    /**
     * 根据车架号查询车辆基础信息
     *
     * @param vin 车架号
     * @return 车辆基础信息
     */
    VehBasicInfoPo selectPoByVin(String vin);

    /**
     * CAS 补丁更新（CR-071）：按期望版本号更新白名单字段，防止丢失更新
     *
     * @param po 更新目标字段（非空字段才写）
     * @param expectedRowVersion 期望当前版本（乐观锁条件）
     * @return 影响行数（0=版本冲突）
     */
    int patchPoByExpectedVersion(@Param("po") VehBasicInfoPo po, @Param("expectedRowVersion") Integer expectedRowVersion);

}
