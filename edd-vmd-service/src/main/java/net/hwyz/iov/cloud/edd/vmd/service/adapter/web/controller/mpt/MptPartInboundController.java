package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PartInfoQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartInfoDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 零件入站对账管理接口
 * <p>
 * 专门用于入站对账，支持按入站来源、零件类型、入站批次号等维度查询
 * 区别于MptPartInfoController的通用零件实例查询
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/partInbound/v1")
public class MptPartInboundController extends BaseController {

    private final PartInfoAppService partInfoAppService;

    /**
     * 分页查询零件入站记录
     * <p>
     * 支持入站特有查询维度：source（入站来源）、partType（零件类型）、inboundBatchNo（入站批次号）
     *
     * @param query 查询条件（含入站特有字段）
     * @return 零件实例列表
     */
    @RequiresPermissions("completeVehicle:vehicle:partInbound:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<PartInfoDto>> list(PartInfoQuery query) {
        log.info("管理后台用户[{}]分页查询零件入站记录，来源[{}]类型[{}]批次号[{}]",
                SecurityContextHolder.getUserName(),
                query.getSource(), query.getPartType(), query.getInboundBatchNo());
        startPage();
        List<PartInfoDto> dtoList = partInfoAppService.search(query);
        return ApiResponse.ok(getPageResult(dtoList));
    }

    /**
     * 导出零件入站记录
     *
     * @param response 响应
     * @param query    查询条件
     */
    @Log(title = "零件入站对账", businessType = BusinessType.EXPORT)
    @RequiresPermissions("completeVehicle:vehicle:partInbound:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, PartInfoQuery query) {
        log.info("管理后台用户[{}]导出零件入站记录", SecurityContextHolder.getUserName());
        // TODO: 实现导出逻辑
    }
}
