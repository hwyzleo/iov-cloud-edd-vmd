package net.hwyz.iov.cloud.edd.vmd.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车辆
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleVo extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 工厂代码
     */
    private String manufacturerCode;

    /**
     * 品牌代码
     */
    private String brandCode;

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
    private String buildConfigCode;

    /**
     * 车辆下线时间
     */
    private Date eolTime;

    /**
     * 创建时间
     */
    private Date createTime;

}
