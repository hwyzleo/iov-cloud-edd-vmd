package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartCategoryInconsistencyPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 零件设备类别一致性 DAO（SEC-VIAM-DSN-CR-006 / D-VIAM-036）
 * <p>
 * 事实源为 `tb_mdm_vehicle_node.device_category`；`tb_part_info.device_category`
 * 为展示用派生冗余列。本 DAO 只做只读比对查询，修复经既有 PartInfoRepository 乐观锁完成。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-24
 */
@Mapper
public interface PartCategoryConsistencyMapper {

    String INCONSISTENT_WHERE = " FROM tb_part_info p JOIN tb_mdm_vehicle_node n ON n.code = p.vehicle_node_code "
            + "WHERE p.row_valid = 1 AND n.row_valid = 1 "
            + "AND (p.device_category IS NULL OR p.device_category <> n.device_category) ";

    /**
     * 统计不一致数量
     *
     * @return 不一致行数
     */
    @Select("SELECT COUNT(*)" + INCONSISTENT_WHERE)
    long countInconsistent();

    /**
     * 查询不一致清单（按零件实例升序）
     *
     * @param limit 返回上限
     * @return 不一致清单
     */
    @Select("SELECT p.id AS id, p.part_code AS partCode, p.sn AS sn, p.vehicle_node_code AS vehicleNodeCode, "
            + "p.device_category AS currentCategory, n.device_category AS expectedCategory, "
            + "p.row_version AS rowVersion" + INCONSISTENT_WHERE
            + "ORDER BY p.id LIMIT #{limit}")
    List<PartCategoryInconsistencyPo> selectInconsistent(@Param("limit") int limit);

    /**
     * 按车载节点查询不一致清单（节点类别变更后重算）
     *
     * @param vehicleNodeCode 车载节点代码
     * @return 不一致清单
     */
    @Select("SELECT p.id AS id, p.part_code AS partCode, p.sn AS sn, p.vehicle_node_code AS vehicleNodeCode, "
            + "p.device_category AS currentCategory, n.device_category AS expectedCategory, "
            + "p.row_version AS rowVersion" + INCONSISTENT_WHERE
            + "AND p.vehicle_node_code = #{vehicleNodeCode} ORDER BY p.id")
    List<PartCategoryInconsistencyPo> selectInconsistentByVehicleNodeCode(
            @Param("vehicleNodeCode") String vehicleNodeCode);

    /**
     * 查询存在不一致的车载节点代码（CR-078：定时巡检按节点批量收敛的分组键）
     *
     * @return 节点代码列表（升序、去重、非空）
     */
    @Select("SELECT DISTINCT p.vehicle_node_code" + INCONSISTENT_WHERE
            + "AND p.vehicle_node_code IS NOT NULL AND p.vehicle_node_code <> '' "
            + "ORDER BY p.vehicle_node_code")
    List<String> selectInconsistentVehicleNodeCodes();
}
