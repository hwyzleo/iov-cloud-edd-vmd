package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.PartImportDataRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ReplayPartImportPostProcessRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ImportResultResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PartImportDataResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PartImportPostProcessReplayResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ReplayPostProcessResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptPartImportDataAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler.MptPartImportPostProcessReplayAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartImportDataCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PartImportDataQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportPostProcessReplayDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayPostProcessResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartImportPostProcessReplayAppService;
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
 * 零件导入数据相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/partImportData/v1")
public class MptPartImportDataController extends BaseController {

    private final PartImportDataAppService partImportDataAppService;
    private final PartImportPostProcessReplayAppService partImportPostProcessReplayAppService;

    @RequiresPermissions("vmd:vehicle:importData:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<PartImportDataResponse>> list(PartImportDataRequest partImportData) {
        log.info("管理后台用户[{}]分页查询零件导入数据", SecurityContextHolder.getUserName());
        startPage();
        PartImportDataQuery query = PartImportDataQuery.builder()
                .batchNum(partImportData.getBatchNum())
                .partCode(partImportData.getPartCode())
                .handle(partImportData.getHandle())
                .beginTime(getBeginTime(partImportData))
                .endTime(getEndTime(partImportData))
                .build();
        List<PartImportDataDto> dtoList = partImportDataAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(dtoList, MptPartImportDataAssembler.INSTANCE::fromDto)));
    }

    @Log(title = "零件导入数据管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("vmd:vehicle:importData:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, PartImportDataRequest partImportData) {
        log.info("管理后台用户[{}]导出零件导入数据", SecurityContextHolder.getUserName());
    }

    @RequiresPermissions("vmd:vehicle:importData:query")
    @GetMapping(value = "/{partImportDataId}")
    public ApiResponse<PartImportDataResponse> getInfo(@PathVariable Long partImportDataId) {
        log.info("管理后台用户[{}]根据零件导入数据ID[{}]获取零件导入数据", SecurityContextHolder.getUserName(), partImportDataId);
        return ApiResponse.ok(MptPartImportDataAssembler.INSTANCE.fromDto(partImportDataAppService.getPartImportDataById(partImportDataId)));
    }

    @Log(title = "零件导入数据管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("vmd:vehicle:importData:add")
    @PostMapping
    public ApiResponse<ImportResultResponse> add(@Validated @RequestBody PartImportDataRequest partImportData) {
        log.info("管理后台用户[{}]新增零件导入数据[{}]", SecurityContextHolder.getUserName(), partImportData.getBatchNum());
        if (!partImportDataAppService.checkBatchNumUnique(partImportData.getId(), partImportData.getBatchNum())) {
            return ApiResponse.fail("新增零件导入数据'" + partImportData.getBatchNum() + "'失败，批次号已存在");
        }
        PartImportDataCmd cmd = MptPartImportDataAssembler.INSTANCE.toCmd(partImportData);
        if (partImportDataAppService.createPartImportData(cmd, SecurityUtils.getUserId().toString()) <= 0) {
            return ApiResponse.fail("操作失败");
        }
        try {
            ImportResult result = partImportDataAppService.parsePartImportData(partImportData.getBatchNum());
            ImportResultResponse response = ImportResultResponse.builder()
                    .totalCount(result.getTotalCount())
                    .successCount(result.getSuccessCount())
                    .failureCount(result.getFailureCount())
                    .invalidCount(result.getInvalidCount())
                    .description(result.getDescription())
                    .build();
            return ApiResponse.ok(response);
        } catch (Exception e) {
            log.error("零件导入数据[{}]解析异常", partImportData.getBatchNum(), e);
            return ApiResponse.fail("零件导入数据'" + partImportData.getBatchNum() + "'解析异常");
        }
    }

    @Log(title = "零件导入数据管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("vmd:vehicle:importData:edit")
    @PutMapping
    public ApiResponse<ImportResultResponse> edit(@Validated @RequestBody PartImportDataRequest partImportData) {
        log.info("管理后台用户[{}]修改保存零件导入数据[{}]", SecurityContextHolder.getUserName(), partImportData.getBatchNum());
        if (!partImportDataAppService.checkBatchNumUnique(partImportData.getId(), partImportData.getBatchNum())) {
            return ApiResponse.fail("修改保存零件导入数据'" + partImportData.getBatchNum() + "'失败，批次号已存在");
        }
        PartImportDataCmd cmd = MptPartImportDataAssembler.INSTANCE.toCmd(partImportData);
        if (partImportDataAppService.modifyPartImportData(cmd, SecurityUtils.getUserId().toString()) <= 0) {
            return ApiResponse.fail("操作失败");
        }
        try {
            ImportResult result = partImportDataAppService.parsePartImportData(partImportData.getBatchNum());
            ImportResultResponse response = ImportResultResponse.builder()
                    .totalCount(result.getTotalCount())
                    .successCount(result.getSuccessCount())
                    .failureCount(result.getFailureCount())
                    .invalidCount(result.getInvalidCount())
                    .description(result.getDescription())
                    .build();
            return ApiResponse.ok(response);
        } catch (Exception e) {
            log.error("零件导入数据[{}]解析异常", partImportData.getBatchNum(), e);
            return ApiResponse.fail("零件导入数据'" + partImportData.getBatchNum() + "'解析异常");
        }
    }

    @Log(title = "零件导入数据管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("vmd:vehicle:importData:remove")
    @DeleteMapping("/{partImportDataIds}")
    public ApiResponse<Void> remove(@PathVariable Long[] partImportDataIds) {
        log.info("管理后台用户[{}]删除零件导入数据[{}]", SecurityContextHolder.getUserName(), partImportDataIds);
        return partImportDataAppService.deletePartImportDataByIds(partImportDataIds) > 0 ? ApiResponse.ok() : ApiResponse.fail("操作失败");
    }

    /**
     * 零件导入后置处理人工重放
     * <p>
     * VMD-DSN-CR-056: 从历史导入批次识别候选实例，读取当前权威状态，
     * 逐项执行适用的事件发布、下游联动、绑定事实发布与安全常量补偿；
     * 不重入零件入站解析器，不改写零件主体或绑定关系。
     *
     * @param partImportDataId 零件导入数据ID
     * @param request          重放请求
     * @return 重放结果
     */
    @Log(title = "零件导入后置处理重放", businessType = BusinessType.OTHER)
    @RequiresPermissions("vmd:partImportData:replayPostProcess")
    @PostMapping("/{partImportDataId}/replayPostProcess")
    public ApiResponse<ReplayPostProcessResponse> replayPostProcess(@PathVariable Long partImportDataId,
                                                                     @RequestBody(required = false) ReplayPartImportPostProcessRequest request) {
        log.info("管理后台用户[{}]重放零件导入数据[id={}]后置处理", SecurityContextHolder.getUserName(), partImportDataId);
        if (request == null) {
            request = new ReplayPartImportPostProcessRequest();
        }
        ReplayPostProcessResult result = partImportPostProcessReplayAppService.replay(
                partImportDataId,
                MptPartImportPostProcessReplayAssembler.INSTANCE.toCmd(request),
                SecurityUtils.getUserId().toString(),
                SecurityContextHolder.getUserName()
        );
        ReplayPostProcessResponse response = ReplayPostProcessResponse.builder()
                .replayId(result.getReplayId())
                .itemCount(result.getItemCount())
                .actionCount(result.getActionCount())
                .queuedCount(result.getQueuedCount())
                .successCount(result.getSuccessCount())
                .skippedCount(result.getSkippedCount())
                .failureCount(result.getFailureCount())
                .failures(result.getFailures())
                .build();
        return ApiResponse.ok(response);
    }

    /**
     * 查询零件导入后置处理重放主任务及逐项动作明细
     * <p>
     * VMD-DSN-CR-056: 供前端查看重放进度 / 对失败项发起 retryFailedOnly=true 重试
     *
     * @param replayId 重放请求ID
     * @return 主任务 + 动作明细
     */
    @RequiresPermissions("vmd:partImportData:replayPostProcess")
    @GetMapping("/replays/{replayId}")
    public ApiResponse<PartImportPostProcessReplayResponse> getReplay(@PathVariable String replayId) {
        log.info("管理后台用户[{}]查询零件导入后置处理重放[{}]", SecurityContextHolder.getUserName(), replayId);
        PartImportPostProcessReplayDto dto = partImportPostProcessReplayAppService.getReplayByReplayId(replayId);
        return ApiResponse.ok(MptPartImportPostProcessReplayAssembler.INSTANCE.fromDto(dto));
    }
}
