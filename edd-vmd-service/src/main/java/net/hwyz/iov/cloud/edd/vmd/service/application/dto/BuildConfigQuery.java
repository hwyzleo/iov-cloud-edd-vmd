package net.hwyz.iov.cloud.edd.vmd.service.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 生产配置查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class BuildConfigQuery {

    private String platformCode;
    private String seriesCode;
    private String modelCode;
    private String baseModelCode;
    private String code;
    private String name;
    private Date beginTime;
    private Date endTime;

}
