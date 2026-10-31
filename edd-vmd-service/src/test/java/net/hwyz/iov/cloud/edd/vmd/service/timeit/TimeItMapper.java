package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * CR-073 G1 测试专用 mapper（XML 通道，namespace 必须与本接口全限定名一致）。
 * 仅用于隔离表 t_time_it，验证 XML resultMap/参数 typeHandler 双通道，不改生产 mapper。
 */
@Mapper
public interface TimeItMapper {

    /** 显式 typeHandler（FW UtcInstantTypeHandler）写入 c_dt6 */
    int insertWithUtcHandler(Map<String, Object> params);

    /** MyBatis 默认 InstantTypeHandler 写入 c_dt6（对照：旧路径） */
    int insertWithDefaultInstant(Map<String, Object> params);

    /** resultMap 读取（dt6 列 typeHandler=UtcInstantTypeHandler） */
    TimeItPo selectByRowKey(@Param("rowKey") String rowKey);
}
