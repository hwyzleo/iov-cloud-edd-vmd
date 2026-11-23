package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationNode;

/**
 * 车辆首次激活节点投影 仓储接口（VMD-DSN-CR-077 / D-VMD-077-04）
 * <p>
 * 写入语义固定为「条件 upsert」：无行插入；有行且候选 {@code occurred_at} 严格更早则向前纠正；
 * 否则不动投影。实现层由唯一键 {@code uk_veh_activation_node_vin} + 条件更新
 * （{@code WHERE occurred_at > ?}）双保险承担并发安全。
 *
 * @author hwyz_leo
 */
public interface VehicleActivationNodeRepository {

    /**
     * 按 VIN 查询首次激活投影
     *
     * @param vin 车架号
     * @return 投影（可能为 null）
     */
    VehicleActivationNode selectByVin(String vin);

    /**
     * 插入投影行
     *
     * @param node 投影
     * @return 影响行数
     */
    int insert(VehicleActivationNode node);

    /**
     * 条件向前纠正：仅当当前投影 {@code occurred_at} 严格晚于给定事实时刻时更新
     *
     * @param node 候选事实（vin 必填，occurredAt 必填）
     * @return 影响行数（0=未纠正，说明已存在更早或相等事实）
     */
    int correctIfEarlier(VehicleActivationNode node);

}
