package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ImportResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.VehicleImportDataParser;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 总装上线 ECU 清单数据解析器 V1.0
 * <p>
 * 解析 TOL 类型的 ECU 清单数据，完成 ECU↔VIN 绑定。
 * 复用六步内核和零件绑定域。
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@Slf4j
@RequiredArgsConstructor
@Component("tolEcuListParserV1.0")
public class TolEcuListParserV1_0 extends BaseProcessor implements VehicleImportDataParser {

    private final ImportDataParserRegistry parserRegistry;

    @PostConstruct
    public void init() {
        parserRegistry.register(this);
    }

    @Override
    public String getType() {
        return "TOL";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public ImportResult parse(String batchNum, JSONObject dataJson) {
        throw new UnsupportedOperationException("TODO: TOL parser not yet implemented");
    }
}
