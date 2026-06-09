package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 版本特征值 DTO（原BaseModelFeatureCodeCmd，CR-016重命名）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantFeatureCodeCmd {

    private Long id;
    private String variantCode;
    private String familyCode;
    private String familyName;
    private String[] featureCode;
    private String[] featureName;
    private String featureType;
    private String description;

}