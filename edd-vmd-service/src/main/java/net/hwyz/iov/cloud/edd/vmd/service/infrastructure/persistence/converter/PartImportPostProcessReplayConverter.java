package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportPostProcessReplay;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartImportPostProcessReplayPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 零件导入后置处理重放主任务转换器
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Mapper
public interface PartImportPostProcessReplayConverter {

    PartImportPostProcessReplayConverter INSTANCE = Mappers.getMapper(PartImportPostProcessReplayConverter.class);

    PartImportPostProcessReplay toEntity(PartImportPostProcessReplayPo po);

    PartImportPostProcessReplayPo toPo(PartImportPostProcessReplay entity);
}
