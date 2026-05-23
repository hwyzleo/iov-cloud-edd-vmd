package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;

import java.util.List;

/**
 * 车辆下线零件绑定完成事件
 * <p>
 * EOL 解析器完成零件入库后发布，由异步订阅者负责通知 TSP/OTA 下游服务
 *
 * @author hwyz_leo
 */
@Getter
public class VehicleEolPartBoundEvent extends BaseEvent {

    /**
     * 车架号
     */
    private final String vin;
    /**
     * 零件元数据列表（供 TSP/OTA 使用）
     */
    private final List<PartMeta> parts;

    public VehicleEolPartBoundEvent(String vin, List<PartMeta> parts) {
        super(vin);
        this.vin = vin;
        this.parts = parts;
    }

    /**
     * 零件元数据
     */
    @Getter
    public static class PartMeta {
        private final String sn;
        private final String pn;
        private final String deviceCode;
        private final String deviceItem;
        private final String supplierCode;
        private final String batchNum;
        private final String configWord;
        private final String hardwareVer;
        private final String softwareVer;
        private final String hardwarePn;
        private final String softwarePn;
        private final String iccid1;
        private final String iccid2;

        public PartMeta(String sn, String pn, String deviceCode, String deviceItem,
                        String supplierCode, String batchNum, String configWord,
                        String hardwareVer, String softwareVer, String hardwarePn,
                        String softwarePn, String iccid1, String iccid2) {
            this.sn = sn;
            this.pn = pn;
            this.deviceCode = deviceCode;
            this.deviceItem = deviceItem;
            this.supplierCode = supplierCode;
            this.batchNum = batchNum;
            this.configWord = configWord;
            this.hardwareVer = hardwareVer;
            this.softwareVer = softwareVer;
            this.hardwarePn = hardwarePn;
            this.softwarePn = softwarePn;
            this.iccid1 = iccid1;
            this.iccid2 = iccid2;
        }
    }

}
