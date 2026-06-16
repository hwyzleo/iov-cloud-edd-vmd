package net.hwyz.iov.cloud.edd.vmd.service.application.vid;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DownstreamProcessorRegistry单元测试
 *
 * @author hwyz_leo
 */
class DownstreamProcessorRegistryTest {

    @Test
    @DisplayName("注册表应能注册和获取处理器")
    void testRegisterAndGetProcessor() {
        // 创建模拟处理器
        DownstreamProcessor mockProcessor = mock(DownstreamProcessor.class);
        when(mockProcessor.getSupportedVehicleNodeCode()).thenReturn("TSP");

        List<DownstreamProcessor> processors = Arrays.asList(mockProcessor);
        DownstreamProcessorRegistry registry = new DownstreamProcessorRegistry(processors);

        // 验证注册
        DownstreamProcessor retrieved = registry.getProcessor("TSP");
        assertNotNull(retrieved);
        assertEquals("TSP", retrieved.getSupportedVehicleNodeCode());
    }

    @Test
    @DisplayName("注册表应能处理重复注册")
    void testDuplicateRegistration() {
        // 创建两个相同类型的处理器
        DownstreamProcessor mockProcessor1 = mock(DownstreamProcessor.class);
        when(mockProcessor1.getSupportedVehicleNodeCode()).thenReturn("TSP");

        DownstreamProcessor mockProcessor2 = mock(DownstreamProcessor.class);
        when(mockProcessor2.getSupportedVehicleNodeCode()).thenReturn("TSP");

        List<DownstreamProcessor> processors = Arrays.asList(mockProcessor1, mockProcessor2);
        DownstreamProcessorRegistry registry = new DownstreamProcessorRegistry(processors);

        // 验证第一个处理器被保留
        DownstreamProcessor retrieved = registry.getProcessor("TSP");
        assertNotNull(retrieved);
        assertSame(mockProcessor1, retrieved);
    }

    @Test
    @DisplayName("注册表应返回null对于不存在的处理器")
    void testGetNonExistentProcessor() {
        DownstreamProcessorRegistry registry = new DownstreamProcessorRegistry(Arrays.asList());

        DownstreamProcessor retrieved = registry.getProcessor("NON_EXISTENT");
        assertNull(retrieved);
    }
}
