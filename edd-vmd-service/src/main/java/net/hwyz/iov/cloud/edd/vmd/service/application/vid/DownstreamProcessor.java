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

    /**
     * 获取所属下游系统标识（TSP / OTA / IDK 等）
     * <p>
     * VMD-DSN-CR-056：供零件导入后置处理重放按下游系统判定 TSP_SYNC / OTA_SYNC / IDK_SYNC 动作适用性。
     * 默认返回 UNKNOWN，具体处理器按各自下游系统覆写。
     *
     * @return 下游系统标识
     */
    default String downstreamSystem() {
        return "UNKNOWN";
    }

    /**
     * 重放场景的下游联动入口
     * <p>
     * VMD-DSN-CR-056：零件导入后置处理人工重放复用正常导入后的下游联动处理器，
     * 但不经过主体落库解析器入口。默认委托 {@link #process(String, String, String, JSONObject)}，
     * 需要按幂等键定制下游调用的处理器可覆写本方法。
     *
     * @param batchNum        原导入批次号
     * @param partCode        零件编码
     * @param vehicleNodeCode 车载节点代码
     * @param data            由当前 part_info 快照重建的数据 JSON（结构对齐正常导入报文）
     * @param idempotencyKey  动作幂等键（replayId + partCode + sn + actionType），供下游幂等 upsert
     */
    default void processReplay(String batchNum, String partCode, String vehicleNodeCode, JSONObject data, String idempotencyKey) {
        process(batchNum, partCode, vehicleNodeCode, data);
    }
}
