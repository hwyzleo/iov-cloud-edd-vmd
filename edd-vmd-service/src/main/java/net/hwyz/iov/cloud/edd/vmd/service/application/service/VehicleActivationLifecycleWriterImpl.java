package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.service.VehicleActivationLifecycleWriter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 首次激活事实 → 车辆生命周期节点专用写入实现（VMD-DSN-CR-079 / D079-02、D079-03）
 * <p>
 * 只操作既有 {@code tb_veh_lifecycle} 的 {@code VEHICLE_ACTIVE} 节点，复用既有
 * {@link VehicleLifecycleNodeRepository}，不新增表/列/索引，不改其它节点的「首次写入胜出」语义。
 * <p>
 * 时间口径：目标时间来自激活事实 {@code occurredAt} 的最早收敛值（UTC），
 * 不使用 {@code receivedAt}、消费时间或数据库当前时间冒充事实发生时间（FR-VMD-079-02）。
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleActivationLifecycleWriterImpl implements VehicleActivationLifecycleWriter {

    private final VehicleLifecycleNodeRepository vehicleLifecycleNodeRepository;

    @Override
    public LifecycleWriteOutcome write(String vin, LocalDateTime earliestActivatedAt) {
        if (StrUtil.isBlank(vin)) {
            throw new IllegalArgumentException("VIN 不能为空");
        }
        if (earliestActivatedAt == null) {
            throw new IllegalArgumentException("事实最早激活时刻不能为空: vin=" + vin);
        }
        Instant target = earliestActivatedAt.toInstant(ZoneOffset.UTC);

        VehicleLifecycleNode existing = vehicleLifecycleNodeRepository
                .selectByVinAndNode(vin, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE);
        if (existing == null) {
            return insert(vin, target);
        }
        return converge(vin, target, existing);
    }

    /**
     * 节点不存在：插入目标时间；并发唯一键冲突后重读重判（D079-03）
     *
     * @param vin    车架号
     * @param target 目标触达时间
     * @return 写入结论
     */
    private LifecycleWriteOutcome insert(String vin, Instant target) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.VEHICLE_ACTIVE)
                .reachTime(target)
                .build();
        node.init();
        try {
            vehicleLifecycleNodeRepository.save(node);
            log.info("首次激活生命周期节点已写入: vin={}, node={}, reachTime={}",
                    maskVin(vin), VehicleLifecycleNodeEnum.VEHICLE_ACTIVE, target);
            return LifecycleWriteOutcome.applied(target);
        } catch (DuplicateKeyException ex) {
            // 并发同 VIN 写入：唯一键兜底，重读后按「更早则纠正 / 否则重复」重判
            VehicleLifecycleNode reRead = vehicleLifecycleNodeRepository
                    .selectByVinAndNode(vin, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE);
            if (reRead == null) {
                // 唯一键冲突但重读为空：不是可收敛状态，交回事务回滚与既有重试机制
                throw ex;
            }
            log.debug("首次激活生命周期节点并发写入冲突，转收敛判定: vin={}", maskVin(vin));
            return converge(vin, target, reRead);
        }
    }

    /**
     * 节点已存在：只向前纠正，0 行时重读按已收敛处理（D079-03）
     *
     * @param vin      车架号
     * @param target   目标触达时间
     * @param existing 既有节点
     * @return 写入结论
     */
    private LifecycleWriteOutcome converge(String vin, Instant target, VehicleLifecycleNode existing) {
        Instant current = existing.getReachTime();
        if (current != null && !target.isBefore(current)) {
            // 更晚或相等：保持既有最早时间，不动（FR-VMD-079-06）
            log.debug("首次激活生命周期节点已收敛，保持既有时间: vin={}, existing={}, target={}",
                    maskVin(vin), current, target);
            return LifecycleWriteOutcome.duplicate(current);
        }
        int rows = vehicleLifecycleNodeRepository.correctReachTimeIfLater(
                vin, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE, target);
        if (rows > 0) {
            log.info("首次激活生命周期节点向前纠正: vin={}, node={}, old={}, new={}",
                    maskVin(vin), VehicleLifecycleNodeEnum.VEHICLE_ACTIVE, current, target);
            return LifecycleWriteOutcome.corrected(current, target);
        }
        // 条件更新 0 行：并发已收敛到更早或相等时间，重读按成功/重复处理
        VehicleLifecycleNode reRead = vehicleLifecycleNodeRepository
                .selectByVinAndNode(vin, VehicleLifecycleNodeEnum.VEHICLE_ACTIVE);
        Instant converged = reRead == null ? current : reRead.getReachTime();
        log.debug("首次激活生命周期节点条件纠正未生效（并发已收敛）: vin={}, converged={}",
                maskVin(vin), converged);
        return LifecycleWriteOutcome.duplicate(converged);
    }

    /**
     * VIN 脱敏（保留前 3 后 4）
     *
     * @param vin 车架号
     * @return 脱敏后的 VIN
     */
    private static String maskVin(String vin) {
        if (StrUtil.isBlank(vin) || vin.length() <= 7) {
            return "****";
        }
        return vin.substring(0, 3) + "****" + vin.substring(vin.length() - 4);
    }
}
