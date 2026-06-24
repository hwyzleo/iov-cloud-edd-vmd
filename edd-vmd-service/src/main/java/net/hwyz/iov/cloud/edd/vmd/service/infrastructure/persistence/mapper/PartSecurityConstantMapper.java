package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartSecurityConstantPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
     * 根据零件编码和序列号统计数量
     *
     * @param partCode 零件编码
     * @param sn 零件序列号
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_part_security_constant WHERE part_code = #{partCode} AND sn = #{sn} AND deleted = 0")
    long countPoByPartCodeAndSn(@Param("partCode") String partCode, @Param("sn") String sn);
}
