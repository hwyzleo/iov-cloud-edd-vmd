package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 零件导入数据表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-06-15
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_part_import_data")
public class PartImportDataPo extends BasePo {

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
     * 零件编码
     */
    @TableField("part_code")
    private String partCode;

    /**
     * 数据版本
     */
    @TableField("version")
    private String version;

    /**
     * 零件导入数据
     */
    @TableField("data")
    private String data;

    /**
     * 是否处理
     */
    @TableField("handle")
    private Boolean handle;
}
