package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PlatformDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PlatformAppService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MptPlatformController集成测试
 *
 * @author hwyz_leo
 */
@AutoConfigureMockMvc
class MptPlatformControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatformAppService platformAppService;

    @Test
    @DisplayName("GET /api/mpt/platform/v1/list 应返回分页车辆平台列表")
    void list_shouldReturnPaginatedPlatformList() throws Exception {
        PlatformDto platform1 = PlatformDto.builder()
                .id(1L)
                .code("PLATFORM001")
                .name("测试平台1")
                .build();
        PlatformDto platform2 = PlatformDto.builder()
                .id(2L)
                .code("PLATFORM002")
                .name("测试平台2")
                .build();
        List<PlatformDto> platforms = Arrays.asList(platform1, platform2);

        when(platformAppService.search(any())).thenReturn(platforms);

        mockMvc.perform(get("/api/mpt/platform/v1/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/platform/v1/listAll 应返回所有车辆平台列表")
    void listAll_shouldReturnAllPlatformList() throws Exception {
        PlatformDto platform1 = PlatformDto.builder()
                .id(1L)
                .code("PLATFORM001")
                .name("测试平台1")
                .build();
        PlatformDto platform2 = PlatformDto.builder()
                .id(2L)
                .code("PLATFORM002")
                .name("测试平台2")
                .build();
        List<PlatformDto> platforms = Arrays.asList(platform1, platform2);

        when(platformAppService.search(any())).thenReturn(platforms);

        mockMvc.perform(get("/api/mpt/platform/v1/listAll")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/platform/v1/{platformId} 应返回车辆平台详情")
    void getInfo_shouldReturnPlatformDetails() throws Exception {
        Long platformId = 1L;
        PlatformDto platform = PlatformDto.builder()
                .id(platformId)
                .code("PLATFORM001")
                .name("测试平台")
                .build();

        when(platformAppService.getPlatformById(platformId)).thenReturn(platform);

        mockMvc.perform(get("/api/mpt/platform/v1/{platformId}", platformId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(platformId))
                .andExpect(jsonPath("$.data.code").value("PLATFORM001"))
                .andExpect(jsonPath("$.data.name").value("测试平台"));
    }

    @Test
    @DisplayName("GET /api/mpt/platform/v1/list 应支持查询参数")
    void list_shouldSupportQueryParameters() throws Exception {
        PlatformDto platform = PlatformDto.builder()
                .id(1L)
                .code("PLATFORM001")
                .name("测试平台")
                .build();
        List<PlatformDto> platforms = Arrays.asList(platform);

        when(platformAppService.search(any())).thenReturn(platforms);

        mockMvc.perform(get("/api/mpt/platform/v1/list")
                        .param("code", "PLATFORM001")
                        .param("name", "测试")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/platform/v1/{platformId} 应返回空当车辆平台不存在时")
    void getInfo_shouldReturnEmptyWhenPlatformNotFound() throws Exception {
        Long platformId = 999L;
        when(platformAppService.getPlatformById(platformId)).thenReturn(null);

        mockMvc.perform(get("/api/mpt/platform/v1/{platformId}", platformId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
