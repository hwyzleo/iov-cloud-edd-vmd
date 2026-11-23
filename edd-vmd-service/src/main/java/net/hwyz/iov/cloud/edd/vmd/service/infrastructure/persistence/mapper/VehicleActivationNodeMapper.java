package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleActivationNodePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆首次激活节点投影表 Mapper 接口（VMD-DSN-CR-077）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleActivationNodeMapper extends BaseMapper<VehicleActivationNodePo> {
}
