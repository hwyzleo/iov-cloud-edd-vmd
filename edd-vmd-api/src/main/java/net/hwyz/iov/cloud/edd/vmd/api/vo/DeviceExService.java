package net.hwyz.iov.cloud.edd.vmd.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务车辆信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceExService {

    /**
     * 主键
     */
    private Long id;

    /**
     * 设备编码
     */
    private String code;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备英文名称
     */
    private String nameEn;

    /**
     * 设备类型
     */
    private String type;

    /**
     * 设备项
     */
    private String deviceItem;

    /**
     * 功能域
     */
    private String funcDomain;

    /**
     * OTA支持类型
     */
    private String otaSupport;

    /**
     * 分区类型
     */
    private String partitionType;

    /**
     * 解闭锁安全件
     */
    private Integer lockUnlockSecurityComponent;

    /**
     * 链路配置源
     */
    private String linkConfigSource;

    /**
     * 链路生效目标
     */
    private String linkFlashTarget;

    /**
     * CAN/CANFD总线发送标识
     */
    private String canTxId;

    /**
     * CAN/CANFD总线接收标识
     */
    private String canRxId;

    /**
     * 以太网的业务IP
     */
    private String ethernetIp;

    /**
     * DoIP协议网关标识
     */
    private String doipGatewayId;

    /**
     * DoIP协议设备标识
     */
    private String doipEntityId;

    /**
     * 是否核心设备
     */
    private Boolean core;

    /**
     * 排序
     */
    private Integer sort;

}
