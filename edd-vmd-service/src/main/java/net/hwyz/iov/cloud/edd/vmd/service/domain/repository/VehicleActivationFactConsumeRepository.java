package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationFactConsume;

/**
 * 车辆首次激活候选事实消费台账 仓储接口（VMD-DSN-CR-077 / D-VMD-077-03）
 * <p>
 * 双重幂等查询（{@code eventId} + {@code (vin, bindingOperationId)}）、
 * 未生效事实（隔离/失败）查询与状态归档；写入冲突由唯一键兜底，实现层把
 * {@code DuplicateKeyException} 翻译为 0 行并交由应用层重读重判。
 *
 * @author hwyz_leo
 */
public interface VehicleActivationFactConsumeRepository {

    /**
     * 按事件 ID 查询台账（幂等键 1）
     *
     * @param eventId 事件 ID
     * @return 台账（可能为 null）
     */
    VehicleActivationFactConsume selectByEventId(String eventId);

    /**
     * 按业务键查询台账（幂等键 2）
     *
     * @param vin                车架号
     * @param bindingOperationId 绑定操作 ID
     * @return 台账（可能为 null）
     */
    VehicleActivationFactConsume selectByBusinessKey(String vin, String bindingOperationId);

    /**
     * 查询该 VIN 最近一条未生效事实（PENDING 状态来源）
     *
     * @param vin 车架号
     * @return 最近一条未生效台账（可能为 null）
     */
    VehicleActivationFactConsume selectLatestIneffectiveByVin(String vin);

    /**
     * 新增台账
     *
     * @param consume 台账
     * @return 影响行数（0=唯一键冲突）
     */
    int insert(VehicleActivationFactConsume consume);

    /**
     * 更新台账（幂等重投 / 隔离 / 失败归档）
     *
     * @param consume 台账（id 必填）
     * @return 影响行数
     */
    int update(VehicleActivationFactConsume consume);

}
