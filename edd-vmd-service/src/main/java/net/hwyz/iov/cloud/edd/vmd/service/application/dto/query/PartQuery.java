package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 零件查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class PartQuery {

    private String key;
    private String code;
    private String name;
    private String partType;
    private String vehicleNodeCode;
    private Date beginTime;
    private Date endTime;

}
