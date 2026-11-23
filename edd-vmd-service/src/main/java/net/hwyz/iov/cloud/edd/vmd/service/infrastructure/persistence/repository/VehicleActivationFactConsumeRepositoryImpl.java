package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationFactConsume;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ActivationFactConsumeStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationFactConsumeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleActivationFactConsumeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehicleActivationFactConsumeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleActivationFactConsumePo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 车辆首次激活候选事实消费台账 仓储实现（VMD-DSN-CR-077 / D-VMD-077-03）
 * <p>
 * 并发同业务键插入由唯一键（{@code uk_veh_act_consume_event} /
 * {@code uk_veh_act_consume_business}）兜底：捕获 {@link DuplicateKeyException} 返回 0 行，
 * 由应用层重读后按「重复 / 真冲突」重判，绝不静默吞掉证据。
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleActivationFactConsumeRepositoryImpl implements VehicleActivationFactConsumeRepository {

    private final VehicleActivationFactConsumeMapper vehicleActivationFactConsumeMapper;

    @Override
    public VehicleActivationFactConsume selectByEventId(String eventId) {
        LambdaQueryWrapper<VehicleActivationFactConsumePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleActivationFactConsumePo::getEventId, eventId)
                .eq(VehicleActivationFactConsumePo::getRowValid, true);
        return VehicleActivationFactConsumeConverter.INSTANCE.toDomain(
                vehicleActivationFactConsumeMapper.selectOne(wrapper));
    }

    @Override
    public VehicleActivationFactConsume selectByBusinessKey(String vin, String bindingOperationId) {
        LambdaQueryWrapper<VehicleActivationFactConsumePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleActivationFactConsumePo::getVin, vin)
                .eq(VehicleActivationFactConsumePo::getBindingOperationId, bindingOperationId)
                .eq(VehicleActivationFactConsumePo::getRowValid, true);
        return VehicleActivationFactConsumeConverter.INSTANCE.toDomain(
                vehicleActivationFactConsumeMapper.selectOne(wrapper));
    }

    @Override
    public VehicleActivationFactConsume selectLatestIneffectiveByVin(String vin) {
        LambdaQueryWrapper<VehicleActivationFactConsumePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleActivationFactConsumePo::getVin, vin)
                .in(VehicleActivationFactConsumePo::getStatus,
                        ActivationFactConsumeStatus.QUARANTINED.name(),
                        ActivationFactConsumeStatus.FAILED.name())
                .eq(VehicleActivationFactConsumePo::getApplied, 0)
                .eq(VehicleActivationFactConsumePo::getRowValid, true)
                .orderByDesc(VehicleActivationFactConsumePo::getReceivedAt)
                .last("LIMIT 1");
        List<VehicleActivationFactConsumePo> poList =
                vehicleActivationFactConsumeMapper.selectList(wrapper);
        if (poList == null || poList.isEmpty()) {
            return null;
        }
        return VehicleActivationFactConsumeConverter.INSTANCE.toDomain(poList.get(0));
    }

    @Override
    public int insert(VehicleActivationFactConsume consume) {
        VehicleActivationFactConsumePo po = VehicleActivationFactConsumeConverter.INSTANCE.fromDomain(consume);
        po.setId(null);
        if (po.getCreateTime() == null) {
            po.setCreateTime(new java.util.Date());
        }
        try {
            int result = vehicleActivationFactConsumeMapper.insert(po);
            if (result > 0 && po.getId() != null) {
                consume.setId(po.getId());
            }
            return result;
        } catch (DuplicateKeyException e) {
            // 并发同业务键/同事件写入：唯一键兜底，交由应用层重读重判
            log.debug("激活事实台账唯一键冲突（并发写入），转重读判定");
            return 0;
        }
    }

    @Override
    public int update(VehicleActivationFactConsume consume) {
        VehicleActivationFactConsumePo po = VehicleActivationFactConsumeConverter.INSTANCE.fromDomain(consume);
        po.setModifyTime(new java.util.Date());
        return vehicleActivationFactConsumeMapper.updateById(po);
    }

}
