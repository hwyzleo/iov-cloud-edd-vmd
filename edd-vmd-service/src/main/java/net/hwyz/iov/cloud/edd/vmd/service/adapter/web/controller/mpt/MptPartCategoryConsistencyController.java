package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartCategoryConsistencyAppService;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.exception.InputParamInvalidException;
import org.springframework.web.bind.annotation.*;

/**
 * 零件设备类别一致性管理接口（SEC-VIAM-DSN-CR-006 / D-VIAM-036）
 * <p>
 * 事实源为 MDM 车辆节点类别；`tb_part_info.device_category` 为展示用派生冗余列。
 * 提供只读巡检与受控修复（幂等、期望版本、属性变更历史）。不提供任意字段改写通道。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-24
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/importOperation/v1/category-consistency")
public class MptPartCategoryConsistencyController {

    private final PartCategoryConsistencyAppService partCategoryConsistencyAppService;

    /**
     * 只读巡检：不一致总数与清单
     *
     * @param limit 清单上限（默认 100，最大 500）
     * @return 巡检结果
     */
    @RequiresPermissions("vmd:importOperation:query")
    @GetMapping
    public ApiResponse<PartCategoryConsistencyAppService.PreviewResult> preview(
            @RequestParam(required = false) Integer limit) {
        log.info("管理后台用户[{}]查询零件类别一致性巡检", SecurityContextHolder.getUserName());
        return ApiResponse.ok(partCategoryConsistencyAppService.preview(limit));
    }

    /**
     * 受控修复：按显式条目或按车载节点批量重算
     *
     * @param command 修复命令（reason、ticketNo 必填）
     * @return 逐项修复结果
     */
    @RequiresPermissions("vmd:importOperation:execute")
    @PostMapping("/repair")
    public ApiResponse<PartCategoryConsistencyAppService.RepairResult> repair(
            @RequestBody PartCategoryConsistencyAppService.RepairCommand command) {
        if (command.getReason() == null || command.getReason().trim().isEmpty()) {
            throw new InputParamInvalidException("reason不能为空");
        }
        if (command.getTicketNo() == null || command.getTicketNo().trim().isEmpty()) {
            throw new InputParamInvalidException("ticketNo不能为空");
        }
        if ((command.getItems() == null || command.getItems().isEmpty())
                && (command.getVehicleNodeCode() == null || command.getVehicleNodeCode().trim().isEmpty())) {
            throw new InputParamInvalidException("items与vehicleNodeCode不能同时为空");
        }
        String userId = SecurityUtils.getUserId() != null ? SecurityUtils.getUserId().toString() : "SYSTEM";
        String userName = SecurityContextHolder.getUserName();
        log.info("管理后台用户[{}]零件类别一致性修复: nodeCode={}, items={}, reason={}, ticketNo={}",
                userName, command.getVehicleNodeCode(),
                command.getItems() == null ? 0 : command.getItems().size(),
                command.getReason(), command.getTicketNo());
        return ApiResponse.ok(partCategoryConsistencyAppService.repair(command, userId, userName));
    }
}
