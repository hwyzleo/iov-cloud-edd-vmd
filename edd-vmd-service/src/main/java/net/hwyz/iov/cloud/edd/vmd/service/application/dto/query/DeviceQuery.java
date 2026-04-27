package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 设备查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class DeviceQuery {

    private String code;
    private String name;
    private String funcDomain;
    private Date beginTime;
    private Date endTime;

}
