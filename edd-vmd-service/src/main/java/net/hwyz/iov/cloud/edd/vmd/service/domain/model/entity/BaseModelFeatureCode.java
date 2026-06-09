package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.Instant;

/**
 * 基础车型特征值关系领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class BaseModelFeatureCode implements DomainObj<BaseModelFeatureCode> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 版本代码（原baseModelCode，CR-016重命名）
     */
    private String variantCode;

    /**
     * @deprecated Use {@link #getVariantCode()} instead. Kept for backward compatibility.
     */
    @Deprecated
    public String getBaseModelCode() {
        return variantCode;
    }

    /**
     * @deprecated Use {@link #setVariantCode(String)} instead. Kept for backward compatibility.
     */
    @Deprecated
    public void setBaseModelCode(String baseModelCode) {
        this.variantCode = baseModelCode;
    }

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
    private String featureCode;

    /**
     * 特征值名称
     */
    private String featureName;

    /**
     * 特征类型
     */
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
