package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;

import java.util.List;

/**
 * 车辆生命周期节点领域仓库接口
 *
 * @author hwyz_leo
 */
public interface VehicleLifecycleNodeRepository extends BaseRepository<String, VehicleLifecycleNode> {

    /**
     * 根据车架号查询车辆生命周期节点列表
     *
     * @param vin 车架号
     * @return 车辆生命周期节点列表
     */
    List<VehicleLifecycleNode> selectByVin(String vin);

    /**
     * 判断指定 VIN 和节点是否已存在
     *
     * @param vin  车架号
     * @param node 生命周期节点
     * @return 是否存在
     */
    boolean existsByVinAndNode(String vin, VehicleLifecycleNodeEnum node);

}
