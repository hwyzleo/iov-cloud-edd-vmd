package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 特征值查询 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
public class FeatureCodeQuery {

    private Long featureFamilyId;
    private String familyCode;
    private String name;
    private String featureCode;
    private Date beginTime;
    private Date endTime;

}
