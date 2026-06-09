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

    /**
     * 选项族代码(原familyCode, CR-018别名)
     */
    private String optionFamilyCode;

    /**
     * 选项值代码(原featureCode, CR-018别名)
     */
    private String optionCode;

}
