package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.Instant;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 物理零件实例本体表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-06-10
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_part_info")
public class PartInfoPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 零件编码（关联tb_mdm_part.pn）
     */
    @TableField("part_code")
    private String partCode;

    /**
     * 零件序列号
     */
    @TableField("sn")
    private String sn;

    /**
     * 车载节点代码（关联tb_mdm_vehicle_node.code）
     */
    @TableField("vehicle_node_code")
    private String vehicleNodeCode;

    /**
     * 配置字
     */
    @TableField("config_word")
    private String configWord;

    /**
     * 供应商编码
     */
    @TableField("supplier_code")
    private String supplierCode;

    /**
     * 批次号
     */
    @TableField("batch_num")
    private String batchNum;

    /**
     * 硬件版本号
     */
    @TableField("hardware_ver")
    private String hardwareVer;

    /**
     * 软件版本号
     */
    @TableField("software_ver")
    private String softwareVer;

    /**
     * 硬件零件号
     */
    @TableField("hardware_pn")
    private String hardwarePn;

    /**
     * 软件零件号
     */
    @TableField("software_pn")
    private String softwarePn;

    /**
     * 附加信息
     */
    @TableField("extra")
    private String extra;

    /**
     * 实例状态：0-在库，1-在用，2-待更换，3-已报废
     */
    @TableField("instance_state")
    private Integer instanceState;

    /**
     * 首次入库时间
     */
    @TableField("first_seen_time")
    private Instant firstSeenTime;

    /**
     * 入站来源系统：MES-制造执行系统, MANUAL-手动导入, WMS-仓储管理系统, IQC-来料检验, OTHER-其他
     */
    @TableField("source")
    private String source;

    /**
     * 零件类型快照：TBOX-车载终端, BTM-蓝牙模块, CCP-域控制器, IDCM-智能驾驶控制, SIM-SIM卡, OTHER-其他
     */
    @TableField("part_type")
    private String partType;

    /**
     * 入站批次号（批次级幂等去重键）
     */
    @TableField("inbound_batch_no")
    private String inboundBatchNo;

    /**
     * 源事件ID（事件级幂等去重键）
     */
    @TableField("source_event_id")
    private String sourceEventId;

    /**
     * 最近一次入站upsert时间
     */
    @TableField("last_inbound_time")
    private Instant lastInboundTime;
}
