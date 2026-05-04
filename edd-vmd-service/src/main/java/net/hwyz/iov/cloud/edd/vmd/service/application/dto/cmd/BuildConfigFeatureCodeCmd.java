package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildConfigFeatureCodeCmd {

    private Long id;
    private String buildConfigCode;
    private String familyCode;
    private String familyName;
    private String[] featureCode;
    private String[] featureName;
    private String featureType;
    private String description;

}