package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆生命周期对外服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/vehicleLifecycle/v1")
public class ServiceVehicleLifecycleController extends BaseController {

    private final VehicleLifecycleAppService vehicleLifecycleAppService;

    /**
     * 记录第一次申请车联终端证书节点
     *
     * @param vin 车架号
     * @return 结果
     */
    @PostMapping("/{vin}/recordFirstApplyTboxCertNode")
    public ApiResponse<Void> recordFirstApplyTboxCertNode(@PathVariable String vin) {
        log.info("内部服务请求记录车辆[{}]第一次申请车联终端证书节点", vin);
        vehicleLifecycleAppService.recordFirstApplyTboxCertNode(vin);
        return ApiResponse.ok();
    }

    /**
     * 记录第一次申请车联终端通讯密钥节点
     *
     * @param vin 车架号
     * @return 结果
     */
    @PostMapping("/{vin}/recordFirstApplyTboxCommSkNode")
    public ApiResponse<Void> recordFirstApplyTboxCommSkNode(@PathVariable String vin) {
        log.info("内部服务请求记录车辆[{}]第一次申请车联终端通讯密钥节点", vin);
        vehicleLifecycleAppService.recordFirstApplyTboxCommSkNode(vin);
        return ApiResponse.ok();
    }

    /**
     * 记录第一次申请中央计算平台证书节点
     *
     * @param vin 车架号
     * @return 结果
     */
    @PostMapping("/{vin}/recordFirstApplyCcpCertNode")
    public ApiResponse<Void> recordFirstApplyCcpCertNode(@PathVariable String vin) {
        log.info("内部服务请求记录车辆[{}]第一次申请中央计算平台证书节点", vin);
        vehicleLifecycleAppService.recordFirstApplyCcpCertNode(vin);
        return ApiResponse.ok();
    }

    /**
     * 记录第一次申请中央计算平台通讯密钥节点
     *
     * @param vin 车架号
     * @return 结果
     */
    @PostMapping("/{vin}/recordFirstApplyCcpCommSkNode")
    public ApiResponse<Void> recordFirstApplyCcpCommSkNode(@PathVariable String vin) {
        log.info("内部服务请求记录车辆[{}]第一次申请中央计算平台通讯密钥节点", vin);
        vehicleLifecycleAppService.recordFirstApplyCcpCommSkNode(vin);
        return ApiResponse.ok();
    }

    /**
     * 记录第一次申请信息娱乐模块平台证书节点
     *
     * @param vin 车架号
     * @return 结果
     */
    @PostMapping("/{vin}/recordFirstApplyIdcmCertNode")
    public ApiResponse<Void> recordFirstApplyIdcmCertNode(@PathVariable String vin) {
        log.info("内部服务请求记录车辆[{}]第一次申请信息娱乐模块平台证书节点", vin);
        vehicleLifecycleAppService.recordFirstApplyIdcmCertNode(vin);
        return ApiResponse.ok();
    }

    /**
     * 记录第一次申请信息娱乐模块平台通讯密钥节点
     *
     * @param vin 车架号
     * @return 结果
     */
    @PostMapping("/{vin}/recordFirstApplyIdcmCommSkNode")
    public ApiResponse<Void> recordFirstApplyIdcmCommSkNode(@PathVariable String vin) {
        log.info("内部服务请求记录车辆[{}]第一次申请信息娱乐模块平台通讯密钥节点", vin);
        vehicleLifecycleAppService.recordFirstApplyIdcmCommSkNode(vin);
        return ApiResponse.ok();
    }

    /**
     * 记录第一次申请智驾模块平台证书节点
     *
     * @param vin 车架号
     * @return 结果
     */
    @PostMapping("/{vin}/recordFirstApplyAdcmCertNode")
    public ApiResponse<Void> recordFirstApplyAdcmCertNode(@PathVariable String vin) {
        log.info("内部服务请求记录车辆[{}]第一次申请智驾模块平台证书节点", vin);
        vehicleLifecycleAppService.recordFirstApplyAdcmCertNode(vin);
        return ApiResponse.ok();
    }

    /**
     * 记录第一次申请智驾模块平台通讯密钥节点
     *
     * @param vin 车架号
     * @return 结果
     */
    @PostMapping("/{vin}/recordFirstApplyAdcmCommSkNode")
    public ApiResponse<Void> recordFirstApplyAdcmCommSkNode(@PathVariable String vin) {
        log.info("内部服务请求记录车辆[{}]第一次申请智驾模块平台通讯密钥节点", vin);
        vehicleLifecycleAppService.recordFirstApplyAdcmCommSkNode(vin);
        return ApiResponse.ok();
    }

}
