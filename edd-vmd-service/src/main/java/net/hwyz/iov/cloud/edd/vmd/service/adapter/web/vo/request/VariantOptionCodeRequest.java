package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 管理后台版本选项值 请求（原BaseModelFeatureCodeRequest→VariantFeatureCodeRequest，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VariantOptionCodeRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 版本代码
     */
    private String variantCode;

    /**
     * 选项族代码
     */
    private String optionFamilyCode;

    /**
     * 选项族名称
     */
    private String optionFamilyName;

    /**
     * 选项值代码
     */
    private String[] optionCode;

    /**
     * 选项值名称
     */
    private String[] optionName;

    /**
     * 选项值类型
     */
    private String optionType;

}
