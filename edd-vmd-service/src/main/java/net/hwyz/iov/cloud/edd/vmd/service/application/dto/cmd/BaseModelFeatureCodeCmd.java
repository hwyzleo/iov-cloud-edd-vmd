package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基础车型特征值 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseModelFeatureCodeCmd {

    private Long id;
    private String baseModelCode;
    private String familyCode;
    private String familyName;
    private String[] featureCode;
    private String[] featureName;
    private String featureType;
    private String description;

}
