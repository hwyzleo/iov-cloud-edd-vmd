package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleLifecycleVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleLifecycleAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleLifecycleNodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleLifecycleNodeEnum;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehLifecycleRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 查询车辆生命周期信息
     *
     * @param vin 车架号
     * @return 车辆生命周期列表
     */
    public List<VehicleLifecycleVo> search(String vin) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        List<VehicleLifecycle> vehicleLifecycleList = vehLifecycleRepository.selectByMap(map);
        return PageUtil.convert(vehicleLifecycleList, VehicleLifecycleAssembler.INSTANCE::fromDomain);
    }

    /**
     * 获取车辆生命周期信息
     *
     * @param vin  车架号
     * @param node 生命周期节点
     * @return 车辆生命周期信息
     */
    public VehicleLifecycle getLifecycle(String vin, VehicleLifecycleNodeEnum node) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("node", node.name());
        List<VehicleLifecycle> list = vehLifecycleRepository.selectByMap(map);
        return list.isEmpty() ? null : list.get(0);
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
                .reachTime(new Date())
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
                .reachTime(new Date())
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
                .reachTime(eolTime)
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录绑定订单节点
     *
     * @param vin      车架号
     * @param orderNum 订单编号
     */
    public void recordBindOrderNode(String vin, String orderNum) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.ORDER_BIND)
                .reachTime(new Date())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录合格证打印节点
     *
     * @param vin      车架号
     * @param certDate 打印日期
     */
    public void recordCertificateNode(String vin, Date certDate) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.CERTIFICATE)
                .reachTime(certDate)
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 记录车辆激活节点
     *
     * @param vin       车架号
     * @param accountId 账号ID
     */
    public void recordVehicleActiveNode(String vin, String accountId) {
        VehicleLifecycleNode node = VehicleLifecycleNode.builder()
                .vin(vin)
                .node(VehicleLifecycleNodeEnum.VEHICLE_ACTIVE)
                .reachTime(new Date())
                .build();
        node.init();
        vehicleLifecycleNodeRepository.save(node);
    }

    /**
     * 根据车架号物理删除车辆生命周期
     *
     * @param vin 车架号
     * @return 影响行数
     */
    public int deleteVehicleLifecycleByVin(String vin) {
        return vehLifecycleRepository.physicalDeleteByVin(vin);
    }

}
