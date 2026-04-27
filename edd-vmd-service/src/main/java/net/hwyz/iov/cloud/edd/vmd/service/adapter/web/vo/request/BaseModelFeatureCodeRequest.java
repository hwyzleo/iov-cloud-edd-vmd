package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 管理后台基础车型特征值 请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseModelFeatureCodeRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 基础车型代码
     */
    private String baseModelCode;

    /**
     * 特征族代码
     */
    private String familyCode;

    /**
     * 特征族名称
     */
    private String familyName;

    /**
     * 特征值代码
     */
    private String[] featureCode;

    /**
     * 特征值名称
     */
    private String[] featureName;

    /**
     * 特征值类型
     */
    private String featureType;

}
