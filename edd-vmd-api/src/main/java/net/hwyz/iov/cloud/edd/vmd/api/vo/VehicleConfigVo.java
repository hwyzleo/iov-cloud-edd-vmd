package net.hwyz.iov.cloud.edd.vmd.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车辆配置
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleConfigVo extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 配置版本
     */
    private String version;

    /**
     * 配置状态
     */
    private String state;

    /**
     * 创建时间
     */
    private Date createTime;

}
