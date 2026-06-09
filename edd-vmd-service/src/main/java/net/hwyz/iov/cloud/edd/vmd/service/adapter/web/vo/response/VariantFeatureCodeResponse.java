package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台版本特征值 响应（原BaseModelFeatureCodeResponse，CR-016重命名）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantFeatureCodeResponse {

    /**
     * 主键
     */
    private Long id;

    /**
     * 版本代码
     */
    private String variantCode;

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

    /**
     * 创建时间
     */
    private Date createTime;

}