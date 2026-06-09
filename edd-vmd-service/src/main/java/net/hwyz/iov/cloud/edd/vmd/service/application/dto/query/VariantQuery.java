package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 版本查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class VariantQuery {

    private String platformCode;
    private String carLineCode;
    private String modelCode;
    private String code;
    private String name;
    private Date beginTime;
    private Date endTime;

}
