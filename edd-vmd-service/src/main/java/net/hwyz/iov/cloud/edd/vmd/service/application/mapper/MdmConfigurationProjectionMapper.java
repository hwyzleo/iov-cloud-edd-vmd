package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ConfigurationResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ConfigurationProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.ConfigurationSyncMetrics;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Configuration 统一投影映射器（CR-047 / RD-047-3）
 * <p>
 * Bootstrap 全量同步与 Kafka 增量事件共用的单一映射内核：
 * 快照/事件 → ConfigurationProjectionCommand → 校验（缺必需字段即契约错误）→ 版本门禁 →
 * 幂等 upsert（external_ref_id 优先、code 兜底）；删除/失效事件按投影语义逻辑删除。
 * 禁止 Bootstrap 与 Consumer 各自维护字段复制逻辑。
 * </p>
 *
 * @author CR-047
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MdmConfigurationProjectionMapper {

    private final MdmConfigurationRepository mdmConfigurationRepository;
    private final ConfigurationSyncMetrics configurationSyncMetrics;

    /**
     * MDM 快照转投影命令
     *
     * @param snapshot MDM Configuration 快照
     * @return 投影命令
     */
    public ConfigurationProjectionCommand fromSnapshot(ConfigurationResponse snapshot) {
        Long version = snapshot.getVersion() != null ? snapshot.getVersion().longValue() : null;
        return ConfigurationProjectionCommand.builder()
                .code(snapshot.getCode())
                .name(snapshot.getName())
                .nameLocal(snapshot.getNameLocal())
                .variantCode(snapshot.getVariantCode())
                .description(snapshot.getDescription())
                .externalRefId(snapshot.getSourceId())
                .externalVersion(version)
                .occurredAt(convertToLocalDateTime(snapshot.getModifyTime()))
                .build();
    }

    /**
     * MDM 事件转投影命令
     *
     * @param event MDM Configuration 事件
     * @return 投影命令
     */
    public ConfigurationProjectionCommand fromEvent(MdmConfigurationEvent event) {
        return ConfigurationProjectionCommand.builder()
                .code(event.getCode())
                .name(event.getName())
                .nameLocal(event.getNameLocal())
                .variantCode(event.getVariantCode())
                .description(event.getDescription())
                .externalRefId(event.getEntityId())
                .externalVersion(event.getVersion())
                .occurredAt(event.getOccurredAt())
                .build();
    }

    /**
     * 校验投影命令必需字段（code/name/variantCode/externalRefId/version）
     * <p>
     * 缺 variantCode 等契约字段时抛 {@link ConfigurationProjectionException}，
     * 由消息消费者计入失败指标并进入现有重试/DLQ，不写半条投影。
     * </p>
     *
     * @param command 投影命令
     */
    public void validate(ConfigurationProjectionCommand command) {
        if (command == null) {
            throw new ConfigurationProjectionException("Configuration 投影 payload 为空");
        }
        if (isBlank(command.getCode())) {
            throw new ConfigurationProjectionException("Configuration 投影 payload 缺少 code");
        }
        if (isBlank(command.getName())) {
            throw new ConfigurationProjectionException("Configuration 投影 payload 缺少 name, code=" + command.getCode());
        }
        if (isBlank(command.getVariantCode())) {
            throw new ConfigurationProjectionException("Configuration 投影 payload 缺少 variantCode, code=" + command.getCode());
        }
        if (isBlank(command.getExternalRefId())) {
            throw new ConfigurationProjectionException("Configuration 投影 payload 缺少 externalRefId, code=" + command.getCode());
        }
        if (command.getExternalVersion() == null) {
            throw new ConfigurationProjectionException("Configuration 投影 payload 缺少 version, code=" + command.getCode());
        }
    }

    /**
     * 版本门禁 + 幂等 upsert（external_ref_id 优先、code 兜底）
     *
     * @param command 投影命令
     */
    public void apply(ConfigurationProjectionCommand command) {
        validate(command);
        Configuration local = mdmConfigurationRepository.selectByExternalRefId(command.getExternalRefId());
        if (local == null) {
            local = mdmConfigurationRepository.selectByCode(command.getCode());
        }
        LocalDateTime syncTime = command.getOccurredAt() != null ? command.getOccurredAt() : LocalDateTime.now();
        if (local == null) {
            Configuration configuration = Configuration.builder()
                    .code(command.getCode())
                    .name(command.getName())
                    .nameLocal(command.getNameLocal())
                    .variantCode(command.getVariantCode())
                    .description(command.getDescription())
                    .source(SourceType.MDM)
                    .externalRefId(command.getExternalRefId())
                    .externalVersion(command.getExternalVersion())
                    .lastSyncTime(syncTime)
                    .build();
            mdmConfigurationRepository.insert(configuration);
            log.info("Configuration 投影新增: code={}, externalRefId={}, version={}",
                    command.getCode(), command.getExternalRefId(), command.getExternalVersion());
        } else {
            if (command.getExternalVersion() <= local.getExternalVersion()) {
                log.info("Configuration 投影忽略旧版本: code={}, eventVersion={}, localVersion={}",
                        command.getCode(), command.getExternalVersion(), local.getExternalVersion());
                configurationSyncMetrics.recordStaleEvent();
                return;
            }
            local.setName(command.getName());
            local.setNameLocal(command.getNameLocal());
            local.setVariantCode(command.getVariantCode());
            local.setDescription(command.getDescription());
            local.setExternalRefId(command.getExternalRefId());
            local.setExternalVersion(command.getExternalVersion());
            local.setLastSyncTime(syncTime);
            mdmConfigurationRepository.updateById(local);
            log.info("Configuration 投影更新: code={}, version={}",
                    command.getCode(), command.getExternalVersion());
        }
    }

    /**
     * 删除/失效事件处理：按现有投影删除语义逻辑删除，不物理级联删除选项映射与车辆历史事实
     *
     * @param event MDM Configuration 删除/失效事件
     */
    public void handleDeletion(MdmConfigurationEvent event) {
        Configuration local = mdmConfigurationRepository.selectByCode(event.getCode());
        if (local == null) {
            local = mdmConfigurationRepository.selectByExternalRefId(event.getEntityId());
        }
        if (local == null) {
            log.info("Configuration 删除事件本地投影不存在: code={}, entityId={}",
                    event.getCode(), event.getEntityId());
            return;
        }
        if (event.getVersion() != null && event.getVersion() <= local.getExternalVersion()) {
            log.info("Configuration 删除事件忽略旧版本: code={}, eventVersion={}, localVersion={}",
                    event.getCode(), event.getVersion(), local.getExternalVersion());
            configurationSyncMetrics.recordStaleEvent();
            return;
        }
        mdmConfigurationRepository.logicalDeleteById(local.getId());
        configurationSyncMetrics.recordDeletedEvent();
        log.info("Configuration 投影逻辑删除: code={}, version={}",
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
