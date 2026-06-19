package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 单车选项值快照表 持久化对象
 * </p>
 *
 * @author VMD-DSN-CR-030 / US-043
 * @since 2026-06-19
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_option")
public class VehicleOptionPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车辆识别码
     */
    @TableField("vin")
    private String vin;

    /**
     * 选项族编码
     */
    @TableField("option_family_code")
    private String optionFamilyCode;

    /**
     * 选项值编码
     */
    @TableField("option_code")
    private String optionCode;

    /**
     * 数据来源
     */
    @TableField("source")
    private String source;

    /**
     * 导入批次号
     */
    @TableField("batch_num")
    private String batchNum;

    /**
     * 快照时间
     */
    @TableField("snapshot_time")
    private LocalDateTime snapshotTime;
}
