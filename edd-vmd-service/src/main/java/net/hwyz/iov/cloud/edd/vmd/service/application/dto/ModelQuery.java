package net.hwyz.iov.cloud.edd.vmd.service.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 车型查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class ModelQuery {

    private String platformCode;
    private String seriesCode;
    private String code;
    private String name;
    private Date beginTime;
    private Date endTime;

}
