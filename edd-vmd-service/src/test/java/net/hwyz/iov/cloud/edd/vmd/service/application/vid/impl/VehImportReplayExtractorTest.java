package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 车辆导入补发候选提取器单元测试
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展为按 ImportType 路由的动作补偿
 * <p>
 * 覆盖：TOL/EOL 报文 VIN + 候选零件提取、同 VIN 多 ITEM 合并、去重、EOL 时间解析、空报文。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@DisplayName("VehImportReplayExtractor 测试")
class VehImportReplayExtractorTest {

    private final VehImportReplayExtractor extractor = new VehImportReplayExtractor();

    @Test
    @DisplayName("TOL报文应提取VIN与候选零件（ASSEMBLY_PART_NO + SN）")
    void extractTolCandidates() {
        String data = "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"vin-a\",\"PARTS\":["
                + "{\"ASSEMBLY_PART_NO\":\"PN001\",\"HARDWARE_PART_NO\":\"HW001\",\"SN\":\"SN001\"},"
                + "{\"ASSEMBLY_PART_NO\":\"PN002\",\"SN\":\"SN002\"}]},"
                + "{\"VIN\":\"VIN-A\",\"PARTS\":[{\"ASSEMBLY_PART_NO\":\"PN001\",\"SN\":\"SN003\"}]}]}}}";

        List<VehImportReplayExtractor.VehCandidate> candidates = extractor.extractCandidates(data, "TOL");

        assertEquals(1, candidates.size());
        VehImportReplayExtractor.VehCandidate candidate = candidates.get(0);
        assertEquals("VIN-A", candidate.vin());
        assertNull(candidate.eolTime());
        // 同 VIN 两条 ITEM 零件合并
        assertEquals(3, candidate.parts().size());
        assertEquals("PN001:SN001", candidate.parts().get(0).partCode() + ":" + candidate.parts().get(0).sn());
        assertEquals("PN002:SN002", candidate.parts().get(1).partCode() + ":" + candidate.parts().get(1).sn());
        assertEquals("PN001:SN003", candidate.parts().get(2).partCode() + ":" + candidate.parts().get(2).sn());
    }

    @Test
    @DisplayName("EOL报文应提取VIN、PART_NO/PART_SN与EOL_TIME毫秒时间戳")
    void extractEolCandidates() {
        String data = "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"vin-b\",\"EOL_TIME\":1784391702000,\"PARTS\":["
                + "{\"PART_NO\":\"17300011AA\",\"PART_SN\":\"SNB001\",\"HARDWARE_PN\":\"17300013AA\"}]}]}}}";

        List<VehImportReplayExtractor.VehCandidate> candidates = extractor.extractCandidates(data, "EOL");

        assertEquals(1, candidates.size());
        VehImportReplayExtractor.VehCandidate candidate = candidates.get(0);
        assertEquals("VIN-B", candidate.vin());
        assertEquals(Instant.ofEpochMilli(1784391702000L), candidate.eolTime());
        assertEquals(1, candidate.parts().size());
        assertEquals("17300011AA", candidate.parts().get(0).partCode());
        assertEquals("SNB001", candidate.parts().get(0).sn());
    }

    @Test
    @DisplayName("EOL报文EOL_TIME缺失时兼容EOL_DATE yyyyMMdd")
    void extractEolDateFallback() {
        String data = "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"vin-c\",\"EOL_DATE\":\"20260617\",\"PARTS\":[]}]}}}";
        List<VehImportReplayExtractor.VehCandidate> candidates = extractor.extractCandidates(data, "EOL");
        assertEquals(1, candidates.size());
        assertNotNull(candidates.get(0).eolTime());
    }

    @Test
    @DisplayName("PRODUCE报文仅提取VIN")
    void extractProduceVins() {
        String data = "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"vin-d\"},{\"VIN\":\"vin-e\"}]}}}";
        List<VehImportReplayExtractor.VehCandidate> candidates = extractor.extractCandidates(data, "PRODUCE");
        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().allMatch(c -> c.parts().isEmpty()));
    }

    @Test
    @DisplayName("空报文/缺少DATA/缺少ITEMS应返回空列表")
    void extractEmptyOrMalformed() {
        assertTrue(extractor.extractCandidates(null, "TOL").isEmpty());
        assertTrue(extractor.extractCandidates("{}", "TOL").isEmpty());
        assertTrue(extractor.extractCandidates("{\"REQUEST\":{\"DATA\":{}}}", "TOL").isEmpty());
        assertTrue(extractor.extractCandidates("{\"REQUEST\":{\"DATA\":{\"ITEMS\":[]}}}", "TOL").isEmpty());
    }

    @Test
    @DisplayName("零件编码或SN为空的记录应跳过")
    void extractSkipBlankPart() {
        String data = "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"vin-f\",\"PARTS\":["
                + "{\"ASSEMBLY_PART_NO\":\"\",\"SN\":\"SN001\"},"
                + "{\"ASSEMBLY_PART_NO\":\"PN003\",\"SN\":\"\"},"
                + "{\"ASSEMBLY_PART_NO\":\"PN004\",\"SN\":\"SN004\"}]}]}}}";
        List<VehImportReplayExtractor.VehCandidate> candidates = extractor.extractCandidates(data, "TOL");
        assertEquals(1, candidates.size());
        assertEquals(1, candidates.get(0).parts().size());
        assertEquals("PN004", candidates.get(0).parts().get(0).partCode());
    }
}
