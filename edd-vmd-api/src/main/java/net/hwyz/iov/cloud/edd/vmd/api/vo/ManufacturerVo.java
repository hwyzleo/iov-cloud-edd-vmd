package net.hwyz.iov.cloud.edd.vmd.api.vo;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车辆厂商
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ManufacturerVo extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车辆平台代码
     */
    private String code;

    /**
     * 车辆平台名称
     */
    private String name;

    /**
     * 车辆平台英文名称
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
