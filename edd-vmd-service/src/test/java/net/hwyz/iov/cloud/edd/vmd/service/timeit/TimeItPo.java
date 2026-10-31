package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.hwyz.iov.cloud.framework.mysql.typehandler.UtcInstantTypeHandler;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * CR-073 G1 测试专用候选 PO（隔离表 {@code t_time_it}，不指向任何生产表）。
 * <p>
 * {@code dt6} 声明 {@code @TableField(typeHandler = UtcInstantTypeHandler.class)} 为候选 MP 注解映射
 * （RD-TIME-003 字段级 opt-in）；本批经 XML resultMap 通道实测（TimeItXmlMappingIT），
 * MP 注解通道的运行时装配验证留待服务级装配（G2 前置），不默认发布到生产 PO。
 */
@Data
@TableName("t_time_it")
public class TimeItPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("row_key")
    private String rowKey;

    @TableField("c_ts")
    private Instant ts;

    @TableField("c_ts6")
    private Instant ts6;

    @TableField("c_dt")
    private LocalDateTime dt;

    @TableField("c_dt3")
    private LocalDateTime dt3;

    /** 候选字段级 opt-in：Instant ↔ UTC DATETIME(6)，保留微秒 */
    @TableField(value = "c_dt6", typeHandler = UtcInstantTypeHandler.class)
    private Instant dt6;
}
