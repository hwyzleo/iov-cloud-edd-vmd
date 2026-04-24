package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 车辆生命周期节点枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum VehicleLifecycleNodeEnum {

    /** 车辆生产 **/
    PRODUCE,
    /** 车辆生成密钥及安全常量 **/
    IMMO_SK,
    /** 车联终端申请证书 **/
    TBOX_CERT,
    /** 车联终端申请通讯密钥 **/
    TBOX_COMM_SK,
    /** 中央计算平台申请证书 **/
    CCP_CERT,
    /** 中央计算平台申请通讯密钥 **/
    CCP_COMM_SK,
    /** 智驾模块申请证书 **/
    ADCM_CERT,
    /** 智驾模块申请通讯密钥 **/
    ADCM_COMM_SK,
    /** 信息娱乐模块申请证书 **/
    IDCM_CERT,
    /** 信息娱乐模块申请通讯密钥 **/
    IDCM_COMM_SK,
    /** 车辆下线 **/
    EOL,
    /** 打印合格证 **/
    CERTIFICATE,
    /** 入前置库 **/
    PDC_INBOUND,
    /** 车辆订单绑定 **/
    ORDER_BIND,
    /** PDI电检 **/
    PDI,
    /** SIM卡绑定 **/
    SIM_BIND,
    /** 车辆发运 **/
    VEHICLE_SHIPPING,
    /** 车辆开票 **/
    VEHICLE_INVoICING,
    /** 车主绑定 **/
    VEHICLE_OWNER_BIND,
    /** 车辆激活 **/
    VEHICLE_ACTIVE,
    /** 车辆交付 **/
    VEHICLE_DELIVERY,
    /** 高精地图激活 **/
    HD_MAP_ACTIVE,
    /** RTK激活 **/
    RTK_ACTIVE;

    public static VehicleLifecycleNodeEnum valOf(String val) {
        return Arrays.stream(VehicleLifecycleNodeEnum.values())
                .filter(vehicleLifecycleNode -> vehicleLifecycleNode.name().equals(val))
                .findFirst()
                .orElse(null);
    }

}
