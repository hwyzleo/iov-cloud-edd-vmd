package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportEventReplayItem;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportEventReplayItemPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 车辆导入事件补发逐项动作审计转换器
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发逐项动作审计
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Mapper
public interface VehImportEventReplayItemConverter {

    VehImportEventReplayItemConverter INSTANCE = Mappers.getMapper(VehImportEventReplayItemConverter.class);

    VehImportEventReplayItem toEntity(VehImportEventReplayItemPo po);

    VehImportEventReplayItemPo toPo(VehImportEventReplayItem entity);
}
