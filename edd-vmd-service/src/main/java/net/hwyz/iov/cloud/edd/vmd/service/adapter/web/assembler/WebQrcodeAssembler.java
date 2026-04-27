package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.QrcodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.QrcodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 网页端二维码 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface WebQrcodeAssembler {

    WebQrcodeAssembler INSTANCE = Mappers.getMapper(WebQrcodeAssembler.class);

    /**
     * DTO 转响应 VO
     *
     * @param qrcodeDto 二维码 DTO
     * @return 二维码响应 VO
     */
    QrcodeResponse fromDto(QrcodeDto qrcodeDto);

}
