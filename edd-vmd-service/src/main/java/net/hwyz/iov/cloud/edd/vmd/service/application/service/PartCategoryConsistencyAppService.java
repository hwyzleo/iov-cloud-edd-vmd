package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoChangeLogRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.PartCategoryConsistencyMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartCategoryInconsistencyPo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 零件设备类别一致性巡检与受控修复（SEC-VIAM-DSN-CR-006 / D-VIAM-036）
 * <p>
 * 事实源固定为 `tb_mdm_vehicle_node.device_category`；`tb_part_info.device_category`
 * 为展示用派生冗余列（保留、不删除）。本服务只做：
 * <ol>
 *   <li>只读巡检：列出不一致清单；</li>
 *   <li>受控修复：期望类别必须等于当前 MDM 节点值、rowVersion 乐观锁、写属性变更历史；</li>
 *   <li>节点类别变更后的批量重算入口（按 vehicleNodeCode）。</li>
 * </ol>
 * 对外接口与绑定事件一律读取节点类别，本修复不改变任何安全判定口径。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartCategoryConsistencyAppService {

    /** 单次巡检返回上限 */
    private static final int MAX_PREVIEW_LIMIT = 500;

    /** 修复触发来源：人工受控修复（MPT） */
    public static final String TRIGGER_MANUAL = "MANUAL";

    /** 修复触发来源：MDM 节点类别变更自动收敛 */
    public static final String TRIGGER_NODE_CHANGED = "NODE_CHANGED";

    /** 修复触发来源：定时巡检自动收敛 */
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";

    private final PartCategoryConsistencyMapper consistencyMapper;
    private final PartInfoRepository partInfoRepository;
    private final MdmVehicleNodeRepository mdmVehicleNodeRepository;
    private final PartInfoChangeLogRepository partInfoChangeLogRepository;

    /**
     * 只读巡检：不一致总数 + 清单
     *
     * @param limit 清单上限（默认 100，最大 500）
     * @return 巡检结果
     */
    public PreviewResult preview(Integer limit) {
        int effectiveLimit = limit == null || limit <= 0 ? 100 : Math.min(limit, MAX_PREVIEW_LIMIT);
        long total = consistencyMapper.countInconsistent();
        List<PartCategoryInconsistencyPo> items = total == 0
                ? List.of() : consistencyMapper.selectInconsistent(effectiveLimit);
        log.info("零件类别一致性巡检: total={}, returned={}", total, items.size());
        return new PreviewResult(total, effectiveLimit, items);
    }

    /**
     * 受控修复（事务内逐项处理，逐项返回结果，不静默成功）
     * <p>
     * 两种用法：显式 items；或只给 vehicleNodeCode（节点类别变更后批量重算该节点下全部不一致零件）。
     * 默认按人工受控修复（{@link #TRIGGER_MANUAL}）记录审计。
     * </p>
     *
     * @param command      修复命令
     * @param operatorId   操作人ID（服务端注入）
     * @param operatorName 操作人名称
     * @return 修复结果
     */
    public RepairResult repair(RepairCommand command, String operatorId, String operatorName) {
        return repair(command, operatorId, operatorName, TRIGGER_MANUAL);
    }

    /**
     * 受控修复（按触发来源记录审计，CR-078 / D-VMD-78-06）
     *
     * @param command      修复命令
     * @param operatorId   操作人ID
     * @param operatorName 操作人名称
     * @param trigger      触发来源：MANUAL / NODE_CHANGED / SCHEDULED
     * @return 修复结果
     */
    @Transactional(rollbackFor = Exception.class)
    public RepairResult repair(RepairCommand command, String operatorId, String operatorName, String trigger) {
        String effectiveTrigger = isBlank(trigger) ? TRIGGER_MANUAL : trigger.trim();
        List<RepairItemCmd> items = resolveItems(command);
        String operationId = command.getOperationId() == null || command.getOperationId().isBlank()
                ? "CAT-CONSISTENCY-" + java.time.Instant.now().toEpochMilli()
                : command.getOperationId();
        List<RepairItemResult> results = new ArrayList<>(items.size());
        int applied = 0;
        int skipped = 0;
        int conflict = 0;
        int failed = 0;
        int seq = 0;
        for (RepairItemCmd item : items) {
            seq++;
            RepairItemResult result = repairOne(item, operatorId, operatorName, operationId, seq,
                    command.getReason(), command.getTicketNo(), effectiveTrigger);
            results.add(result);
            switch (result.getResult()) {
                case "APPLIED" -> applied++;
                case "SKIPPED" -> skipped++;
                case "CONFLICT" -> conflict++;
                default -> failed++;
            }
        }
        log.info("零件类别一致性修复: operationId={}, trigger={}, total={}, applied={}, skipped={}, conflict={}, failed={}",
                operationId, effectiveTrigger, items.size(), applied, skipped, conflict, failed);
        return new RepairResult(items.size(), applied, skipped, conflict, failed, results);
    }

    /**
     * 节点类别变更后批量重算（供节点更新链路或管理面调用）
     *
     * @param vehicleNodeCode 车载节点代码
     * @param operatorId      操作人
     * @param operatorName    操作人名称
     * @param reason          原因
     * @return 修复结果
     */
    @Transactional(rollbackFor = Exception.class)
    public RepairResult recomputeByVehicleNodeCode(String vehicleNodeCode, String operatorId,
                                                   String operatorName, String reason) {
        RepairCommand command = new RepairCommand();
        command.setVehicleNodeCode(vehicleNodeCode);
        command.setReason(reason);
        return repair(command, operatorId, operatorName, TRIGGER_NODE_CHANGED);
    }

    /**
     * 按节点收敛派生类别（独立事务，供自动路径使用，CR-078 / D-VMD-78-04）
     * <p>
     * 以 {@code REQUIRES_NEW} 开启独立事务：自动路径的失败不得把外层（MDM 节点投影/定时巡检）
     * 事务标记为回滚，也不影响 Kafka 消费位点。
     * </p>
     *
     * @param vehicleNodeCode 车载节点代码
     * @param trigger         触发来源（NODE_CHANGED / SCHEDULED）
     * @return 修复结果
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public RepairResult convergeByVehicleNodeCode(String vehicleNodeCode, String trigger) {
        RepairCommand command = new RepairCommand();
        command.setVehicleNodeCode(vehicleNodeCode);
        command.setReason("派生类别自动收敛（trigger=" + trigger + "）");
        command.setTicketNo("AUTO-CATEGORY-CONSISTENCY");
        return repair(command, "SYSTEM", "SYSTEM", trigger);
    }

    /**
     * 只读：统计派生类别与 MDM 节点类别不一致条数（CR-078 指标）
     *
     * @return 不一致条数
     */
    public long countInconsistent() {
        return consistencyMapper.countInconsistent();
    }

    /**
     * 只读：列出存在不一致的车载节点代码（CR-078 定时收敛分组键）
     *
     * @return 节点代码列表
     */
    public List<String> listInconsistentVehicleNodeCodes() {
        List<String> nodeCodes = consistencyMapper.selectInconsistentVehicleNodeCodes();
        return nodeCodes == null ? List.of() : nodeCodes;
    }

    /**
     * 解析待修复条目：显式 items 优先；否则按节点批量推导
     *
     * @param command 修复命令
     * @return 条目列表
     */
    private List<RepairItemCmd> resolveItems(RepairCommand command) {
        if (command.getItems() != null && !command.getItems().isEmpty()) {
            return command.getItems();
        }
        String vehicleNodeCode = command.getVehicleNodeCode();
        if (vehicleNodeCode == null || vehicleNodeCode.isBlank()) {
            throw new IllegalArgumentException("items 与 vehicleNodeCode 不能同时为空");
        }
        List<PartCategoryInconsistencyPo> poList =
                consistencyMapper.selectInconsistentByVehicleNodeCode(vehicleNodeCode.trim());
        List<RepairItemCmd> items = new ArrayList<>(poList.size());
        for (PartCategoryInconsistencyPo po : poList) {
            RepairItemCmd item = new RepairItemCmd();
            item.setPartCode(po.getPartCode());
            item.setSn(po.getSn());
            item.setExpectedCategory(po.getExpectedCategory());
            item.setExpectedRowVersion(po.getRowVersion());
            items.add(item);
        }
        return items;
    }

    /**
     * 修复单个零件类别
     *
     * @param item         条目
     * @param operatorId   操作人
     * @param operatorName 操作人名称
     * @param operationId  操作ID（幂等与审计）
     * @param seq          序号
     * @param reason       原因
     * @param ticketNo     工单号
     * @return 单项结果
     */
    private RepairItemResult repairOne(RepairItemCmd item, String operatorId, String operatorName,
                                       String operationId, int seq, String reason, String ticketNo,
                                       String trigger) {
        if (isBlank(item.getPartCode()) || isBlank(item.getSn())) {
            return RepairItemResult.failed(item, "partCode/sn 不能为空");
        }
        PartInfo part = partInfoRepository.selectByPartCodeAndSn(item.getPartCode(), item.getSn());
        if (part == null) {
            return RepairItemResult.failed(item, "零件实例不存在");
        }
        if (isBlank(part.getVehicleNodeCode())) {
            return RepairItemResult.failed(item, "零件未关联车载节点");
        }
        VehicleNode node = mdmVehicleNodeRepository.selectByCode(part.getVehicleNodeCode());
        if (node == null || isBlank(node.getDeviceCategory())) {
            return RepairItemResult.failed(item, "MDM 节点类别不可用");
        }
        String expected = node.getDeviceCategory().trim();
        if (!isBlank(item.getExpectedCategory())
                && !item.getExpectedCategory().trim().equalsIgnoreCase(expected)) {
            return RepairItemResult.failed(item, "期望类别与当前MDM节点类别不一致: expected="
                    + item.getExpectedCategory() + ", actual=" + expected);
        }
        String current = part.getDeviceCategory() == null ? null : part.getDeviceCategory().trim();
        if (expected.equalsIgnoreCase(current)) {
            return RepairItemResult.skipped(item, current, expected, part.getRowVersion(), "已一致");
        }
        if (item.getExpectedRowVersion() != null
                && !item.getExpectedRowVersion().equals(part.getRowVersion())) {
            return RepairItemResult.conflict(item, current, expected, part.getRowVersion(),
                    "版本冲突: current=" + part.getRowVersion());
        }
        String operator = isBlank(operatorId) ? "SYSTEM" : operatorId;
        PartInfo target = PartInfo.builder()
                .id(part.getId())
                .deviceCategory(expected)
                .modifyBy(operator)
                .build();
        int rows = partInfoRepository.patchByExpectedVersion(target, part.getRowVersion(), List.of());
        if (rows == 0) {
            return RepairItemResult.conflict(item, current, expected, part.getRowVersion(), "乐观锁更新失败");
        }
        int newRowVersion = part.getRowVersion() + 1;
        partInfoChangeLogRepository.insert(buildChangeLog(part, current, expected, operator, operatorName,
                operationId, seq, newRowVersion, reason, ticketNo, trigger));
        log.info("零件类别已按 MDM 节点收敛: partCode={}, sn={}, {} -> {}, version={}, trigger={}",
                part.getPartCode(), part.getSn(), current, expected, newRowVersion, trigger);
        return RepairItemResult.applied(item, current, expected, newRowVersion);
    }

    /**
     * 构造属性变更历史（同事务）
     * <p>
     * CR-078 / D-VMD-78-06：人工修复保持 MANUAL/MPT；自动触发（节点变更、定时）
     * 统一为 SYSTEM/SYSTEM，并在 metadata 中标明 trigger 与 nodeCode。
     * </p>
     */
    private PartInfoChangeLog buildChangeLog(PartInfo part, String current, String expected, String operatorId,
                                             String operatorName, String operationId, int seq, int newRowVersion,
                                             String reason, String ticketNo, String trigger) {
        boolean auto = trigger != null && !TRIGGER_MANUAL.equals(trigger);
        return PartInfoChangeLog.builder()
                .partId(part.getId())
                .changeType("ATTRIBUTE_UPDATE")
                .beforeJson("{\"deviceCategory\":" + jsonString(current) + "}")
                .afterJson("{\"deviceCategory\":" + jsonString(expected) + "}")
                .changedFields("[\"deviceCategory\"]")
                .source(auto ? "SYSTEM" : "MANUAL")
                .operationSource(auto ? "SYSTEM" : "MPT")
                .operatorId(operatorId)
                .operatorName(operatorName)
                .changeTime(LocalDateTime.now(ZoneOffset.UTC))
                .operationId(operationId)
                .mutationKey(sha256(operationId + ":" + part.getId() + ":CATEGORY_CONSISTENCY:" + seq))
                .rowVersion(newRowVersion)
                .metadataJson("{\"reason\":" + jsonString(reason) + ",\"ticketNo\":" + jsonString(ticketNo)
                        + ",\"expectedCategory\":" + jsonString(expected)
                        + ",\"trigger\":" + jsonString(trigger)
                        + ",\"nodeCode\":" + jsonString(part.getVehicleNodeCode()) + "}")
                .build();
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static byte[] sha256(String raw) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("mutationKey 计算失败", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 巡检结果
     */
    @Data
    public static class PreviewResult {

        /** 不一致总数 */
        private long total;

        /** 本次返回上限 */
        private int limit;

        /** 不一致清单 */
        private List<PartCategoryInconsistencyPo> items;

        public PreviewResult() {
        }

        public PreviewResult(long total, int limit, List<PartCategoryInconsistencyPo> items) {
            this.total = total;
            this.limit = limit;
            this.items = items;
        }
    }

    /**
     * 修复命令
     */
    @Data
    public static class RepairCommand {

        /** 显式修复条目 */
        private List<RepairItemCmd> items;

        /** 按节点批量重算（items 为空时生效） */
        private String vehicleNodeCode;

        /** 幂等键/操作ID（可空，服务端生成） */
        private String operationId;

        /** 原因（必填） */
        private String reason;

        /** 工单号（必填） */
        private String ticketNo;
    }

    /**
     * 修复条目
     */
    @Data
    public static class RepairItemCmd {

        /** 零件编码 */
        private String partCode;

        /** 零件序列号 */
        private String sn;

        /** 期望类别（必须等于当前 MDM 节点类别；可空） */
        private String expectedCategory;

        /** 期望业务版本（乐观锁；可空） */
        private Integer expectedRowVersion;
    }

    /**
     * 修复结果
     */
    @Data
    public static class RepairResult {

        private int total;
        private int applied;
        private int skipped;
        private int conflict;
        private int failed;
        private List<RepairItemResult> items;

        public RepairResult() {
        }

        public RepairResult(int total, int applied, int skipped, int conflict, int failed,
                            List<RepairItemResult> items) {
            this.total = total;
            this.applied = applied;
            this.skipped = skipped;
            this.conflict = conflict;
            this.failed = failed;
            this.items = items;
        }
    }

    /**
     * 单项修复结果
     */
    @Data
    public static class RepairItemResult {

        private String partCode;
        private String sn;
        /** APPLIED / SKIPPED / CONFLICT / FAILED */
        private String result;
        private String currentCategory;
        private String expectedCategory;
        private Integer newRowVersion;
        private String message;

        static RepairItemResult applied(RepairItemCmd item, String current, String expected, int newRowVersion) {
            return of(item, "APPLIED", current, expected, newRowVersion, null);
        }

        static RepairItemResult skipped(RepairItemCmd item, String current, String expected,
                                        Integer rowVersion, String message) {
            return of(item, "SKIPPED", current, expected, rowVersion, message);
        }

        static RepairItemResult conflict(RepairItemCmd item, String current, String expected,
                                         Integer rowVersion, String message) {
            return of(item, "CONFLICT", current, expected, rowVersion, message);
        }

        static RepairItemResult failed(RepairItemCmd item, String message) {
            return of(item, "FAILED", null, item == null ? null : item.getExpectedCategory(), null, message);
        }

        private static RepairItemResult of(RepairItemCmd item, String result, String current, String expected,
                                           Integer rowVersion, String message) {
            RepairItemResult r = new RepairItemResult();
            r.partCode = item == null ? null : item.getPartCode();
            r.sn = item == null ? null : item.getSn();
            r.result = result;
            r.currentCategory = current;
            r.expectedCategory = expected;
            r.newRowVersion = rowVersion;
            r.message = message;
            return r;
        }
    }
}
