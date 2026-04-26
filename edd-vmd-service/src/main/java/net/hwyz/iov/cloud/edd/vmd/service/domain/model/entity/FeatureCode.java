package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.Instant;

/**
 * 特征值领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class FeatureCode implements DomainObj<FeatureCode> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 特征族代码
     */
    private String familyCode;

    /**
     * 特征值代码
     */
    private String code;

    /**
     * 特征值名称
     */
    private String name;

    /**
     * 特征值英文名称
     */
    private String nameEn;

    /**
     * 特征值代表值
     */
    private String val;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

}
