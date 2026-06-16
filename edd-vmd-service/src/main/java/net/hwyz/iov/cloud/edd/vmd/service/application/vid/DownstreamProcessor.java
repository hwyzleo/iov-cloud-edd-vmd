package net.hwyz.iov.cloud.edd.vmd.service.application.vid;

import cn.hutool.json.JSONObject;

/**
 * 下游处理器接口
 * 用于处理零件实例导入的下游联动（TSP/OTA/IDK）
 *
 * @author hwyz_leo
 */
public interface DownstreamProcessor {

    /**
     * 处理下游联动
     *
     * @param batchNum 批次号
     * @param partCode 零件编码
     * @param vehicleNodeCode 车载节点代码
     * @param data 数据JSON
     */
    void process(String batchNum, String partCode, String vehicleNodeCode, JSONObject data);

    /**
     * 获取支持的车载节点代码
     *
     * @return 车载节点代码
     */
    String getSupportedVehicleNodeCode();
}
