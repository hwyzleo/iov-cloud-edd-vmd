package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.QrcodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Qrcode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 二维码 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface QrcodeAssembler {

    QrcodeAssembler INSTANCE = Mappers.getMapper(QrcodeAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param qrcode 领域对象
     * @return DTO
     */
    @Mapping(target = "state", source = "qrcodeState")
    QrcodeDto fromDomain(Qrcode qrcode);

}
