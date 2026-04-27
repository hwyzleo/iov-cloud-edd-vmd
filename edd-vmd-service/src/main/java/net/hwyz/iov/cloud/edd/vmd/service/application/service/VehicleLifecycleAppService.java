package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleLifecycleAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleLifecycleDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehLifecycleRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * 车辆生命周期应用服务类
 *
 * @author hwyz_leo
 */
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
     * 记录车辆生产节点
     *
     * @param vin 车架号
     */
    public void recordProduceNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.PRODUCE)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
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
     * 记录车辆合格证节点
     *
     * @param vin             车架号
     * @param certificateTime 合格证打印时间
     */
    public void recordCertificateNode(String vin, Date certificateTime) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.CERTIFICATE)
                .reachTime(certificateTime == null ? null : certificateTime.toInstant())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
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
     * 记录第一次申请车联终端证书节点
     *
     * @param vin 车架号
     */
    public void recordFirstApplyTboxCertNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.TBOX_CERT)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录第一次申请车联终端通讯密钥节点
     *
     * @param vin 车架号
     */
    public void recordFirstApplyTboxCommSkNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.TBOX_COMM_SK)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录第一次申请中央计算平台证书节点
     *
     * @param vin 车架号
     */
    public void recordFirstApplyCcpCertNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.CCP_CERT)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录第一次申请中央计算平台通讯密钥节点
     *
     * @param vin 车架号
     */
    public void recordFirstApplyCcpCommSkNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.CCP_COMM_SK)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录第一次申请信息娱乐模块平台证书节点
     *
     * @param vin 车架号
     */
    public void recordFirstApplyIdcmCertNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.IDCM_CERT)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录第一次申请信息娱乐模块平台通讯密钥节点
     *
     * @param vin 车架号
     */
    public void recordFirstApplyIdcmCommSkNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.IDCM_COMM_SK)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录第一次申请智驾模块平台证书节点
     *
     * @param vin 车架号
     */
    public void recordFirstApplyAdcmCertNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.ADCM_CERT)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录第一次申请智驾模块平台通讯密钥节点
     *
     * @param vin 车架号
     */
    public void recordFirstApplyAdcmCommSkNode(String vin) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.ADCM_COMM_SK)
                .reachTime(Instant.now())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
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
