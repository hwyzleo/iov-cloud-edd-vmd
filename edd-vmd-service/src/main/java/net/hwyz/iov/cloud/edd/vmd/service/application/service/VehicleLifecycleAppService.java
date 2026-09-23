package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleLifecycleAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleLifecycleDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehLifecycleRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * 车辆生命周期应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleLifecycleAppService {

    private final VehLifecycleRepository vehLifecycleRepository;
    private final VehicleLifecycleNodeRepository vehicleLifecycleNodeRepository;

    /**
     * 根据车架号查询车辆生命周期信息
     *
     * @param vin 车架号
     * @return 车辆生命周期 DTO 列表
     */
    public List<VehicleLifecycleDto> getVehicleLifecycleByVin(String vin) {
        List<VehicleLifecycle> list = vehLifecycleRepository.selectByVin(vin);
        return PageUtil.convert(list, VehicleLifecycleAssembler.INSTANCE::fromDomain);
    }

    /**
     * 获取车辆生命周期节点信息
     *
     * @param vin 车架号
     * @return 车辆生命周期节点信息
     */
    public VehicleLifecycleDto getVehicleLifecycleNode(String vin) {
        List<VehicleLifecycleNode> nodes = vehicleLifecycleNodeRepository.selectByVin(vin);
        return VehicleLifecycleAssembler.INSTANCE.fromNodes(vin, nodes);
    }

    /**
     * 记录车辆生产节点（VMD-DSN-CR-050 幂等：首次写入胜出，重复调用忽略）
     * <p>
     * 采用存在性判断 + 并发唯一键回查兜底，已存在或并发插入冲突且节点已存在时均视为成功，
     * 不更新已有 reachTime，不将重复节点包装成新的业务错误码。
     *
     * @param vin 车架号
     */
    public void recordProduceNode(String vin) {
        VehicleLifecycleNodeEnum node = VehicleLifecycleNodeEnum.PRODUCE;
        if (vehicleLifecycleNodeRepository.existsByVinAndNode(vin, node)) {
            log.debug("车辆生命周期节点已存在，跳过写入: vin={}, node={}", vin, node);
            return;
        }
        VehicleLifecycleNode lifecycleNode = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(node)
                .reachTime(Instant.now())
                .build();
        lifecycleNode.init();
        try {
            vehicleLifecycleNodeRepository.save(lifecycleNode);
        } catch (DuplicateKeyException ex) {
            // 并发竞态兜底：唯一键冲突但目标 VIN+PRODUCE 节点已存在，视为幂等成功
            if (vehicleLifecycleNodeRepository.existsByVinAndNode(vin, node)) {
                log.debug("并发写入生命周期节点冲突，回查已存在，视为幂等成功: vin={}, node={}", vin, node);
                return;
            }
            throw ex;
        }
    }

    /**
     * 记录车辆生成密钥节点
     *
     * @param vin 车架号
     */
    public void recordGenerateVehicleSkNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.IMMO_SK)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录车辆绑定订单节点
     *
     * @param vin 车架号
     */
    public void recordBindOrderNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.ORDER_BIND)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录车辆下线节点
     *
     * @param vin     车架号
     * @param eolTime 下线时间
     */
    public void recordEolNode(String vin, Date eolTime) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.EOL)
                .reachTime(eolTime == null ? null : eolTime.toInstant())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录总装上线节点
     *
     * @param vin     车架号
     * @param tolTime 总装上线时间
     */
    public void recordTolNode(String vin, Date tolTime) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.TOL)
                .reachTime(tolTime == null ? null : tolTime.toInstant())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录车辆合格证节点（幂等：首次写入胜出，重复调用忽略）
     *
     * @param vin             车架号
     * @param certificateTime 合格证打印时间
     */
    public void recordCertificateNode(String vin, Date certificateTime) {
        VehicleLifecycleNodeEnum node = VehicleLifecycleNodeEnum.CERTIFICATE;
        if (vehicleLifecycleNodeRepository.existsByVinAndNode(vin, node)) {
            log.debug("车辆生命周期节点已存在，跳过写入: vin={}, node={}", vin, node);
            return;
        }
        VehicleLifecycleNode lifecycleNode = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(node)
                .reachTime(certificateTime == null ? null : certificateTime.toInstant())
                .build();
        lifecycleNode.init();
        try {
            vehicleLifecycleNodeRepository.save(lifecycleNode);
        } catch (DuplicateKeyException ex) {
            // 并发竞态兜底：唯一键冲突但目标 VIN+CERTIFICATE 节点已存在，视为幂等成功
            if (vehicleLifecycleNodeRepository.existsByVinAndNode(vin, node)) {
                log.debug("并发写入生命周期节点冲突，回查已存在，视为幂等成功: vin={}, node={}", vin, node);
                return;
            }
            throw ex;
        }
    }

    /**
     * 记录车辆下电节点（CR-043，幂等：首次写入胜出，重复调用忽略）
     *
     * @param vin           车架号
     * @param powerDownTime 下电时间
     */
    public void recordPowerDownNode(String vin, Instant powerDownTime) {
        VehicleLifecycleNodeEnum node = VehicleLifecycleNodeEnum.POWER_DOWN;
        if (vehicleLifecycleNodeRepository.existsByVinAndNode(vin, node)) {
            log.debug("车辆生命周期节点已存在，跳过写入: vin={}, node={}", vin, node);
            return;
        }
        VehicleLifecycleNode lifecycleNode = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(node)
                .reachTime(powerDownTime)
                .build();
        lifecycleNode.init();
        try {
            vehicleLifecycleNodeRepository.save(lifecycleNode);
        } catch (DuplicateKeyException ex) {
            // 并发竞态兜底：唯一键冲突但目标 VIN+POWER_DOWN 节点已存在，视为幂等成功
            if (vehicleLifecycleNodeRepository.existsByVinAndNode(vin, node)) {
                log.debug("并发写入生命周期节点冲突，回查已存在，视为幂等成功: vin={}, node={}", vin, node);
                return;
            }
            throw ex;
        }
    }

    /**
     * 记录车辆激活节点
     *
     * @param vin 车架号
     */
    public void recordVehicleActiveNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.VEHICLE_ACTIVE)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录第一次申请节点（幂等：首次写入胜出，重复调用忽略）
     *
     * @param vin      车架号
     * @param nodeCode 节点编码
     */
    public void recordFirstApplyNode(String vin, String nodeCode) {
        VehicleLifecycleNodeEnum nodeEnum = VehicleLifecycleNodeEnum.valOf(nodeCode);
        if (nodeEnum == null) {
            throw new IllegalArgumentException("无效的节点编码: " + nodeCode);
        }
        if (vehicleLifecycleNodeRepository.existsByVinAndNode(vin, nodeEnum)) {
            log.debug("车辆生命周期节点已存在，跳过写入: vin={}, node={}", vin, nodeCode);
            return;
        }
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(nodeEnum)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 根据车架号查询车辆生命周期时间线（按 reachTime 升序，空值末尾）
     *
     * @param vin 车架号
     * @return 车辆生命周期节点列表
     */
    public List<VehicleLifecycleNode> getVehicleTimelineByVin(String vin) {
        List<VehicleLifecycleNode> nodes = vehicleLifecycleNodeRepository.selectByVin(vin);
        nodes.sort((a, b) -> {
            if (a.getReachTime() == null && b.getReachTime() == null) return 0;
            if (a.getReachTime() == null) return 1;
            if (b.getReachTime() == null) return -1;
            return a.getReachTime().compareTo(b.getReachTime());
        });
        return nodes;
    }

    /**
     * 根据车架号删除车辆生命周期信息
     *
     * @param vin 车架号
     */
    public void deleteVehicleLifecycleByVin(String vin) {
        vehLifecycleRepository.physicalDeleteByVin(vin);
    }
}
