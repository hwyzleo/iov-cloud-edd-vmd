package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车型
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelRequest extends BaseRequest {

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
    private String carLineCode;

    /**
     * 车型代码
     */
    private String code;

    /**
     * 车型名称
     */
    private String name;

    /**
     * 车型本地化名称
     */
    private String nameLocal;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

}
