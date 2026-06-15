package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 整车主档后台导入解析器 (US-040)
 * 处理 type=PRODUCE 的整车主档批量导入
 *
 * @since CR-025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleProduceDataParserV1_0 implements ImportDataParser {

    private final VehicleImportDataAppService vehicleImportDataAppService;
    private final ImportDataParserRegistry parserRegistry;

    @PostConstruct
    public void init() {
        parserRegistry.register(this);
    }

    @Override
    public String getType() {
        return "PRODUCE";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public ImportResult parse(String batchNum, JSONObject dataJson) {
        log.info("US-040: 处理整车主档批量导入, batchNum={}", batchNum);

        // 复用 US-019 PRODUCE 解析器逻辑
        // VehicleProduceEvent 契约不变
        return vehicleImportDataAppService.handleProduceImport(batchNum, dataJson);
    }
}
