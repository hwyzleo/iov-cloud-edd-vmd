package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleActivationNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleActivationNodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehicleActivationNodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleActivationNodePo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 车辆首次激活节点投影 仓储实现（VMD-DSN-CR-077 / D-VMD-077-04）
 * <p>
 * 并发与乱序安全：
 * <ol>
 *   <li>插入冲突（唯一键 {@code uk_veh_activation_node_vin}）不视为错误，返回 0 行由应用层重判</li>
 *   <li>向前纠正使用条件更新 {@code WHERE vin=? AND occurred_at > ?}，
 *       数据库层保证「只前不后」，即使应用层判定被并发绕过也不会回退首次激活时刻</li>
 * </ol>
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleActivationNodeRepositoryImpl implements VehicleActivationNodeRepository {

    private final VehicleActivationNodeMapper vehicleActivationNodeMapper;

    @Override
    public VehicleActivationNode selectByVin(String vin) {
        LambdaQueryWrapper<VehicleActivationNodePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleActivationNodePo::getVin, vin)
                .eq(VehicleActivationNodePo::getRowValid, true);
        return VehicleActivationNodeConverter.INSTANCE.toDomain(
                vehicleActivationNodeMapper.selectOne(wrapper));
    }

    @Override
    public int insert(VehicleActivationNode node) {
        VehicleActivationNodePo po = VehicleActivationNodeConverter.INSTANCE.fromDomain(node);
        po.setId(null);
        if (po.getCreateTime() == null) {
            po.setCreateTime(new java.util.Date());
        }
        try {
            int result = vehicleActivationNodeMapper.insert(po);
            if (result > 0 && po.getId() != null) {
                node.setId(po.getId());
            }
            return result;
        } catch (DuplicateKeyException e) {
            // 并发同 VIN 首次写入：由唯一键兜底，交由应用层重判（更早则纠正 / 否则忽略）
            log.debug("首次激活投影唯一键冲突（并发同VIN写入），转条件纠正判定");
            return 0;
        }
    }

    @Override
    public int correctIfEarlier(VehicleActivationNode node) {
        VehicleActivationNodePo po = VehicleActivationNodeConverter.INSTANCE.fromDomain(node);
        po.setId(null);
        po.setFirstActivatedAt(node.getOccurredAt());
        po.setModifyTime(new java.util.Date());
        LambdaUpdateWrapper<VehicleActivationNodePo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(VehicleActivationNodePo::getVin, node.getVin())
                .gt(VehicleActivationNodePo::getFirstActivatedAt, node.getOccurredAt())
                .eq(VehicleActivationNodePo::getRowValid, true);
        int result = vehicleActivationNodeMapper.update(po, wrapper);
        if (result > 0) {
            LocalDateTime now = LocalDateTime.now();
            node.setFirstActivatedAt(node.getOccurredAt());
            node.setModifyTime(now);
        }
        return result;
    }

}
