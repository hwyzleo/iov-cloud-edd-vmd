package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ReplayVehicleImportEventRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehImportDataRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ImportResultResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ReplayEventResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehImportDataResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptVehImportDataAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehImportDataCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayEventResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehImportDataQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehImportEventReplayAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车辆导入数据相关管理接口实现类
 * <p>
 * VMD-DSN-CR-027: 车辆数据导入域独立化
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/vehImportData/v1")
public class MptVehImportDataController extends BaseController {

    private final VehImportDataAppService vehImportDataAppService;
    private final VehImportEventReplayAppService vehImportEventReplayAppService;

    @RequiresPermissions("completeVehicle:vehicle:importData:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<VehImportDataResponse>> list(VehImportDataRequest vehImportData) {
        log.info("管理后台用户[{}]分页查询车辆导入数据", SecurityContextHolder.getUserName());
        startPage();
        VehImportDataQuery query = VehImportDataQuery.builder()
                .batchNum(vehImportData.getBatchNum())
                .type(vehImportData.getType())
                .handle(vehImportData.getHandle())
                .beginTime(getBeginTime(vehImportData))
                .endTime(getEndTime(vehImportData))
                .build();
        List<VehImportDataDto> dtoList = vehImportDataAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(dtoList, MptVehImportDataAssembler.INSTANCE::fromDto)));
    }

    @Log(title = "车辆导入数据管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:importData:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, VehImportDataRequest vehImportData) {
        log.info("管理后台用户[{}]导出车辆导入数据", SecurityContextHolder.getUserName());
    }

    @RequiresPermissions("completeVehicle:vehicle:importData:query")
    @GetMapping(value = "/{vehImportDataId}")
    public ApiResponse<VehImportDataResponse> getInfo(@PathVariable Long vehImportDataId) {
        log.info("管理后台用户[{}]根据车辆导入数据ID[{}]获取车辆导入数据", SecurityContextHolder.getUserName(), vehImportDataId);
        return ApiResponse.ok(MptVehImportDataAssembler.INSTANCE.fromDto(vehImportDataAppService.getVehImportDataById(vehImportDataId)));
    }

    @Log(title = "车辆导入数据管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("completeVehicle:vehicle:importData:add")
    @PostMapping
    public ApiResponse<ImportResultResponse> add(@Validated @RequestBody VehImportDataRequest vehImportData) {
        log.info("管理后台用户[{}]新增车辆导入数据[{}]", SecurityContextHolder.getUserName(), vehImportData.getBatchNum());
        if (!vehImportDataAppService.checkBatchNumUnique(vehImportData.getId(), vehImportData.getBatchNum())) {
            return ApiResponse.fail("新增车辆导入数据'" + vehImportData.getBatchNum() + "'失败，批次号已存在");
        }
        VehImportDataCmd cmd = MptVehImportDataAssembler.INSTANCE.toCmd(vehImportData);
        if (vehImportDataAppService.createVehImportData(cmd, SecurityUtils.getUserId().toString()) <= 0) {
            return ApiResponse.fail("操作失败");
        }
        try {
            ImportResult result = vehImportDataAppService.parseVehImportData(vehImportData.getBatchNum());
            ImportResultResponse response = ImportResultResponse.builder()
                    .totalCount(result.getTotalCount())
                    .successCount(result.getSuccessCount())
                    .failureCount(result.getFailureCount())
                    .invalidCount(result.getInvalidCount())
                    .build();
            return ApiResponse.ok(response);
        } catch (Exception e) {
            log.error("车辆导入数据[{}]解析异常", vehImportData.getBatchNum(), e);
            return ApiResponse.fail("车辆导入数据'" + vehImportData.getBatchNum() + "'解析异常");
        }
    }

    @Log(title = "车辆导入数据管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("completeVehicle:vehicle:importData:edit")
    @PutMapping
    public ApiResponse<ImportResultResponse> edit(@Validated @RequestBody VehImportDataRequest vehImportData) {
        log.info("管理后台用户[{}]修改保存车辆导入数据[{}]", SecurityContextHolder.getUserName(), vehImportData.getBatchNum());
        if (!vehImportDataAppService.checkBatchNumUnique(vehImportData.getId(), vehImportData.getBatchNum())) {
            return ApiResponse.fail("修改保存车辆导入数据'" + vehImportData.getBatchNum() + "'失败，批次号已存在");
        }
        VehImportDataCmd cmd = MptVehImportDataAssembler.INSTANCE.toCmd(vehImportData);
        if (vehImportDataAppService.modifyVehImportData(cmd, SecurityUtils.getUserId().toString()) <= 0) {
            return ApiResponse.fail("操作失败");
        }
        try {
            ImportResult result = vehImportDataAppService.parseVehImportData(vehImportData.getBatchNum());
            ImportResultResponse response = ImportResultResponse.builder()
                    .totalCount(result.getTotalCount())
                    .successCount(result.getSuccessCount())
                    .failureCount(result.getFailureCount())
                    .invalidCount(result.getInvalidCount())
                    .build();
            return ApiResponse.ok(response);
        } catch (Exception e) {
            log.error("车辆导入数据[{}]解析异常", vehImportData.getBatchNum(), e);
            return ApiResponse.fail("车辆导入数据'" + vehImportData.getBatchNum() + "'解析异常");
        }
    }

    @Log(title = "车辆导入数据管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("completeVehicle:vehicle:importData:remove")
    @DeleteMapping("/{vehImportDataIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] vehImportDataIds) {
        log.info("管理后台用户[{}]删除车辆导入数据[{}]", SecurityContextHolder.getUserName(), vehImportDataIds);
        return vehImportDataAppService.deleteVehImportDataByIds(vehImportDataIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 补发车辆导入成功事件
     * <p>
     * VMD-DSN-CR-039: 车辆导入成功事件人工补发
     *
     * @param vehImportDataId 车辆导入数据ID
     * @param request 补发请求
     * @return 补发结果
     */
    @Log(title = "车辆导入事件补发", businessType = BusinessType.OTHER)
    @RequiresPermissions("completeVehicle:vehicle:importData:replay")
    @PostMapping("/{vehImportDataId}/replayEvent")
    public ApiResponse<ReplayEventResponse> replayEvent(@PathVariable Long vehImportDataId,
                                                         @RequestBody(required = false) ReplayVehicleImportEventRequest request) {
        log.info("管理后台用户[{}]补发车辆导入数据[id={}]事件", SecurityContextHolder.getUserName(), vehImportDataId);
        if (request == null) {
            request = new ReplayVehicleImportEventRequest();
        }
        ReplayEventResult result = vehImportEventReplayAppService.replay(
                vehImportDataId,
                request.getRequestId(),
                SecurityUtils.getUserId().toString(),
                SecurityContextHolder.getUserName(),
                request.getReason()
        );
        ReplayEventResponse response = ReplayEventResponse.builder()
                .replayId(result.getReplayId())
                .totalCount(result.getTotalCount())
                .queuedCount(result.getQueuedCount())
                .failureCount(result.getFailureCount())
                .failures(result.getFailures())
                .build();
        return ApiResponse.ok(response);
    }
}
