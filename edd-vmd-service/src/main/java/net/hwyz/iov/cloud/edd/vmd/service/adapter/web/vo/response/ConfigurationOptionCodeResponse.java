package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台配置选项值 响应（原BuildConfigFeatureCodeResponse→ConfigurationFeatureCodeResponse，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationOptionCodeResponse {

    private Long id;

    private String configurationCode;

    private String optionFamilyCode;

    private String optionFamilyName;

    private String[] optionCode;

    private String[] optionName;

    private String optionType;

    private Date createTime;

}
