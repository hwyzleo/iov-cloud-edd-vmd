package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.InventoryObservedMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ApplySoftwareManifestItemCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.VehicleSoftwareInventoryObservedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SoftwareInventoryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSoftwareBindingResolver;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SoftwareIdempotencyConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SoftwareManifestInvalidException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SoftwareManifestItemInvalidException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.SoftwareInventoryConsumeAudit;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.SoftwareInventoryConsumeAuditRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OTA 车辆软件观测事件 Kafka 消费者
 * <p>
 * VMD-DSN-CR-046: 消费 IOV-OTA 经 Transactional Outbox 发布的
 * {@code ota.vehicle-software-inventory.observed}（Key=VIN），
 * 事件固定映射 VEHICLE_REPORT / confirmed，经 SoftwareInventoryAppService
 * 统一消解写入 part_software_installation。
 * <p>
 * 职责：云服务事件契约校验 → eventId + observationKey 幂等 → 按 vin + ecuId
 * 解析唯一 active 绑定 → item 映射 → 消解写入 → 消费审计（幂等/隔离/DLQ）。
 * VMD SHALL NOT 解析 FOTA Proto / canonicalization。
 * <p>
 * 消费者开关默认关闭（暗部署，IOV-OTA 生产者就绪后经配置启用）。
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vmd.software-inventory.observed.kafka.enabled", havingValue = "true")
public class VehicleSoftwareInventoryObservedConsumer {

    private static final String SOURCE_VEHICLE_REPORT = "VEHICLE_REPORT";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_QUARANTINED = "QUARANTINED";
    private static final String STATUS_FAILED = "FAILED";

    private final SoftwareInventoryAppService softwareInventoryAppService;
    private final VehicleSoftwareBindingResolver vehicleSoftwareBindingResolver;
    private final InventoryObservedMapper inventoryObservedMapper;
    private final SoftwareInventoryConsumeAuditRepository consumeAuditRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${vmd.software-inventory.observed.kafka.dlq-topic:ota.vehicle-software-inventory.observed.dlq}")
    private String dlqTopic;

    /**
     * 消费 OTA 车辆软件观测事件
     *
     * @param record Kafka 消费者记录
     */
    @KafkaListener(
            topics = {"${vmd.software-inventory.observed.kafka.topic:ota.vehicle-software-inventory.observed}"},
            groupId = "${vmd.software-inventory.observed.kafka.group-id:edd-vmd-vehicle-software-inventory}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void onObservedEvent(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到OTA车辆软件观测事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        SoftwareInventoryConsumeAudit audit = null;
        try {
            // 1. 解析与契约校验
            VehicleSoftwareInventoryObservedEvent event = parseAndValidate(record);

            // 2. 事件级幂等（eventId）
            SoftwareInventoryConsumeAudit existingByEvent = consumeAuditRepository.selectByEventId(event.getEventId());
            if (existingByEvent != null) {
                log.info("事件幂等命中: eventId={}, status={}，跳过重复投递", event.getEventId(), existingByEvent.getStatus());
                return;
            }

            // 3. observationKey 幂等冲突检查（同 key 不同 eventId → 摘要不一致 → DLQ）
            SoftwareInventoryConsumeAudit existingByObsKey = consumeAuditRepository.selectByObservationKey(event.getObservationKey());
            if (existingByObsKey != null && !existingByObsKey.getEventId().equals(event.getEventId())) {
                throw new SoftwareIdempotencyConflictException(
                        "observationKey=" + event.getObservationKey() + " 已被 eventId="
                                + existingByObsKey.getEventId() + " 占用，当前 eventId=" + event.getEventId()
                                + "，摘要不一致，进入DLQ");
            }

            // 4. 建立消费审计（PROCESSING）
            audit = buildAudit(event);
            consumeAuditRepository.insert(audit);

            // 5. 逐 item：绑定解析 → 映射 → 消解写入；无唯一绑定/非法 item 隔离
            int applied = 0;
            int ignored = 0;
            int quarantined = 0;
            List<JSONObject> quarantineDetails = new ArrayList<>();

            if (event.getItems() != null) {
                for (VehicleSoftwareInventoryObservedEvent.Item item : event.getItems()) {
                    try {
                        VehicleSoftwareBindingResolver.BindingResolution resolution =
                                vehicleSoftwareBindingResolver.resolve(event.getVin(), item.getEcuId());
                        ApplySoftwareManifestItemCmd cmd =
                                inventoryObservedMapper.toManifestItem(event, item, resolution);

                        SoftwareInventoryAppService.ApplyManifestResult result = softwareInventoryAppService.applyManifest(
                                cmd.getPartId(), cmd.getBindingId(), event.getVin(),
                                cmd.getSoftwareTargetCode(), cmd.getSoftwarePartNo(), cmd.getSoftwareVersion(),
                                cmd.getDigest(), cmd.getSlot(), cmd.getChangeType(),
                                SOURCE_VEHICLE_REPORT, event.getEventId(), event.getCollectedAt(),
                                event.getCollectedAt(), true,
                                cmd.getIsActiveSlot(), cmd.getObservationKey(),
                                cmd.getCanonicalizationVersion(), cmd.getCanonicalDigest(),
                                event.getAcceptedAt());

                        if (result.applied()) {
                            applied++;
                        } else if (result.ignoredByVersionGate()) {
                            ignored++;
                        }
                    } catch (VehicleSoftwareBindingResolver.ActiveBindingNotFoundException
                             | VehicleSoftwareBindingResolver.MultipleActiveBindingException
                             | SoftwareManifestItemInvalidException e) {
                        quarantined++;
                        quarantineDetails.add(buildQuarantineDetail(event.getVin(), item, e.getMessage()));
                        log.warn("item隔离: vin={}, ecuId={}, reason={}", event.getVin(), item.getEcuId(), e.getMessage());
                    }
                }
            }

            // 6. 更新审计结果
            String status = quarantined > 0 ? (applied + ignored == 0 ? STATUS_QUARANTINED : STATUS_PARTIAL) : STATUS_SUCCESS;
            completeAudit(audit, status, applied, ignored, quarantined, quarantineDetails, null);
            log.info("OTA车辆软件观测事件处理完成: eventId={}, itemTotal={}, applied={}, ignored={}, quarantined={}, 耗时={}ms",
                    event.getEventId(), audit.getItemTotal(), applied, ignored, quarantined,
                    System.currentTimeMillis() - startTime);

        } catch (SoftwareManifestInvalidException | SoftwareIdempotencyConflictException e) {
            // 事件级契约错误 / 幂等冲突 → DLQ + 审计 FAILED
            log.error("OTA车辆软件观测事件处理失败（事件级）: offset={}, error={}", record.offset(), e.getMessage());
            sendToDlq(record);
            if (audit != null) {
                completeAudit(audit, STATUS_FAILED, 0, 0, 0, null, e.getMessage());
            }
        } catch (Exception e) {
            // 未知异常 → DLQ + 审计 FAILED（避免无限重试阻塞分区）
            log.error("OTA车辆软件观测事件处理异常: offset={}, error={}", record.offset(), e.getMessage(), e);
            sendToDlq(record);
            if (audit != null) {
                completeAudit(audit, STATUS_FAILED, 0, 0, 0, null, e.getMessage());
            }
        }
    }

    /**
     * 解析事件并做契约校验
     *
     * @param record Kafka 记录
     * @return 解析后的事件
     * @throws Exception 解析或校验失败
     */
    private VehicleSoftwareInventoryObservedEvent parseAndValidate(ConsumerRecord<String, String> record) throws Exception {
        String payloadJson = record.value();
        if (StrUtil.isBlank(payloadJson)) {
            throw new SoftwareManifestInvalidException("事件 payload 为空");
        }
        VehicleSoftwareInventoryObservedEvent event = objectMapper.readValue(payloadJson, VehicleSoftwareInventoryObservedEvent.class);

        if (StrUtil.isBlank(event.getEventId())) {
            throw new SoftwareManifestInvalidException("缺失必填字段 eventId");
        }
        if (StrUtil.isBlank(event.getObservationKey())) {
            throw new SoftwareManifestInvalidException("缺失必填字段 observationKey");
        }
        if (StrUtil.isBlank(event.getVin())) {
            throw new SoftwareManifestInvalidException("缺失必填字段 vin");
        }
        if (StrUtil.isBlank(event.getInventoryRevision())) {
            throw new SoftwareManifestInvalidException("缺失必填字段 inventoryRevision");
        }
        if (event.getCollectedAt() == null) {
            throw new SoftwareManifestInvalidException("缺失必填字段 collectedAt");
        }
        if (event.getCanonicalizationVersion() == null) {
            throw new SoftwareManifestInvalidException("缺失必填字段 canonicalizationVersion");
        }
        if (StrUtil.isBlank(event.getCanonicalDigest())) {
            throw new SoftwareManifestInvalidException("缺失必填字段 canonicalDigest");
        }

        // Kafka Key=VIN 与 payload.vin 一致性（Key 为空时以 payload 为准）
        if (StrUtil.isNotBlank(record.key()) && !record.key().equals(event.getVin())) {
            throw new SoftwareManifestInvalidException("Kafka Key=" + record.key() + " 与 payload.vin=" + event.getVin() + " 不一致");
        }
        return event;
    }

    /**
     * 构建消费审计记录（PROCESSING）
     *
     * @param event 观测事件
     * @return 消费审计记录
     */
    private SoftwareInventoryConsumeAudit buildAudit(VehicleSoftwareInventoryObservedEvent event) {
        int itemTotal = event.getItems() == null ? 0 : event.getItems().size();
        return SoftwareInventoryConsumeAudit.builder()
                .eventId(event.getEventId())
                .observationKey(event.getObservationKey())
                .vinHash(SecureUtil.sha256(event.getVin()))
                .status(STATUS_PROCESSING)
                .itemTotal(itemTotal)
                .itemApplied(0)
                .itemIgnored(0)
                .itemQuarantined(0)
                .retryCount(0)
                .receivedAt(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 完成消费审计记录
     *
     * @param audit 消费审计记录
     * @param status 终态
     * @param applied 成功写入数
     * @param ignored 忽略数
     * @param quarantined 隔离数
     * @param quarantineDetails 隔离明细
     * @param error 错误信息（可空）
     */
    private void completeAudit(SoftwareInventoryConsumeAudit audit, String status,
                               int applied, int ignored, int quarantined,
                               List<JSONObject> quarantineDetails, String error) {
        try {
            audit.setStatus(status);
            audit.setItemApplied(applied);
            audit.setItemIgnored(ignored);
            audit.setItemQuarantined(quarantined);
            if (quarantineDetails != null && !quarantineDetails.isEmpty()) {
                audit.setQuarantineDetail(JSONUtil.toJsonStr(new JSONArray(quarantineDetails)));
            }
            if (StrUtil.isNotBlank(error)) {
                audit.setLastErrorCode(truncate(error, 16));
            }
            audit.setCompletedAt(LocalDateTime.now());
            consumeAuditRepository.update(audit);
        } catch (Exception e) {
            log.error("更新消费审计失败: eventId={}, error={}", audit.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 构造 item 隔离明细（脱敏 VIN + 原事件引用）
     *
     * @param vin VIN
     * @param item 隔离的 item
     * @param reason 隔离原因
     * @return 隔离明细 JSON
     */
    private JSONObject buildQuarantineDetail(String vin, VehicleSoftwareInventoryObservedEvent.Item item, String reason) {
        return JSONUtil.createObj()
                .set("ecuId", item == null ? null : item.getEcuId())
                .set("target", item == null ? null : item.getSoftwareTargetCode())
                .set("slot", item == null ? null : item.getSlot())
                .set("vinMasked", maskVin(vin))
                .set("reason", truncate(reason, 500));
    }

    /**
     * 发送消息到 DLQ
     *
     * @param record 原始记录
     */
    private void sendToDlq(ConsumerRecord<String, String> record) {
        try {
            kafkaTemplate.send(dlqTopic, record.key(), record.value());
            log.warn("已转发消息到DLQ: topic={}, key={}, offset={}", dlqTopic, record.key(), record.offset());
        } catch (Exception e) {
            log.error("转发DLQ失败: dlqTopic={}, error={}", dlqTopic, e.getMessage(), e);
        }
    }

    /**
     * VIN 脱敏（保留前 4 后 4）
     *
     * @param vin VIN
     * @return 脱敏后的 VIN
     */
    private String maskVin(String vin) {
        if (StrUtil.isBlank(vin)) {
            return vin;
        }
        if (vin.length() <= 8) {
            return vin.charAt(0) + "***" + vin.charAt(vin.length() - 1);
        }
        return vin.substring(0, 4) + "***" + vin.substring(vin.length() - 4);
    }

    /**
     * 截断字符串
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
