package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 车辆导入数据表 持久化对象
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_import_data")
public class VehImportDataPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 批次号
     */
    @TableField("batch_num")
    private String batchNum;

    /**
     * 车辆生命周期节点类型（本轮仅PRODUCE）
     */
    @TableField("type")
    private String type;

    /**
     * 数据版本
     */
    @TableField("version")
    private String version;

    /**
     * 原始报文
     */
    @TableField("data")
    private String data;

    /**
     * 处理状态：0-未处理，1-已处理
     */
    @TableField("handle")
    private Boolean handle;

    /**
     * 失败原因（按列长截断）
     */
    @TableField("description")
    private String description;
}
