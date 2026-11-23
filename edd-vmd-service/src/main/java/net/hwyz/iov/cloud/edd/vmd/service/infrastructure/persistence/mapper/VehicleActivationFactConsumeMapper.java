package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleActivationFactConsumePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆首次激活候选事实消费台账表 Mapper 接口（VMD-DSN-CR-077）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleActivationFactConsumeMapper extends BaseMapper<VehicleActivationFactConsumePo> {
}
