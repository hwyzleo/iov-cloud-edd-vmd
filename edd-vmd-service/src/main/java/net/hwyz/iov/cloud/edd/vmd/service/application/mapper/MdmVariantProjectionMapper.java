package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.VariantResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmVariantEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VariantProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVariantRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Variant 统一投影映射器（CR-048 / RD-048-3）
 * <p>
 * Bootstrap 全量同步与 Kafka 增量事件共用的单一映射内核：
 * 快照/事件 → VariantProjectionCommand → 校验（缺必需字段即契约错误）→ 版本门禁 →
 * 幂等 upsert（external_ref_id 优先、code 兜底）；删除/失效事件按投影语义逻辑删除。
 * 不映射旧 platform/carLine 冗余列与 enable/sort/name_en（RD-048-1/2）；
 * 禁止 Bootstrap 与 Consumer 各自维护字段复制逻辑。
 * </p>
 *
 * @author CR-048
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MdmVariantProjectionMapper {

    private final MdmVariantRepository mdmVariantRepository;

    /**
     * MDM 快照转投影命令
     *
     * @param snapshot MDM Variant 快照
     * @return 投影命令
     */
    public VariantProjectionCommand fromSnapshot(VariantResponse snapshot) {
        Long version = snapshot.getVersion() != null ? snapshot.getVersion().longValue() : null;
        return VariantProjectionCommand.builder()
                .code(snapshot.getCode())
                .name(snapshot.getName())
                .nameLocal(snapshot.getNameLocal())
                .modelCode(snapshot.getModelCode())
                .description(snapshot.getDescription())
                .externalRefId(snapshot.getSourceId())
                .externalVersion(version)
                .occurredAt(convertToLocalDateTime(snapshot.getModifyTime()))
                .build();
    }

    /**
     * MDM 事件转投影命令
     *
     * @param event MDM Variant 事件
     * @return 投影命令
     */
    public VariantProjectionCommand fromEvent(MdmVariantEvent event) {
        return VariantProjectionCommand.builder()
                .code(event.getCode())
                .name(event.getName())
                .nameLocal(event.getNameLocal())
                .modelCode(event.getModelCode())
                .description(event.getDescription())
                .externalRefId(event.getEntityId())
                .externalVersion(event.getVersion())
                .occurredAt(event.getOccurredAt())
                .build();
    }

    /**
     * 校验投影命令必需字段（code/name/modelCode/externalRefId/version）
     * <p>
     * 缺必需字段时抛 {@link VariantProjectionException}，
     * 由消息消费者计入失败指标并进入现有重试/DLQ，不写半条投影。
     * </p>
     *
     * @param command 投影命令
     */
    public void validate(VariantProjectionCommand command) {
        if (command == null) {
            throw new VariantProjectionException("Variant 投影 payload 为空");
        }
        if (isBlank(command.getCode())) {
            throw new VariantProjectionException("Variant 投影 payload 缺少 code");
        }
        if (isBlank(command.getName())) {
            throw new VariantProjectionException("Variant 投影 payload 缺少 name, code=" + command.getCode());
        }
        if (isBlank(command.getModelCode())) {
            throw new VariantProjectionException("Variant 投影 payload 缺少 modelCode, code=" + command.getCode());
        }
        if (isBlank(command.getExternalRefId())) {
            throw new VariantProjectionException("Variant 投影 payload 缺少 externalRefId, code=" + command.getCode());
        }
        if (command.getExternalVersion() == null) {
            throw new VariantProjectionException("Variant 投影 payload 缺少 version, code=" + command.getCode());
        }
    }

    /**
     * 版本门禁 + 幂等 upsert（external_ref_id 优先、code 兜底）
     *
     * @param command 投影命令
     */
    public void apply(VariantProjectionCommand command) {
        validate(command);
        Variant local = mdmVariantRepository.selectByExternalRefId(command.getExternalRefId());
        if (local == null) {
            local = mdmVariantRepository.selectByCode(command.getCode());
        }
        LocalDateTime syncTime = command.getOccurredAt() != null ? command.getOccurredAt() : LocalDateTime.now();
        if (local == null) {
            Variant variant = Variant.builder()
                    .code(command.getCode())
                    .name(command.getName())
                    .nameLocal(command.getNameLocal())
                    .modelCode(command.getModelCode())
                    .description(command.getDescription())
                    .source(SourceType.MDM)
                    .externalRefId(command.getExternalRefId())
                    .externalVersion(command.getExternalVersion())
                    .lastSyncTime(syncTime)
                    .build();
            mdmVariantRepository.insert(variant);
            log.info("Variant 投影新增: code={}, externalRefId={}, version={}",
                    command.getCode(), command.getExternalRefId(), command.getExternalVersion());
        } else {
            if (command.getExternalVersion() <= local.getExternalVersion()) {
                log.info("Variant 投影忽略旧版本: code={}, eventVersion={}, localVersion={}",
                        command.getCode(), command.getExternalVersion(), local.getExternalVersion());
                return;
            }
            local.setName(command.getName());
            local.setNameLocal(command.getNameLocal());
            local.setModelCode(command.getModelCode());
            local.setDescription(command.getDescription());
            local.setExternalRefId(command.getExternalRefId());
            local.setExternalVersion(command.getExternalVersion());
            local.setLastSyncTime(syncTime);
            mdmVariantRepository.updateById(local);
            log.info("Variant 投影更新: code={}, version={}",
                    command.getCode(), command.getExternalVersion());
        }
    }

    /**
     * 删除/失效事件处理：按现有投影删除语义逻辑删除，不物理级联删除车辆历史事实与下游映射
     *
     * @param event MDM Variant 删除/失效事件
     */
    public void handleDeletion(MdmVariantEvent event) {
        Variant local = mdmVariantRepository.selectByCode(event.getCode());
        if (local == null) {
            local = mdmVariantRepository.selectByExternalRefId(event.getEntityId());
        }
        if (local == null) {
            log.info("Variant 删除事件本地投影不存在: code={}, entityId={}",
                    event.getCode(), event.getEntityId());
            return;
        }
        if (event.getVersion() != null && event.getVersion() <= local.getExternalVersion()) {
            log.info("Variant 删除事件忽略旧版本: code={}, eventVersion={}, localVersion={}",
                    event.getCode(), event.getVersion(), local.getExternalVersion());
            return;
        }
        mdmVariantRepository.logicalDeleteById(local.getId());
        log.info("Variant 投影逻辑删除: code={}, version={}",
                event.getCode(), event.getVersion());
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
