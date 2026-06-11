package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 物理零件实例查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class PartInfoQuery {

    private String partCode;
    private String sn;
    private String vehicleNodeCode;
    private Integer instanceState;
    private Date beginTime;
    private Date endTime;

    /**
     * 入站来源（CR-023）
     */
    private String source;

    /**
     * 零件类型（CR-023）
     */
    private String partType;

    /**
     * 入站批次号（CR-023）
     */
    private String inboundBatchNo;

}
