package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置选项值 DTO（原BuildConfigFeatureCodeCmd→ConfigurationFeatureCodeCmd，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationOptionCodeCmd {

    private Long id;
    private String configurationCode;
    private String optionFamilyCode;
    private String optionFamilyName;
    private String[] optionCode;
    private String[] optionName;
    private String optionType;
    private String description;

}
