package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

/**
 * 管理后台车载节点信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleNodeRequest extends BaseRequest {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车载节点代码
     */
    private String code;

    /**
     * 车载节点名称
     */
    private String name;

    /**
     * 车载节点本地化名称
     */
    private String nameLocal;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 设备分类
     */
    private String deviceCategory;

    /**
     * 功能域
     */
    private String funcDomain;

    /**
     * OTA支持类型
     */
    private String otaSupport;

    /**
     * 是否核心设备
     */
    private Boolean core;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private Date createTime;

}
