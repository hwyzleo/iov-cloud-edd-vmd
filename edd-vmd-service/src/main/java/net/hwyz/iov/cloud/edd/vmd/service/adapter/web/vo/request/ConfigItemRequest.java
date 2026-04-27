package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台配置项
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigItemRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 配置项大类
     */
    private String family;

    /**
     * 配置项编码
     */
    private String code;

    /**
     * 配置项名称
     */
    private String name;

    /**
     * 配置项类型
     */
    private String type;

    /**
     * 配置项单位
     */
    private String unit;

    /**
     * 是否车辆能力
     */
    private Boolean capability;

    /**
     * 端上是否展示
     */
    private Boolean display;

    /**
     * 端上是否缓存
     */
    private Boolean cache;

    /**
     * 创建时间
     */
    private Date createTime;

}
