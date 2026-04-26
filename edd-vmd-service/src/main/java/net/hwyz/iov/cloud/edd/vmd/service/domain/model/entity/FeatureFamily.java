package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.Instant;

/**
 * 特征族领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class FeatureFamily implements DomainObj<FeatureFamily> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 特征族代码
     */
    private String code;

    /**
     * 特征族名称
     */
    private String name;

    /**
     * 特征族英文名称
     */
    private String nameEn;

    /**
     * 特征族分类
     */
    private String type;

    /**
     * 是否强制
     */
    private Boolean mandatory;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

}
