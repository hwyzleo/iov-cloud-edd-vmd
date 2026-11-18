package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

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
public class VehicleRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 生产工厂代码
     */
    private String plantCode;

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
    private String carLineCode;

    /**
     * 车型代码
     */
    private String modelCode;

    /**
     * 版本代码
     */
    private String variantCode;

    /**
     * 配置代码
     */
    private String configurationCode;

    /**
     * 车辆下线时间
     */
    private Date eolTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 车型配置代码
     * <p>
     * 仅用于可分配车辆查询：管理后台按订单车型配置代码筛选车辆，
     * 与配置代码（configurationCode）同义，未传时回退使用 configurationCode
     */
    private String modelConfigCode;

}
