package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.*;

/**
 * VMD生产配置选项值响应（原VmdBuildConfigFeatureCodeResponse，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdBuildConfigOptionCodeResponse {

    private Long id;

    private String buildConfigCode;

    private String optionFamilyCode;

    private String optionFamilyName;

    private String[] optionCode;

    private String[] optionName;

    private String optionType;

}
