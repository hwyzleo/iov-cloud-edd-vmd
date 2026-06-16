package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParserRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 导入数据解析器集成测试
 * <p>
 * 验证解析器正确注册、获取和调用入站内核
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class ImportDataParserIntegrationTest {

    private ImportDataParserRegistry parserRegistry;

    @BeforeEach
    void setUp() {
        parserRegistry = new ImportDataParserRegistry();
    }

    @Test
    @DisplayName("获取不存在的解析器应抛出异常")
    void getParser_notExist_shouldThrowException() {
        // When & Then
        assertThrows(Exception.class, () -> parserRegistry.getParser("UNKNOWN", "1.0"));
    }
}
