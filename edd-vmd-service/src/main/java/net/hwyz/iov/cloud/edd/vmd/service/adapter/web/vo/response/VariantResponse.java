package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台版本 响应
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantResponse {

    /**
     * 主键
     */
    private Long id;

    /**
     * 派生：车辆平台代码（经 Model.platformCode，CR-048）
     */
    private String platformCode;

    /**
     * 派生：车系代码（经 Model.carLineCode，CR-048）
     */
    private String carLineCode;

    /**
     * 车型代码
     */
    private String modelCode;

    /**
     * 版本代码
     */
    private String code;

    /**
     * 版本名称
     */
    private String name;

    /**
     * 版本本地化名称
     */
    private String nameLocal;

    /**
     * 备注
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

}
