package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import org.springframework.web.bind.annotation.*;

/**
 * MDM 同步管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/mdmSync/v1")
public class MptMdmSyncController extends BaseController {

    private final MdmSyncAppService mdmSyncAppService;

    /**
     * Bootstrap 全量同步
     *
     * @param entity 实体类型：brand|series|platform|all
     * @return 操作结果
     */
    @RequiresPermissions("completeVehicle:mdmSync:bootstrap")
    @Log(title = "MDM同步", businessType = BusinessType.OTHER)
    @PostMapping(value = "/bootstrap")
    public ApiResponse<String> bootstrap(@RequestParam(value = "entity", defaultValue = "all") String entity) {
        log.info("管理后台用户触发MDM Bootstrap同步: entity={}", entity);
        switch (entity.toLowerCase()) {
            case "brand":
                mdmSyncAppService.bootstrapBrand();
                return ApiResponse.ok("品牌数据同步完成");
            case "series":
                mdmSyncAppService.bootstrapSeries();
                return ApiResponse.ok("车系数据同步完成");
            case "platform":
                mdmSyncAppService.bootstrapPlatform();
                return ApiResponse.ok("平台数据同步完成");
            case "all":
                mdmSyncAppService.bootstrapAll();
                return ApiResponse.ok("全量数据同步完成");
            default:
                return ApiResponse.fail("不支持的实体类型: " + entity);
        }
    }

}
