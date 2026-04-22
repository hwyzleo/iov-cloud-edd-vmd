package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * <p>
 * 设备信息表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-26
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_device")
public class VmdDeviceDo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备编码
     */
    @TableField("code")
    private String code;

    /**
     * 设备名称
     */
    @TableField("name")
    private String name;

    /**
     * 设备英文名称
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 设备类型
     */
    @TableField("type")
    private String type;

    /**
     * 设备项
     */
    @TableField("device_item")
    private String deviceItem;

    /**
     * 功能域
     */
    @TableField("func_domain")
    private String funcDomain;

    /**
     * 节点类型
     */
    @TableField("node_type")
    private String nodeType;

    /**
     * OTA支持类型
     */
    @TableField("ota_support")
    private String otaSupport;

    /**
     * 分区类型
     */
    @TableField("partition_type")
    private String partitionType;

    /**
     * 解闭锁安全件
     */
    @TableField("lock_unlock_security_component")
    private Integer lockUnlockSecurityComponent;

    /**
     * 链路配置源
     */
    @TableField("link_config_source")
    private String linkConfigSource;

    /**
     * 链路生效目标
     */
    @TableField("link_flash_target")
    private String linkFlashTarget;

    /**
     * 通信协议
     */
    @TableField("comm_protocol")
    private String commProtocol;

    /**
     * 刷写协议
     */
    @TableField("flash_protocol")
    private String flashProtocol;

    /**
     * CAN/CANFD总线发送标识
     */
    @TableField("can_tx_id")
    private String canTxId;

    /**
     * CAN/CANFD总线接收标识
     */
    @TableField("can_rx_id")
    private String canRxId;

    /**
     * 以太网的业务IP
     */
    @TableField("ethernet_ip")
    private String ethernetIp;

    /**
     * DoIP协议网关标识
     */
    @TableField("doip_gateway_id")
    private String doipGatewayId;

    /**
     * DoIP协议设备标识
     */
    @TableField("doip_entity_id")
    private String doipEntityId;

    /**
     * 是否核心设备
     */
    @TableField("core")
    private Boolean core;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;
}
