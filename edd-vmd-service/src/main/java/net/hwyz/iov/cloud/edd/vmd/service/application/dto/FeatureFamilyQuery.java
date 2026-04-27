package net.hwyz.iov.cloud.edd.vmd.service.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 特征族查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class FeatureFamilyQuery {

    private String code;
    private String name;
    private String type;
    private Date beginTime;
    private Date endTime;

}
