package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdConfigurationFeatureCodeResponse {

    private Long id;

    private String configurationCode;

    private String familyCode;

    private String familyName;

    private String[] featureCode;

    private String[] featureName;

    private String featureType;

}
