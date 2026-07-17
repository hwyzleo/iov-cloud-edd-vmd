package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplay;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportEventReplayPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 车辆导入成功事件补发审计转换器
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Mapper
public interface VehImportEventReplayConverter {

    VehImportEventReplayConverter INSTANCE = Mappers.getMapper(VehImportEventReplayConverter.class);

    VehImportEventReplay toEntity(VehImportEventReplayPo po);

    VehImportEventReplayPo toPo(VehImportEventReplay entity);
}
