package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVariantPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 车辆版本表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-01-19
 */
@Mapper
public interface MdmVariantMapper extends BaseDao<MdmVariantPo, Long> {

    /**
     * 通过code查询版本信息
     *
     * @param code 版本编码
     * @return 版本信息
     */
    @Select("SELECT * FROM tb_mdm_variant WHERE code = #{code} AND row_valid = 1 LIMIT 1")
    MdmVariantPo selectPoByCode(@Param("code") String code);

    /**
     * 通过MDM外部引用ID查询版本信息
     *
     * @param externalRefId MDM外部引用ID
     * @return 版本信息
     */
    @Select("SELECT * FROM tb_mdm_variant WHERE external_ref_id = #{externalRefId} AND row_valid = 1")
    MdmVariantPo selectPoByExternalRefId(@Param("externalRefId") String externalRefId);

    /**
     * 根据数据来源统计版本数量
     *
     * @param source 数据来源
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_mdm_variant WHERE source = #{source} AND row_valid = 1")
    long countPoBySource(@Param("source") String source);

}
