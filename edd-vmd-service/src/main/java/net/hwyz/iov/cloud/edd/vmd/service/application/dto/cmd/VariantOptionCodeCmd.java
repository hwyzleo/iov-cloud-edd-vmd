package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 版本选项值 DTO（原BaseModelFeatureCodeCmd→VariantFeatureCodeCmd，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantOptionCodeCmd {

    private Long id;
    private String variantCode;
    private String optionFamilyCode;
    private String optionFamilyName;
    private String[] optionCode;
    private String[] optionName;
    private String optionType;
    private String description;

}
