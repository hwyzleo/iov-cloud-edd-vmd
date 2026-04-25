package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.util.Date;

/**
 * 设备领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class Device extends BaseDo<Long> implements DomainObj<Device> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 备注
     */
    private String description;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改者
     */
    private String modifyBy;

    /**
     * 修改时间
     */
    private Date modifyTime;

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
     * 节点类型
     */
    private String nodeType;

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
     * 通信协议
     */
    private String commProtocol;

    /**
     * 刷写协议
     */
    private String flashProtocol;

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
