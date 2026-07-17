package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VmdOutboxPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 通用 Outbox 转换器
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Mapper
public interface VmdOutboxConverter {

    VmdOutboxConverter INSTANCE = Mappers.getMapper(VmdOutboxConverter.class);

    VmdOutbox toEntity(VmdOutboxPo po);

    VmdOutboxPo toPo(VmdOutbox entity);
}
