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
 * <p>提供从 MDM 全量快照同步数据到 VMD 本地投影的能力。</p>
 * 
 * <p>支持实体：brand（品牌）、carLine（车系）、platform（平台）、plant（工厂）、model（车型）、variant（版本）、all（全部）</p>
 * 
 * <p>同步规则：</p>
 * <ul>
 *   <li>不删除本地已有记录</li>
 *   <li>按 external_ref_id / external_version 幂等 upsert</li>
 *   <li>快照失败不清空本地已有投影数据</li>
 * </ul>
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
     * <p>从 MDM 拉取指定实体的全量快照并 upsert 本地投影副本。
     * 不删除本地已有记录，按 external_ref_id / external_version 幂等。</p>
     * 
     * @param entity 同步实体类型：brand|carLine|platform|plant|model|all
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
            case "model":
                mdmSyncAppService.bootstrapModel();
                return ApiResponse.ok("车型数据同步完成");
            case "variant":
                mdmSyncAppService.bootstrapVariant();
                return ApiResponse.ok("版本数据同步完成");
            case "all":
                mdmSyncAppService.bootstrapAll();
                return ApiResponse.ok("全量数据同步完成");
            default:
                return ApiResponse.fail("不支持的实体类型: " + entity);
        }
    }

}
