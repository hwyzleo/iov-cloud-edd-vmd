package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;

import java.time.Instant;
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

    /**
     * 按 VIN + 节点查询单条生命周期节点（VMD-DSN-CR-079 / D079-02）
     *
     * @param vin  车架号
     * @param node 生命周期节点
     * @return 节点（可能为 null）
     */
    VehicleLifecycleNode selectByVinAndNode(String vin, VehicleLifecycleNodeEnum node);

    /**
     * 仅向前纠正触达时间（VMD-DSN-CR-079 / D079-03）
     * <p>
     * 条件更新 {@code WHERE vin=? AND node=? AND (reach_time IS NULL OR reach_time > ?)}：
     * 数据库层保证「只前不后」，即使应用层判定被并发绕过也不会把既有节点时间向后移动。
     * {@code reach_time IS NULL} 视为无更早证据，允许回填（不是后退）。
     *
     * @param vin       车架号
     * @param node      生命周期节点
     * @param reachTime 目标触达时间（事实最早时刻）
     * @return 影响行数（0=已存在更早或相等时间，无需纠正）
     */
    int correctReachTimeIfLater(String vin, VehicleLifecycleNodeEnum node, Instant reachTime);

}
