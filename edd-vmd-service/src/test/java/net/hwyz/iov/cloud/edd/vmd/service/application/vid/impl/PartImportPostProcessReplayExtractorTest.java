package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 零件导入后置处理候选实例提取器单元测试
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@DisplayName("PartImportPostProcessReplayExtractor 测试")
class PartImportPostProcessReplayExtractorTest {

    private final PartImportPostProcessReplayExtractor extractor = new PartImportPostProcessReplayExtractor();

    private String buildData(String itemsJson) {
        return "{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"ITEMS\":" + itemsJson + "}}}";
    }

    @Test
    @DisplayName("正常报文提取并去重候选 (partCode, sn)")
    void extractDistinctCandidates_normal() {
        String data = buildData("[{\"SN\":\"SN001\",\"ASSEMBLY_PART_NO\":\"PN001\",\"ICCID1\":\"1\"},"
                + "{\"SN\":\"SN002\",\"ASSEMBLY_PART_NO\":\"PN001\",\"ICCID1\":\"2\"},"
                + "{\"SN\":\"SN001\",\"ASSEMBLY_PART_NO\":\"PN001\",\"ICCID1\":\"3\"}]");
        List<PartImportPostProcessReplayExtractor.Candidate> candidates = extractor.extractDistinctCandidates(data);
        assertEquals(2, candidates.size());
        assertEquals("PN001", candidates.get(0).partCode());
        assertEquals("SN001", candidates.get(0).sn());
        assertEquals("PN001", candidates.get(1).partCode());
        assertEquals("SN002", candidates.get(1).sn());
    }

    @Test
    @DisplayName("ASSEMBLY_PART_NO 缺失时回退 HARDWARE_PART_NO")
    void extractDistinctCandidates_fallbackHardwarePartNo() {
        String data = buildData("[{\"SN\":\"SN009\",\"HARDWARE_PART_NO\":\"HW001\"}]");
        List<PartImportPostProcessReplayExtractor.Candidate> candidates = extractor.extractDistinctCandidates(data);
        assertEquals(1, candidates.size());
        assertEquals("HW001", candidates.get(0).partCode());
        assertEquals("SN009", candidates.get(0).sn());
    }

    @Test
    @DisplayName("空 SN 或空零件编码的记录跳过")
    void extractDistinctCandidates_skipBlank() {
        String data = buildData("[{\"SN\":\"\",\"ASSEMBLY_PART_NO\":\"PN001\"},"
                + "{\"SN\":\"SN002\",\"ASSEMBLY_PART_NO\":\"\"},"
                + "{\"SN\":\"SN003\",\"ASSEMBLY_PART_NO\":\"PN003\"}]");
        List<PartImportPostProcessReplayExtractor.Candidate> candidates = extractor.extractDistinctCandidates(data);
        assertEquals(1, candidates.size());
        assertEquals("PN003", candidates.get(0).partCode());
    }

    @Test
    @DisplayName("原始数据缺失 / 无 DATA / 无 ITEMS 返回空")
    void extractDistinctCandidates_empty() {
        assertTrue(extractor.extractDistinctCandidates(null).isEmpty());
        assertTrue(extractor.extractDistinctCandidates("").isEmpty());
        assertTrue(extractor.extractDistinctCandidates("{\"REQUEST\":{}}").isEmpty());
        assertTrue(extractor.extractDistinctCandidates("{\"REQUEST\":{\"DATA\":{}}}").isEmpty());
        assertTrue(extractor.extractDistinctCandidates("not-json").isEmpty());
    }

    @Test
    @DisplayName("候选 trim 后去重（大小写 / 空格归一）")
    void extractDistinctCandidates_trim() {
        String data = buildData("[{\"SN\":\" SN001 \",\"ASSEMBLY_PART_NO\":\"PN001\"},"
                + "{\"SN\":\"SN001\",\"ASSEMBLY_PART_NO\":\" PN001 \"}]");
        List<PartImportPostProcessReplayExtractor.Candidate> candidates = extractor.extractDistinctCandidates(data);
        assertEquals(1, candidates.size());
        assertEquals("PN001", candidates.get(0).partCode());
        assertEquals("SN001", candidates.get(0).sn());
    }
}
