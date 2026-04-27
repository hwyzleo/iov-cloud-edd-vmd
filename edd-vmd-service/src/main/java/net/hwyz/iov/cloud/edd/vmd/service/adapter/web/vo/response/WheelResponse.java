package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台轮胎轮毂
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class WheelResponse {

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
     * 轮胎轮毂代码
     */
    private String code;

    /**
     * 轮胎轮毂名称
     */
    private String name;

    /**
     * 轮胎轮毂英文名称
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

    /**
     * 创建时间
     */
    private Date createTime;

}
