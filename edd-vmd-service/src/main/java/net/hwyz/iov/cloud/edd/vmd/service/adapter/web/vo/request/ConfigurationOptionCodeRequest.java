package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 管理后台配置选项值 请求（原BuildConfigFeatureCodeRequest→ConfigurationFeatureCodeRequest，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigurationOptionCodeRequest extends BaseRequest {

    private Long id;

    private String configurationCode;

    private String optionFamilyCode;

    private String optionFamilyName;

    private String[] optionCode;

    private String[] optionName;

    private String optionType;

}
