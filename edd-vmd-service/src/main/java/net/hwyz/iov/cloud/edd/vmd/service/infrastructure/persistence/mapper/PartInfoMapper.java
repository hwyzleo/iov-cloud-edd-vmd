package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartInfoPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 物理零件实例本体表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-06-10
 */
@Mapper
public interface PartInfoMapper extends BaseDao<PartInfoPo, Long> {

    /**
     * 根据零件编码和序列号查询
     *
     * @param partCode 零件编码
     * @param sn 序列号
     * @return 零件实例信息
     */
    PartInfoPo selectPoByPartCodeAndSn(String partCode, String sn);

    /**
     * 根据序列号查询
     *
     * @param sn 序列号
     * @return 零件实例信息
     */
    PartInfoPo selectPoBySn(String sn);

    /**
     * CAS 补丁更新（CR-071）：按期望版本号更新白名单字段，防止丢失更新；
     * 支持显式置 null（软件投影列在 UNAVAILABLE/AMBIGUOUS 时清空）
     *
     * @param po 更新目标字段（非空字段才写）
     * @param expectedRowVersion 期望当前版本（乐观锁条件）
     * @param nullFields 需显式置 null 的字段名集合（如 softwareSnapshotInstallationId）
     * @return 影响行数（0=版本冲突）
     */
    int patchPoByExpectedVersion(@Param("po") PartInfoPo po, @Param("expectedRowVersion") Integer expectedRowVersion,
                                 @Param("nullFields") java.util.List<String> nullFields);

}
