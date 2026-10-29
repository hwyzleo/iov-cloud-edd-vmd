package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ImportOperationItemPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一导入操作明细表 Mapper 接口（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Mapper
public interface ImportOperationItemMapper extends BaseMapper<ImportOperationItemPo> {
}
