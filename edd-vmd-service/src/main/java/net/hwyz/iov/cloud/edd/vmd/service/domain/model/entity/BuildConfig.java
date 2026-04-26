package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.Instant;

/**
 * 生产配置领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class BuildConfig implements DomainObj<BuildConfig> {

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
    private String seriesCode;

    /**
     * 车型代码
     */
    private String modelCode;

    /**
     * 基础车型代码
     */
    private String baseModelCode;

    /**
     * 生产配置代码
     */
    private String code;

    /**
     * 生产配置名称
     */
    private String name;

    /**
     * 车型配置英文名称
     */
    private String nameEn;

    /**
     * 车辆阶段代码
     */
    private String vehicleStageCode;

    /**
     * 外饰代码
     */
    private String exteriorCode;

    /**
     * 内饰代码
     */
    private String interiorCode;

    /**
     * 轮毂代码
     */
    private String wheelCode;
    /**
     * 轮胎代码
     */
    private String tireCode;

    /**
     * 备胎代码
     */
    private String spareTireCode;

    /**
     * 智驾代码
     */
    private String adasCode;

    /**
     * 座椅代码
     */
    private String seatCode;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

}
