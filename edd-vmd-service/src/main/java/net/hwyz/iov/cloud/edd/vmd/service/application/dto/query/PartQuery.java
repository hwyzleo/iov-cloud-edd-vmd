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
    private String pn;
    private String name;
    private String type;
    private String deviceCode;
    private Date beginTime;
    private Date endTime;

}
