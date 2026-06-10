package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;

import java.util.List;
import java.util.Map;

/**
 * 车载节点数据仓库接口
 *
 * @author hwyz_leo
 */
public interface MdmVehicleNodeRepository {

    /**
     * 根据条件查询车载节点列表
     *
     * @param map 查询条件
     * @return 车载节点列表
     */
    List<VehicleNode> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计车载节点数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询车载节点
     *
     * @param id 主键ID
     * @return 车载节点
     */
    VehicleNode selectById(Long id);

    /**
     * 根据车载节点代码查询车载节点
     *
     * @param code 车载节点代码
     * @return 车载节点
     */
    VehicleNode selectByCode(String code);

    /**
     * 根据MDM外部引用ID查询车载节点
     *
     * @param externalRefId MDM外部引用ID
     * @return 车载节点
     */
    VehicleNode selectByExternalRefId(String externalRefId);

    /**
     * 根据数据来源统计车载节点数量
     *
     * @param source 数据来源
     * @return 数量
     */
    long countBySource(SourceType source);

    /**
     * 新增车载节点
     *
     * @param vehicleNode 车载节点
     * @return 影响行数
     */
    int insert(VehicleNode vehicleNode);

    /**
     * 修改车载节点
     *
     * @param vehicleNode 车载节点
     * @return 影响行数
     */
    int update(VehicleNode vehicleNode);

    /**
     * 根据主键ID修改车载节点
     *
     * @param vehicleNode 车载节点
     * @return 影响行数
     */
    int updateById(VehicleNode vehicleNode);

    /**
     * 批量物理删除车载节点
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
