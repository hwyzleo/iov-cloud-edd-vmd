package net.hwyz.iov.cloud.edd.vmd.service.application.service;

/**
 * active TBOX 绑定的设备身份（CR-054）
 * <p>
 * VIN ↔ device_sn 定位 active 物理绑定后，解析出的权威 HSM UID（证书主体身份）。
 * device_sn 仅用于定位绑定 / 安装确认 / 追溯，hsm_uid 才是 CSR/证书 Subject CN 的期望值。
 *
 * @param vin            车辆VIN
 * @param bindingId      active vehicle_part.id
 * @param partId         设备物理实例（part_info.id）
 * @param deviceSn       TBOX 物理实例序列号
 * @param deviceCategory 设备类别
 * @param hsmUid         规范化权威 HSM UID（hsm_uid / ecu_uid）
 * @param uidSource      UID 来源：PART_INFO_EXTRA / SECURITY_CONSTANT / BOTH
 */
public record BoundDeviceIdentity(
        String vin,
        Long bindingId,
        Long partId,
        String deviceSn,
        String deviceCategory,
        String hsmUid,
        String uidSource
) {
}
