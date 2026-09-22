package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VehicleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVehicleNodeEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNodeProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * VehicleNode 统一投影映射器（CR-049）
 * <p>
 * Bootstrap 全量同步与 Kafka 增量事件共用的单一映射内核（RD-049 阶段一）：
 * 快照/事件 → VehicleNodeProjectionCommand → 校验（缺必需字段即契约错误）→ 版本门禁 →
 * 幂等 upsert（按 code 幂等）。禁止 Bootstrap 与 Consumer 各自维护字段复制逻辑。
 * Deleted/Deactivated 沿用现有投影失效语义（本期不做物理删除，保持 CR-020 基线）。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MdmVehicleNodeProjectionMapper {

    private final MdmVehicleNodeRepository mdmVehicleNodeRepository;

    /**
     * MDM 快照转投影命令
     *
     * @param snapshot MDM VehicleNode 快照
     * @return 投影命令
     */
    public VehicleNodeProjectionCommand fromSnapshot(VehicleNodeResponse snapshot) {
        Long version = snapshot.getExternalVersion() != null
                ? snapshot.getExternalVersion()
                : (snapshot.getVersion() != null ? snapshot.getVersion().longValue() : 0L);
        return VehicleNodeProjectionCommand.builder()
                .code(snapshot.getNodeCode())
                .name(snapshot.getName())
                .nameLocal(snapshot.getNameLocal())
                .deviceCategory(snapshot.getDeviceCategory())
                .hsmCapability(snapshot.getHsmCapability())
                .funcDomain(snapshot.getFunctionalDomain() != null ? snapshot.getFunctionalDomain() : "GENERAL")
                .nodeType(snapshot.getNodeType() != null ? snapshot.getNodeType() : "ECU")
                .otaSupport(snapshot.getOtaSupportType() != null ? snapshot.getOtaSupportType() : "NONE")
                .core(snapshot.getIsCoreNode())
                .sort(0)
                .externalRefId(snapshot.getExternalRefId() != null ? snapshot.getExternalRefId() : snapshot.getNodeCode())
                .externalVersion(version)
                .occurredAt(convertToLocalDateTime(snapshot.getLastSyncTime()))
                .build();
    }

    /**
     * MDM 事件转投影命令
     *
     * @param event MDM VehicleNode 事件
     * @return 投影命令
     */
    public VehicleNodeProjectionCommand fromEvent(MdmVehicleNodeEvent event) {
        return VehicleNodeProjectionCommand.builder()
                .code(event.getCode())
                .name(event.getName())
                .nameLocal(event.getNameEn())
                .deviceCategory(event.getDeviceCategory())
                .hsmCapability(event.getHsmCapability())
                .funcDomain(event.getFuncDomain())
                .nodeType(event.getNodeType())
                .otaSupport(event.getOtaSupport())
                .core(event.getCore())
                .sort(event.getSort() != null ? event.getSort() : 0)
                .externalRefId(event.getEntityId())
                .externalVersion(event.getVersion())
                .occurredAt(event.getOccurredAt())
                .build();
    }

    /**
     * 校验投影命令必需字段（code/externalRefId/version）
     * <p>
     * 缺必需契约字段时抛 {@link VehicleNodeProjectionException}，
     * 由消息消费者计入失败指标并进入现有重试/DLQ，不写半条投影。
     * </p>
     *
     * @param command 投影命令
     */
    public void validate(VehicleNodeProjectionCommand command) {
        if (command == null) {
            throw new VehicleNodeProjectionException("VehicleNode 投影 payload 为空");
        }
        if (isBlank(command.getCode())) {
            throw new VehicleNodeProjectionException("VehicleNode 投影 payload 缺少 code");
        }
        if (isBlank(command.getExternalRefId())) {
            throw new VehicleNodeProjectionException("VehicleNode 投影 payload 缺少 externalRefId, code=" + command.getCode());
        }
        if (command.getExternalVersion() == null) {
            throw new VehicleNodeProjectionException("VehicleNode 投影 payload 缺少 version, code=" + command.getCode());
        }
    }

    /**
     * 版本门禁 + 幂等 upsert（按 code 幂等）
     *
     * @param command 投影命令
     */
    public void apply(VehicleNodeProjectionCommand command) {
        validate(command);
        VehicleNode local = mdmVehicleNodeRepository.selectByCode(command.getCode());
        LocalDateTime syncTime = command.getOccurredAt() != null ? command.getOccurredAt() : LocalDateTime.now();
        if (local == null) {
            VehicleNode vehicleNode = VehicleNode.builder()
                    .code(command.getCode())
                    .name(command.getName())
                    .nameLocal(command.getNameLocal())
                    .deviceCategory(command.getDeviceCategory())
                    .hsmCapability(command.getHsmCapability())
                    .funcDomain(command.getFuncDomain())
                    .nodeType(command.getNodeType())
                    .otaSupport(command.getOtaSupport())
                    .core(command.getCore())
                    .sort(command.getSort() != null ? command.getSort() : 0)
                    .source(SourceType.MDM)
                    .externalRefId(command.getExternalRefId())
                    .externalVersion(command.getExternalVersion())
                    .lastSyncTime(syncTime)
                    .build();
            mdmVehicleNodeRepository.insert(vehicleNode);
            log.info("VehicleNode 投影新增: code={}, hsmCapability={}, deviceCategory={}, version={}",
                    command.getCode(), command.getHsmCapability(), command.getDeviceCategory(), command.getExternalVersion());
        } else {
            if (command.getExternalVersion() <= local.getExternalVersion()) {
                log.info("VehicleNode 投影忽略旧版本: code={}, eventVersion={}, localVersion={}",
                        command.getCode(), command.getExternalVersion(), local.getExternalVersion());
                return;
            }
            local.setName(command.getName());
            local.setNameLocal(command.getNameLocal());
            local.setDeviceCategory(command.getDeviceCategory());
            local.setHsmCapability(command.getHsmCapability());
            local.setFuncDomain(command.getFuncDomain());
            local.setNodeType(command.getNodeType());
            local.setOtaSupport(command.getOtaSupport());
            local.setCore(command.getCore());
            local.setSort(command.getSort() != null ? command.getSort() : 0);
            local.setExternalRefId(command.getExternalRefId());
            local.setExternalVersion(command.getExternalVersion());
            local.setLastSyncTime(syncTime);
            mdmVehicleNodeRepository.updateById(local);
            log.info("VehicleNode 投影更新: code={}, hsmCapability={}, deviceCategory={}, version={}",
                    command.getCode(), command.getHsmCapability(), command.getDeviceCategory(), command.getExternalVersion());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
