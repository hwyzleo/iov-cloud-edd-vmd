package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessorRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;

/**
 * 下游联动（TSP/OTA/IDK）动作处理器抽象基类
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 复用正常导入后的 DownstreamProcessor（Adapter / Application Service），
 * 但不经过主体落库解析器入口（D34）。由当前 part_info + active 绑定快照重建
 * 对齐正常导入报文的 data JSON，调用携带 idempotencyKey；下游按业务唯一键幂等 upsert。
 * 不适用时返回 SKIPPED；超时 / 下游异常结果未知时标记 FAILED_RETRYABLE，不得无审计盲重试。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDownstreamSyncActionHandler implements PartPostProcessActionHandler {

    private final DownstreamProcessorRegistry downstreamProcessorRegistry;

    /**
     * 目标下游系统标识（TSP / OTA / IDK）
     */
    protected abstract String targetSystem();

    @Override
    public boolean supports(PartPostProcessActionContext context) {
        String nodeCode = resolveNodeCode(context);
        if (StrUtil.isBlank(nodeCode)) {
            return false;
        }
        DownstreamProcessor processor = downstreamProcessorRegistry.getProcessor(nodeCode);
        return processor != null && targetSystem().equals(processor.downstreamSystem());
    }

    @Override
    public String idempotencyKey(PartPostProcessActionContext context) {
        return String.join(":", context.getReplayId(), context.getPartCode(), context.getSn(), actionType());
    }

    @Override
    public PartPostProcessActionResult execute(PartPostProcessActionContext context) {
        String nodeCode = resolveNodeCode(context);
        if (StrUtil.isBlank(nodeCode)) {
            return PartPostProcessActionResult.skipped("NO_VEHICLE_NODE");
        }
        DownstreamProcessor processor = downstreamProcessorRegistry.getProcessor(nodeCode);
        if (processor == null) {
            return PartPostProcessActionResult.skipped("NO_DOWNSTREAM_PROCESSOR:" + nodeCode);
        }
        if (!targetSystem().equals(processor.downstreamSystem())) {
            return PartPostProcessActionResult.skipped("DOWNSTREAM_SYSTEM_MISMATCH:" + nodeCode);
        }

        JSONObject dataJson = buildDataJson(context);
        String idempotencyKey = idempotencyKey(context);
        try {
            processor.processReplay(context.getBatchNum(), context.getPartCode(), nodeCode, dataJson, idempotencyKey);
            log.info("零件[{}:{}]{}联动重放成功, nodeCode={}", context.getPartCode(), context.getSn(), targetSystem(), nodeCode);
            return PartPostProcessActionResult.success();
        } catch (Exception e) {
            // 超时 / 下游异常结果未知 → FAILED_RETRYABLE，不得无审计盲重试
            log.warn("零件[{}:{}]{}联动重放失败, nodeCode={}, idempotencyKey={}: {}",
                    context.getPartCode(), context.getSn(), targetSystem(), nodeCode, idempotencyKey, e.getMessage(), e);
            return PartPostProcessActionResult.failedRetryable(null, truncate(e.getMessage()));
        }
    }

    /**
     * 解析当前实例的车载节点代码（当前 part_info 优先，缺失时回退 MDM Part 投影）
     */
    protected String resolveNodeCode(PartPostProcessActionContext context) {
        if (context.getPartInfo() != null && StrUtil.isNotBlank(context.getPartInfo().getVehicleNodeCode())) {
            return context.getPartInfo().getVehicleNodeCode();
        }
        if (context.getMdmPart() != null && StrUtil.isNotBlank(context.getMdmPart().getVehicleNodeCode())) {
            return context.getMdmPart().getVehicleNodeCode();
        }
        return null;
    }

    /**
     * 从当前 part_info + active 绑定快照重建对齐正常导入报文的 data JSON
     */
    protected JSONObject buildDataJson(PartPostProcessActionContext context) {
        PartInfo partInfo = context.getPartInfo();
        VehiclePart binding = context.getActiveBinding();
        JSONObject extra = JSONUtil.parseObj(partInfo != null ? partInfo.getExtra() : null);

        JSONObject dataJson = new JSONObject();
        if (isItemBased()) {
            // TSP / IDK：REQUEST.HEAD.ACCOUNT + REQUEST.DATA.ITEMS 结构
            JSONObject head = new JSONObject();
            head.set("ACCOUNT", partInfo != null ? partInfo.getSupplierCode() : null);
            JSONObject data = new JSONObject();
            JSONArray items = new JSONArray();
            JSONObject item = new JSONObject();
            item.set("ASSEMBLY_PART_NO", context.getPartCode());
            item.set("HARDWARE_PART_NO", partInfo != null ? partInfo.getHardwarePn() : null);
            item.set("HARDWARE_VERSION", partInfo != null ? partInfo.getHardwareVer() : null);
            item.set("SN", context.getSn());
            item.set("ICCID1", extra != null ? extra.getStr("iccid1") : null);
            item.set("ICCID2", extra != null ? extra.getStr("iccid2") : null);
            item.set("HSM", extra != null ? extra.getStr("hsm") : null);
            item.set("MAC", extra != null ? extra.getStr("mac") : null);
            items.add(item);
            data.set("ITEMS", items);
            JSONObject request = new JSONObject();
            request.set("HEAD", head);
            request.set("DATA", data);
            dataJson.set("REQUEST", request);
        } else {
            // OTA：顶层字段结构（对齐 OtaDownstreamProcessor 读取口径）
            dataJson.set("vin", binding != null ? binding.getVin() : null);
            dataJson.set("sn", context.getSn());
            dataJson.set("deviceItem", binding != null ? binding.getDeviceItem() : null);
            dataJson.set("supplierCode", partInfo != null ? partInfo.getSupplierCode() : null);
            dataJson.set("configWord", partInfo != null ? partInfo.getConfigWord() : null);
            dataJson.set("hardwareVer", partInfo != null ? partInfo.getHardwareVer() : null);
            dataJson.set("softwareVer", partInfo != null ? partInfo.getSoftwareVer() : null);
            dataJson.set("hardwarePn", partInfo != null ? partInfo.getHardwarePn() : null);
            dataJson.set("softwarePn", partInfo != null ? partInfo.getSoftwarePn() : null);
        }
        return dataJson;
    }

    /**
     * 是否按 REQUEST/ITEMS 报文结构构造（TSP/IDK 为 true，OTA 为 false）
     */
    protected boolean isItemBased() {
        return true;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 900 ? message : message.substring(0, 897) + "...";
    }
}
