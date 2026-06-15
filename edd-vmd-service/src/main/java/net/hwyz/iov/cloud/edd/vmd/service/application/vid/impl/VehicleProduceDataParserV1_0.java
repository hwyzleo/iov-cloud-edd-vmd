package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartImportDataAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final PartImportDataAppService partImportDataAppService;
    private final ImportDataParserRegistry parserRegistry;
    @Autowired
    private ProduceDataParserV1_0 produceDataParserV1_0;

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

        // 直接调用原始的 ProduceDataParserV1_0，避免通过 PartImportDataAppService 导致无限递归
        return produceDataParserV1_0.parse(batchNum, dataJson);
    }
}
