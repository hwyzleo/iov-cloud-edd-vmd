package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdBuildConfigFeatureCodeResponse {

    private Long id;

    private String buildConfigCode;

    private String familyCode;

    private String familyName;

    private String[] featureCode;

    private String[] featureName;

    private String featureType;

}