package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehOptionCodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 车辆选装值表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-06-09
 */
@Mapper
public interface VehOptionCodeMapper extends BaseDao<VehOptionCodePo, Long> {

    /**
     * 根据车辆选装值代码获取车辆选装值信息
     *
     * @param code 车辆选装值代码
     * @return 车辆选装值信息
     */
    VehOptionCodePo selectPoByCode(String code);

    /**
     * 根据外部引用ID获取车辆选装值信息
     *
     * @param externalRefId 外部引用ID
     * @return 车辆选装值信息
     */
    @Select("SELECT * FROM tb_veh_option_code WHERE external_ref_id = #{externalRefId} AND row_valid = 1")
    VehOptionCodePo selectPoByExternalRefId(@Param("externalRefId") String externalRefId);

    /**
     * 根据数据来源统计选装值数量
     *
     * @param source 数据来源
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_veh_option_code WHERE source = #{source} AND row_valid = 1")
    long countPoBySource(@Param("source") String source);

}
