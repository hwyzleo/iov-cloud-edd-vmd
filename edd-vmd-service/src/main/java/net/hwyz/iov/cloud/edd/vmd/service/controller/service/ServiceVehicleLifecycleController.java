package net.hwyz.iov.cloud.edd.vmd.service.controller.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.VehicleLifecycleNodeEnum;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆生命周期相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleLifecycle/v1")
public class ServiceVehicleLifecycleController {

    private final VehicleLifecycleAppService vehicleLifecycleAppService;

    /**
     * 记录第一次申请车联终端证书节点
     *
     * @param vin 车架号
     */
    @PostMapping("/{vin}/recordFirstApplyTboxCertNode")
    public void recordFirstApplyTboxCertNode(@PathVariable String vin) {
        if (ObjUtil.isNull(vehicleLifecycleAppService.getLifecycle(vin, VehicleLifecycleNodeEnum.TBOX_CERT))) {
            log.info("记录车辆[{}]第一次申请车联终端证书节点", vin);
            vehicleLifecycleAppService.createVehicleLifecycle(vin, VehicleLifecycleNodeEnum.TBOX_CERT);
        }
    }

    /**
     * 记录第一次申请车联终端通讯密钥节点
     *
     * @param vin 车架号
     */
    @PostMapping("/{vin}/recordFirstApplyTboxCommSkNode")
    public void recordFirstApplyTboxCommSkNode(@PathVariable String vin) {
        if (ObjUtil.isNull(vehicleLifecycleAppService.getLifecycle(vin, VehicleLifecycleNodeEnum.TBOX_COMM_SK))) {
            log.info("记录车辆[{}]第一次申请车联终端通讯密钥节点", vin);
            vehicleLifecycleAppService.createVehicleLifecycle(vin, VehicleLifecycleNodeEnum.TBOX_COMM_SK);
        }
    }

    /**
     * 记录第一次申请中央计算平台证书节点
     *
     * @param vin 车架号
     */
    @PostMapping("/{vin}/recordFirstApplyCcpCertNode")
    public void recordFirstApplyCcpCertNode(@PathVariable String vin) {
        if (ObjUtil.isNull(vehicleLifecycleAppService.getLifecycle(vin, VehicleLifecycleNodeEnum.CCP_CERT))) {
            log.info("记录车辆[{}]第一次申请中央计算平台证书节点", vin);
            vehicleLifecycleAppService.createVehicleLifecycle(vin, VehicleLifecycleNodeEnum.CCP_CERT);
        }
    }

    /**
     * 记录第一次申请中央计算平台通讯密钥节点
     *
     * @param vin 车架号
     */
    @PostMapping("/{vin}/recordFirstApplyCcpCommSkNode")
    public void recordFirstApplyCcpCommSkNode(@PathVariable String vin) {
        if (ObjUtil.isNull(vehicleLifecycleAppService.getLifecycle(vin, VehicleLifecycleNodeEnum.CCP_COMM_SK))) {
            log.info("记录车辆[{}]第一次申请中央计算平台通讯密钥节点", vin);
            vehicleLifecycleAppService.createVehicleLifecycle(vin, VehicleLifecycleNodeEnum.CCP_COMM_SK);
        }
    }

    /**
     * 记录第一次申请信息娱乐模块证书节点
     *
     * @param vin 车架号
     */
    @PostMapping("/{vin}/recordFirstApplyIdcmCertNode")
    public void recordFirstApplyIdcmCertNode(@PathVariable String vin) {
        if (ObjUtil.isNull(vehicleLifecycleAppService.getLifecycle(vin, VehicleLifecycleNodeEnum.IDCM_CERT))) {
            log.info("记录车辆[{}]第一次申请信息娱乐模块证书节点", vin);
            vehicleLifecycleAppService.createVehicleLifecycle(vin, VehicleLifecycleNodeEnum.IDCM_CERT);
        }
    }

    /**
     * 记录第一次申请信息娱乐模块通讯密钥节点
     *
     * @param vin 车架号
     */
    @PostMapping("/{vin}/recordFirstApplyIdcmCommSkNode")
    public void recordFirstApplyIdcmCommSkNode(@PathVariable String vin) {
        if (ObjUtil.isNull(vehicleLifecycleAppService.getLifecycle(vin, VehicleLifecycleNodeEnum.IDCM_COMM_SK))) {
            log.info("记录车辆[{}]第一次申请信息娱乐模块通讯密钥节点", vin);
            vehicleLifecycleAppService.createVehicleLifecycle(vin, VehicleLifecycleNodeEnum.IDCM_COMM_SK);
        }
    }

    /**
     * 记录第一次申请智驾模块证书节点
     *
     * @param vin 车架号
     */
    @PostMapping("/{vin}/recordFirstApplyAdcmCertNode")
    public void recordFirstApplyAdcmCertNode(@PathVariable String vin) {
        if (ObjUtil.isNull(vehicleLifecycleAppService.getLifecycle(vin, VehicleLifecycleNodeEnum.ADCM_CERT))) {
            log.info("记录车辆[{}]第一次申请智驾模块证书节点", vin);
            vehicleLifecycleAppService.createVehicleLifecycle(vin, VehicleLifecycleNodeEnum.ADCM_CERT);
        }
    }

    /**
     * 记录第一次申请智驾模块通讯密钥节点
     *
     * @param vin 车架号
     */
    @PostMapping("/{vin}/recordFirstApplyAdcmCommSkNode")
    public void recordFirstApplyAdcmCommSkNode(@PathVariable String vin) {
        if (ObjUtil.isNull(vehicleLifecycleAppService.getLifecycle(vin, VehicleLifecycleNodeEnum.ADCM_COMM_SK))) {
            log.info("记录车辆[{}]第一次申请智驾模块通讯密钥节点", vin);
            vehicleLifecycleAppService.createVehicleLifecycle(vin, VehicleLifecycleNodeEnum.ADCM_COMM_SK);
        }
    }

}
