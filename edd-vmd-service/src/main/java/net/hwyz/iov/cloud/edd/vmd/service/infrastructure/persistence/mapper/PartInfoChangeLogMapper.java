package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartInfoChangeLogPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 零件实例属性变更历史表 Mapper 接口（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Mapper
public interface PartInfoChangeLogMapper extends BaseMapper<PartInfoChangeLogPo> {

    /**
     * 按零件实例与业务版本查询
     *
     * @param partId 零件实例ID
     * @param rowVersion 业务版本
     * @return 变更日志
     */
    PartInfoChangeLogPo selectByPartIdAndRowVersion(@Param("partId") Long partId, @Param("rowVersion") Integer rowVersion);

}
