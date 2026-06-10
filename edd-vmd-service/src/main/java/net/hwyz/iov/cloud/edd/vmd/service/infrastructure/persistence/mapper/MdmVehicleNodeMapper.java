package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVehicleNodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 车载节点表 DAO
 *
 * @author hwyz_leo
 * @since 2026-06-10
 */
@Mapper
public interface MdmVehicleNodeMapper extends BaseDao<MdmVehicleNodePo, Long> {

    /**
     * 通过车载节点代码查询
     *
     * @param code 车载节点代码
     * @return 车载节点信息
     */
    @Select("SELECT * FROM tb_mdm_vehicle_node WHERE code = #{code} AND row_valid = 1 LIMIT 1")
    MdmVehicleNodePo selectPoByCode(@Param("code") String code);

    /**
     * 通过MDM外部引用ID查询
     *
     * @param externalRefId MDM外部引用ID
     * @return 车载节点信息
     */
    @Select("SELECT * FROM tb_mdm_vehicle_node WHERE external_ref_id = #{externalRefId} AND row_valid = 1")
    MdmVehicleNodePo selectPoByExternalRefId(@Param("externalRefId") String externalRefId);

    /**
     * 根据数据来源统计车载节点数量
     *
     * @param source 数据来源
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_mdm_vehicle_node WHERE source = #{source} AND row_valid = 1")
    long countPoBySource(@Param("source") String source);

}
