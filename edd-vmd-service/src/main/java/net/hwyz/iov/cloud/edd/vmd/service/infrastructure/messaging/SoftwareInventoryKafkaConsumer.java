package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import com.google.protobuf.util.JsonFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.SoftwareInventoryAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.proto.vmd.swinv.v1.SoftwareInventoryReport;
import net.hwyz.iov.cloud.proto.vmd.swinv.v1.SoftwareItem;
import net.hwyz.iov.cloud.proto.vmd.swinv.v1.ChangeType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 车端软件实装清单上行事件Kafka消费者
 * <p>
 * 监听VAGW转发的车端软件实装清单上报事件（iov.vagw.up.swinv），
 * 使用 iov-cloud-proto-vmd 定义的 SoftwareInventoryReport proto 解析 payload，
 * 转换为 SoftwareInventoryAppService.applyManifest 调用。
 * </p>
 *
 * @author hwyz_leo
 * @see SoftwareInventoryReport
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vmd.software-inventory.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class SoftwareInventoryKafkaConsumer {

    private final SoftwareInventoryAppService softwareInventoryAppService;
    private final VehiclePartAppService vehiclePartAppService;

    /**
     * 消费车端软件实装清单上行事件
     *
     * @param record Kafka消费者记录
     */
    @KafkaListener(
            topics = {"${vmd.software-inventory.kafka.topic:iov.vagw.up.swinv}"},
            groupId = "${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSoftwareInventoryReport(ConsumerRecord<String, String> record) {
        long startTime = System.currentTimeMillis();
        log.info("收到车端软件实装清单事件: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            String vin = record.key(); // Key=VIN
            String payloadJson = record.value();

            // 使用 proto 定义解析 SoftwareInventoryReport
            SoftwareInventoryReport.Builder reportBuilder = SoftwareInventoryReport.newBuilder();
            JsonFormat.parser().merge(payloadJson, reportBuilder);
            SoftwareInventoryReport report = reportBuilder.build();

            // 提取字段
            String manifestVersion = String.valueOf(report.getManifestVersion());
            Instant collectedAt = Instant.ofEpochMilli(report.getCollectedAt());

            int applied = 0;
            int ignored = 0;

            // 遍历软件条目列表
            for (SoftwareItem item : report.getItemsList()) {
                String vehicleNodeCode = item.getVehicleNodeCode();
                String targetCode = item.getSoftwareTargetCode();
                String partNo = item.getSoftwarePartNo();
                String version = item.getSoftwareVersion();
                String slot = item.getSlot();
                String digest = item.getDigest();
                ChangeType changeTypeProto = item.getChangeType();

                // 查询车辆对应的零件绑定
                VehiclePart binding = vehiclePartAppService.getActiveBindingByVinAndNodeCode(vin, vehicleNodeCode);
                if (binding == null) {
                    log.warn("未找到车辆[{}]节点[{}]的active绑定，跳过", vin, vehicleNodeCode);
                    continue;
                }

                // 转换 ChangeType
                String changeType = convertChangeType(changeTypeProto);

                // 调用消解算法（车端上报为 confirmed）
                SoftwareInventoryAppService.ApplyManifestResult result = softwareInventoryAppService.applyManifest(
                        binding.getPartId(),
                        binding.getId(),
                        vin,
                        targetCode,
                        partNo,
                        version,
                        digest,
                        slot.isEmpty() ? null : slot,
                        changeType,
                        "VEHICLE_REPORT",
                        manifestVersion, // 使用 manifestVersion 作为 sourceEventId
                        collectedAt,
                        collectedAt,
                        true); // 车端上报为 confirmed

                if (result.applied()) {
                    applied++;
                }
                if (result.ignoredByVersionGate()) {
                    ignored++;
                }
            }

            log.info("车端软件实装清单事件处理成功: vin={}, applied={}, ignoredByVersionGate={}", vin, applied, ignored);

        } catch (Exception e) {
            log.error("车端软件实装清单事件处理失败: offset={}, error={}", record.offset(), e.getMessage(), e);
        }
    }

    /**
     * 转换 proto ChangeType 到字符串
     *
     * @param changeType proto ChangeType
     * @return 字符串表示
     */
    private String convertChangeType(ChangeType changeType) {
        return switch (changeType) {
            case INITIAL -> "INITIAL";
            case UPGRADE -> "UPGRADE";
            case ROLLBACK -> "ROLLBACK";
            case REFLASH -> "REFLASH";
            case REPAIR -> "REPAIR";
            default -> "INITIAL";
        };
    }
}
