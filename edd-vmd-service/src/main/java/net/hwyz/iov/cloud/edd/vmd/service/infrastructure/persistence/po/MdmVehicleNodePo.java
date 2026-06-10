package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

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

import java.time.LocalDateTime;

/**
 * 车载节点表 持久化对象
 *
 * <p>由 tb_device 重命名迁移而来（CR-020）。
 * MDM VehicleNode（车载节点，原Device设备）字典/类型主数据本地投影。</p>
 *
 * @author hwyz_leo
 * @since 2026-06-10
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_mdm_vehicle_node")
public class MdmVehicleNodePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车载节点代码（原deviceCode，CR-020重命名）
     */
    @TableField("code")
    private String code;

    /**
     * 车载节点名称
     */
    @TableField("name")
    private String name;

    /**
     * 车载节点英文名称
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 设备类型
     */
    @TableField("type")
    private String type;

    /**
     * 设备项（节点类型）
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

    /**
     * 数据来源：MDM=来自MDM系统，MANUAL=本地手动维护
     */
    @TableField("source")
    private String source;

    /**
     * MDM侧实体主键ID
     */
    @TableField("external_ref_id")
    private String externalRefId;

    /**
     * MDM侧实体版本号
     */
    @TableField("external_version")
    private Long externalVersion;

    /**
     * 最后一次同步时间
     */
    @TableField("last_sync_time")
    private LocalDateTime lastSyncTime;
}
