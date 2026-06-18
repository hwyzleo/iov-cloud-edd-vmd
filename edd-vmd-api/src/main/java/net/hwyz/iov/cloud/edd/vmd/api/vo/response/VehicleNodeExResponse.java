package net.hwyz.iov.cloud.edd.vmd.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务车载节点信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleNodeExResponse {

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
     * 设备分类
     */
    private String deviceCategory;

    /**
     * 功能域
     */
    private String funcDomain;

    /**
     * 节点类型
     */
    private String nodeType;

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

}
