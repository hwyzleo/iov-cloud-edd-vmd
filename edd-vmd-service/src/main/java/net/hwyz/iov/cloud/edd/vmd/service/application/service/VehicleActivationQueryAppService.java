package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleActivationState;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleActivationDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ActivationFactQueryUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationFactConsume;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationFactConsumeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationNodeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 车辆首次激活事实只读查询应用服务（VMD-DSN-CR-077 §7.2 / D-VMD-077-09）
 * <p>
 * 三态判定：投影存在 → {@code RECORDED}；投影不存在但存在未生效台账
 * （{@code QUARANTINED}/{@code FAILED} 且 {@code applied=0}）→ {@code PENDING}；
 * 两者皆无 → {@code NOT_RECORDED}。
 * <p>
 * <strong>fail-closed</strong>：任何技术异常一律抛 806137，绝不降级为 {@code NOT_RECORDED}。
 * 只读：不写投影/台账/生命周期/outbox，不触发任何事件（TC-077-13）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleActivationQueryAppService {

    private final VehicleActivationNodeRepository nodeRepository;
    private final VehicleActivationFactConsumeRepository consumeRepository;

    /**
     * 查询指定 VIN 的首次激活事实三态
     *
     * @param vin 已校验车架号
     * @return 三态结论 + 事实证据
     * @throws ActivationFactQueryUnavailableException 查询技术失败（806137，fail-closed）
     */
    public VehicleActivationDto queryFirstActivation(String vin) {
        try {
            VehicleActivationNode node = nodeRepository.selectByVin(vin);
            if (node != null) {
                return toRecorded(node);
            }
            VehicleActivationFactConsume pending = consumeRepository.selectLatestIneffectiveByVin(vin);
            if (pending != null) {
                return toPending(vin, pending);
            }
            return VehicleActivationDto.builder()
                    .state(VehicleActivationState.NOT_RECORDED)
                    .vin(vin)
                    .build();
        } catch (ActivationFactQueryUnavailableException e) {
            throw e;
        } catch (Exception e) {
            // fail-closed：不可用不得伪装为「未激活」
            throw new ActivationFactQueryUnavailableException(
                    "vIN=" + maskVin(vin) + ", error=" + e.getClass().getSimpleName());
        }
    }

    /**
     * 构建 RECORDED 结果
     *
     * @param node 投影
     * @return 查询结果
     */
    private VehicleActivationDto toRecorded(VehicleActivationNode node) {
        Instant firstActivatedAt = toInstant(node.getFirstActivatedAt());
        return VehicleActivationDto.builder()
                .state(VehicleActivationState.RECORDED)
                .vin(node.getVin())
                .firstActivatedAt(firstActivatedAt)
                .bindingOperationId(node.getBindingOperationId())
                .sourceFactRef(node.getSourceFactRef())
                .eventId(node.getEventId())
                .occurredAt(toInstant(node.getOccurredAt()))
                .receivedAt(toInstant(node.getReceivedAt()))
                .vinResolved(node.getVinResolved() == null ? null : node.getVinResolved() == 1)
                .factRef(buildFactRef(node.getVin(), node.getEventId(), firstActivatedAt))
                .build();
    }

    /**
     * 构建 PENDING 结果（显式 firstActivatedAt=null，调用方不得据此推断已激活）
     *
     * @param vin     车架号
     * @param consume 最近一条未生效台账
     * @return 查询结果
     */
    private VehicleActivationDto toPending(String vin, VehicleActivationFactConsume consume) {
        return VehicleActivationDto.builder()
                .state(VehicleActivationState.PENDING)
                .vin(vin)
                .firstActivatedAt(null)
                .eventId(consume.getEventId())
                .occurredAt(toInstant(consume.getOccurredAt()))
                .receivedAt(toInstant(consume.getReceivedAt()))
                .factRef(null)
                .build();
    }

    /**
     * 事实聚合摘要：{@code vehicleActivation:<vin>:<eventId>:<firstActivatedAt>}
     *
     * @param vin              车架号
     * @param eventId          事件 ID
     * @param firstActivatedAt 首次激活时刻
     * @return 摘要（不暴露自增主键）
     */
    private String buildFactRef(String vin, String eventId, Instant firstActivatedAt) {
        StringBuilder factRef = new StringBuilder("vehicleActivation:")
                .append(vin)
                .append(':')
                .append(StrUtil.nullToEmpty(eventId));
        if (firstActivatedAt != null) {
            factRef.append(':').append(firstActivatedAt);
        }
        return factRef.toString();
    }

    /**
     * 数据库 UTC DATETIME(3) → 绝对时刻
     *
     * @param localDateTime UTC 本地时刻
     * @return 绝对时刻
     */
    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
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
