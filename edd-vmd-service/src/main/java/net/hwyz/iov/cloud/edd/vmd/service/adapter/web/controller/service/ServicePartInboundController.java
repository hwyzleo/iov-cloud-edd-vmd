package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundRecord;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService.PartInboundResult;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 零件实例入站接口（入口①上游系统对接）
 * <p>
 * 独立入站链路，异步事件为主、批量接口兜底
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/partInbound/v1")
public class ServicePartInboundController {

    private final PartInboundAppService partInboundAppService;

    /**
     * 批量入站处理
     *
     * @param request 批量入站请求
     * @return 处理结果
     */
    @PostMapping("/batch")
    public ApiResponse<PartInboundResult> batchInbound(@Validated @RequestBody BatchInboundRequest request) {
        log.info("上游系统批量入站，来源[{}]批次号[{}]记录数[{}]",
                request.getSource(), request.getBatchNo(),
                request.getRecords() != null ? request.getRecords().size() : 0);

        InboundSourceType source = InboundSourceType.valOf(request.getSource());
        if (source == null) {
            return ApiResponse.fail("无效的入站来源: " + request.getSource());
        }

        List<PartInboundRecord> records = request.getRecords().stream()
                .map(item -> convertToRecord(item, request.getBatchNo()))
                .toList();

        PartInboundResult result = partInboundAppService.processInbound(records, source, request.getVin());
        return ApiResponse.ok(result);
    }

    private PartInboundRecord convertToRecord(BatchInboundItem item, String batchNo) {
        return PartInboundRecord.builder()
                .partCode(item.getPartCode())
                .sn(item.getSn())
                .partType(item.getPartType())
                .vehicleNodeCode(item.getVehicleNodeCode())
                .deviceItem(item.getDeviceItem())
                .supplierCode(item.getSupplierCode())
                .batchNum(batchNo)
                .sourceEventId(item.getSourceEventId())
                .extraFields(item.getExtraFields())
                .build();
    }

    /**
     * 批量入站请求
     */
    @lombok.Data
    public static class BatchInboundRequest {
        /**
         * 入站来源：MES/WMS/IQC/OTHER
         */
        private String source;
        /**
         * 批次号
         */
        private String batchNo;
        /**
         * 车架号（可选，有VIN时建立绑定）
         */
        private String vin;
        /**
         * 入站记录列表
         */
        private List<BatchInboundItem> records;
    }

    /**
     * 批量入站记录项
     */
    @lombok.Data
    public static class BatchInboundItem {
        /**
         * 零件编码
         */
        private String partCode;
        /**
         * 零件序列号
         */
        private String sn;
        /**
         * 零件类型：TBOX/BTM/CCP/IDCM/SIM/OTHER
         */
        private String partType;
        /**
         * 车载节点代码（可选）
         */
        private String vehicleNodeCode;
        /**
         * 设备项/安装位置（可选）
         */
        private String deviceItem;
        /**
         * 供应商编码（可选）
         */
        private String supplierCode;
        /**
         * 源事件ID（可选，用于事件级幂等去重）
         */
        private String sourceEventId;
        /**
         * 特殊字段（按part_type的type-schema定义）
         */
        private Map<String, String> extraFields;
    }
}
