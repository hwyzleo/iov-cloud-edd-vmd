package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 基础车型查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class BaseModelQuery {

    private String platformCode;
    private String seriesCode;
    private String modelCode;
    private String code;
    private String name;
    private Date beginTime;
    private Date endTime;

}
