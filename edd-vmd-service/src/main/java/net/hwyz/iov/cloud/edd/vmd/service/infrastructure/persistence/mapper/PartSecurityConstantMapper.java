package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartSecurityConstantPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 零件安全常量表 DAO
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
@Mapper
public interface PartSecurityConstantMapper extends BaseDao<PartSecurityConstantPo, Long> {

    /**
     * 根据零件编码和序列号查询
     *
     * @param partCode 零件编码
     * @param sn 零件序列号
     * @return 零件安全常量
     */
    @Select("SELECT * FROM tb_part_security_constant WHERE part_code = #{partCode} AND sn = #{sn} AND deleted = 0")
    PartSecurityConstantPo selectPoByPartCodeAndSn(@Param("partCode") String partCode, @Param("sn") String sn);

    /**
     * 根据安全芯片 UID 反查器件安全常量（CR-058 keyprov 按已认证 hsmUid 定位设备）
     *
     * @param chipUid 安全芯片/HSM 唯一标识
     * @return PO 列表（同 uid 可能多条，调用方须判定唯一）
     */
    @Select("SELECT * FROM tb_part_security_constant WHERE chip_uid = #{chipUid} AND deleted = 0")
    java.util.List<PartSecurityConstantPo> selectPoListByChipUid(@Param("chipUid") String chipUid);

    /**
     * 根据零件编码和序列号逻辑删除
     *
     * @param partCode 零件编码
     * @param sn 零件序列号
     * @return 影响行数
     */
    @Update("UPDATE tb_part_security_constant SET deleted = 1 WHERE part_code = #{partCode} AND sn = #{sn} AND deleted = 0")
    int deletePoByPartCodeAndSn(@Param("partCode") String partCode, @Param("sn") String sn);
}
