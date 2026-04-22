package net.hwyz.iov.cloud.edd.vmd.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台生产配置
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BuildConfigVo extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车辆平台代码
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
     * 车型配置代码
     */
    private String code;

    /**
     * 车型配置名称
     */
    private String name;

    /**
     * 车系英文名称
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

    /**
     * 创建时间
     */
    private Date createTime;

}
