package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.Instant;

/**
 * 车系领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class Series implements DomainObj<Series> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 平台代码
     */
    private String platformCode;

    /**
     * 车系代码
     */
    private String code;

    /**
     * 车系名称
     */
    private String name;

    /**
     * 车系英文名称
     */
    private String nameEn;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

}
