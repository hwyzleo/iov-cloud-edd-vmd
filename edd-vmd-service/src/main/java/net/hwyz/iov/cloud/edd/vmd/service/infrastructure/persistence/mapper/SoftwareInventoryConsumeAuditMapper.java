package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.SoftwareInventoryConsumeAuditPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * OTA 车辆软件观测消费审计表 Mapper 接口
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Mapper
public interface SoftwareInventoryConsumeAuditMapper extends BaseMapper<SoftwareInventoryConsumeAuditPo> {
}
