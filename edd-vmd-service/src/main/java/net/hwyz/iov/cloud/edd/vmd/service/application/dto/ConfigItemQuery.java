package net.hwyz.iov.cloud.edd.vmd.service.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 配置项查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class ConfigItemQuery {

    private String family;
    private String code;
    private String name;
    private Date beginTime;
    private Date endTime;

}
