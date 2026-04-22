package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 车辆零件表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-27
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_part")
public class VmdVehiclePartDo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 零件编号
     */
    @TableField("pn")
    private String pn;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 设备代码
     */
    @TableField("device_code")
    private String deviceCode;

    /**
     * 设备项
     */
    @TableField("device_item")
    private String deviceItem;

    /**
     * 零件序列号
     */
    @TableField("sn")
    private String sn;

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
     * 绑定时间
     */
    @TableField("bind_time")
    private Date bindTime;

    /**
     * 绑定类型
     */
    @TableField("bind_type")
    private String bindType;

    /**
     * 绑定者
     */
    @TableField("bind_by")
    private String bindBy;

    /**
     * 绑定机构
     */
    @TableField("bind_org")
    private String bindOrg;

    /**
     * 解绑时间
     */
    @TableField("unbind_time")
    private Date unbindTime;

    /**
     * 解绑理由
     */
    @TableField("unbind_reason")
    private String unbindReason;

    /**
     * 解绑者
     */
    @TableField("unbind_by")
    private String unbindBy;

    /**
     * 解绑机构
     */
    @TableField("unbind_org")
    private String unbindOrg;

    /**
     * 零件状态：0-待绑定，1-在用，2-待更换，3-已报废
     */
    @TableField("part_state")
    private Integer partState;
}
