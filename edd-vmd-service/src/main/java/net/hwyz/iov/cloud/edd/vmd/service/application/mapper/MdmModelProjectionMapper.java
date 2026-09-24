package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.mdm.api.vo.response.ModelResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ModelProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmModelEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ModelProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Model 统一投影映射器（CR-048 / RD-048-3）
 * <p>
 * Bootstrap 全量同步与 Kafka 增量事件共用的单一映射内核：
 * 快照/事件 → ModelProjectionCommand → 校验（缺必需字段即契约错误）→ 版本门禁 →
 * 幂等 upsert（external_ref_id 优先、code 兜底）；删除/失效事件按投影语义逻辑删除。
 * 不映射旧 enable/sort/name_en（RD-048-2）；禁止 Bootstrap 与 Consumer 各自维护字段复制逻辑。
 * </p>
 *
 * @author CR-048
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MdmModelProjectionMapper {

    private final MdmModelRepository mdmModelRepository;

    /**
     * MDM 快照转投影命令
     *
     * @param snapshot MDM Model 快照
     * @return 投影命令
     */
    public ModelProjectionCommand fromSnapshot(ModelResponse snapshot) {
        Long version = snapshot.getVersion() != null ? snapshot.getVersion().longValue() : null;
        return ModelProjectionCommand.builder()
                .code(snapshot.getCode())
                .name(snapshot.getName())
                .nameLocal(snapshot.getNameLocal())
                .carLineCode(snapshot.getCarLineCode())
                .platformCode(snapshot.getPlatformCode())
                .description(snapshot.getDescription())
                .externalRefId(snapshot.getSourceId())
                .externalVersion(version)
                .occurredAt(convertToLocalDateTime(snapshot.getModifyTime()))
                .build();
    }

    /**
     * MDM 事件转投影命令
     *
     * @param event MDM Model 事件
     * @return 投影命令
     */
    public ModelProjectionCommand fromEvent(MdmModelEvent event) {
        return ModelProjectionCommand.builder()
                .code(event.getCode())
                .name(event.getName())
                .nameLocal(event.getNameLocal())
                .carLineCode(event.getCarLineCode())
                .platformCode(event.getPlatformCode())
                .description(event.getDescription())
                .externalRefId(event.getEntityId())
                .externalVersion(event.getVersion())
                .occurredAt(event.getOccurredAt())
                .build();
    }

    /**
     * 校验投影命令必需字段（code/name/carLineCode/platformCode/externalRefId/version）
     * <p>
     * 缺必需字段时抛 {@link ModelProjectionException}，
     * 由消息消费者计入失败指标并进入现有重试/DLQ，不写半条投影。
     * </p>
     *
     * @param command 投影命令
     */
    public void validate(ModelProjectionCommand command) {
        if (command == null) {
            throw new ModelProjectionException("Model 投影 payload 为空");
        }
        if (isBlank(command.getCode())) {
            throw new ModelProjectionException("Model 投影 payload 缺少 code");
        }
        if (isBlank(command.getName())) {
            throw new ModelProjectionException("Model 投影 payload 缺少 name, code=" + command.getCode());
        }
        if (isBlank(command.getCarLineCode())) {
            throw new ModelProjectionException("Model 投影 payload 缺少 carLineCode, code=" + command.getCode());
        }
        if (isBlank(command.getPlatformCode())) {
            throw new ModelProjectionException("Model 投影 payload 缺少 platformCode, code=" + command.getCode());
        }
        if (isBlank(command.getExternalRefId())) {
            throw new ModelProjectionException("Model 投影 payload 缺少 externalRefId, code=" + command.getCode());
        }
        if (command.getExternalVersion() == null) {
            throw new ModelProjectionException("Model 投影 payload 缺少 version, code=" + command.getCode());
        }
    }

    /**
     * 版本门禁 + 幂等 upsert（external_ref_id 优先、code 兜底）
     *
     * @param command 投影命令
     */
    public void apply(ModelProjectionCommand command) {
        validate(command);
        Model local = mdmModelRepository.selectByExternalRefId(command.getExternalRefId());
        if (local == null) {
            local = mdmModelRepository.selectByCode(command.getCode());
        }
        LocalDateTime syncTime = command.getOccurredAt() != null ? command.getOccurredAt() : LocalDateTime.now();
        if (local == null) {
            Model model = Model.builder()
                    .code(command.getCode())
                    .name(command.getName())
                    .nameLocal(command.getNameLocal())
                    .carLineCode(command.getCarLineCode())
                    .platformCode(command.getPlatformCode())
                    .description(command.getDescription())
                    .source(SourceType.MDM)
                    .externalRefId(command.getExternalRefId())
                    .externalVersion(command.getExternalVersion())
                    .lastSyncTime(syncTime)
                    .build();
            mdmModelRepository.insert(model);
            log.info("Model 投影新增: code={}, externalRefId={}, version={}",
                    command.getCode(), command.getExternalRefId(), command.getExternalVersion());
        } else {
            if (command.getExternalVersion() <= local.getExternalVersion()) {
                log.info("Model 投影忽略旧版本: code={}, eventVersion={}, localVersion={}",
                        command.getCode(), command.getExternalVersion(), local.getExternalVersion());
                return;
            }
            local.setName(command.getName());
            local.setNameLocal(command.getNameLocal());
            local.setCarLineCode(command.getCarLineCode());
            local.setPlatformCode(command.getPlatformCode());
            local.setDescription(command.getDescription());
            local.setExternalRefId(command.getExternalRefId());
            local.setExternalVersion(command.getExternalVersion());
            local.setLastSyncTime(syncTime);
            mdmModelRepository.updateById(local);
            log.info("Model 投影更新: code={}, version={}",
                    command.getCode(), command.getExternalVersion());
        }
    }

    /**
     * 删除/失效事件处理：按现有投影删除语义逻辑删除，不物理级联删除车辆历史事实与下游映射
     *
     * @param event MDM Model 删除/失效事件
     */
    public void handleDeletion(MdmModelEvent event) {
        Model local = mdmModelRepository.selectByCode(event.getCode());
        if (local == null) {
            local = mdmModelRepository.selectByExternalRefId(event.getEntityId());
        }
        if (local == null) {
            log.info("Model 删除事件本地投影不存在: code={}, entityId={}",
                    event.getCode(), event.getEntityId());
            return;
        }
        if (event.getVersion() != null && event.getVersion() <= local.getExternalVersion()) {
            log.info("Model 删除事件忽略旧版本: code={}, eventVersion={}, localVersion={}",
                    event.getCode(), event.getVersion(), local.getExternalVersion());
            return;
        }
        mdmModelRepository.logicalDeleteById(local.getId());
        log.info("Model 投影逻辑删除: code={}, version={}",
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
