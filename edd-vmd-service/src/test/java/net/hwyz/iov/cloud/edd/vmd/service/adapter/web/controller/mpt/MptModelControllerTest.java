package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ModelDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ModelAppService;
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
 * MptModelController集成测试
 *
 * @author hwyz_leo
 */
@AutoConfigureMockMvc
class MptModelControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModelAppService modelAppService;

    @Test
    @DisplayName("GET /api/mpt/model/v1/list 应返回分页车型列表")
    void list_shouldReturnPaginatedModelList() throws Exception {
        // Given
        ModelDto model1 = ModelDto.builder()
                .id(1L)
                .code("MODEL001")
                .name("测试车型1")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();
        ModelDto model2 = ModelDto.builder()
                .id(2L)
                .code("MODEL002")
                .name("测试车型2")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();
        List<ModelDto> models = Arrays.asList(model1, model2);

        when(modelAppService.search(any())).thenReturn(models);

        // When & Then
        mockMvc.perform(get("/api/mpt/model/v1/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/model/v1/listByPlatformCodeAndSeriesCode 应返回指定平台和车系下的车型列表")
    void listByPlatformCodeAndSeriesCode_shouldReturnModelListForPlatformAndCarLine() throws Exception {
        // Given
        ModelDto model1 = ModelDto.builder()
                .id(1L)
                .code("MODEL001")
                .name("测试车型1")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();
        ModelDto model2 = ModelDto.builder()
                .id(2L)
                .code("MODEL002")
                .name("测试车型2")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();
        List<ModelDto> models = Arrays.asList(model1, model2);

        when(modelAppService.search(any())).thenReturn(models);

        // When & Then
        mockMvc.perform(get("/api/mpt/model/v1/listByPlatformCodeAndSeriesCode")
                        .param("platformCode", "PLATFORM001")
                        .param("carLineCode", "CARLINE001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/model/v1/{modelId} 应返回车型详情")
    void getInfo_shouldReturnModelDetails() throws Exception {
        // Given
        Long modelId = 1L;
        ModelDto model = ModelDto.builder()
                .id(modelId)
                .code("MODEL001")
                .name("测试车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .brandCode("BRAND001")
                .build();

        when(modelAppService.getModelById(modelId)).thenReturn(model);

        // When & Then
        mockMvc.perform(get("/api/mpt/model/v1/{modelId}", modelId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(modelId))
                .andExpect(jsonPath("$.data.code").value("MODEL001"))
                .andExpect(jsonPath("$.data.name").value("测试车型"))
                .andExpect(jsonPath("$.data.platformCode").value("PLATFORM001"))
                .andExpect(jsonPath("$.data.carLineCode").value("CARLINE001"))
                .andExpect(jsonPath("$.data.brandCode").value("BRAND001"));
    }

    @Test
    @DisplayName("GET /api/mpt/model/v1/list 应支持查询参数")
    void list_shouldSupportQueryParameters() throws Exception {
        // Given
        ModelDto model = ModelDto.builder()
                .id(1L)
                .code("MODEL001")
                .name("测试车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();
        List<ModelDto> models = Arrays.asList(model);

        when(modelAppService.search(any())).thenReturn(models);

        // When & Then
        mockMvc.perform(get("/api/mpt/model/v1/list")
                        .param("platformCode", "PLATFORM001")
                        .param("carLineCode", "CARLINE001")
                        .param("code", "MODEL001")
                        .param("name", "测试")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/model/v1/{modelId} 应返回空当车型不存在时")
    void getInfo_shouldReturnEmptyWhenModelNotFound() throws Exception {
        // Given
        Long modelId = 999L;
        when(modelAppService.getModelById(modelId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/mpt/model/v1/{modelId}", modelId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/mpt/model/v1/list 应支持按平台代码查询")
    void list_shouldSupportPlatformCodeFilter() throws Exception {
        // Given
        ModelDto model = ModelDto.builder()
                .id(1L)
                .code("MODEL001")
                .name("测试车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();
        List<ModelDto> models = Arrays.asList(model);

        when(modelAppService.search(any())).thenReturn(models);

        // When & Then
        mockMvc.perform(get("/api/mpt/model/v1/list")
                        .param("platformCode", "PLATFORM001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/model/v1/list 应支持按车系代码查询")
    void list_shouldSupportCarLineCodeFilter() throws Exception {
        // Given
        ModelDto model = ModelDto.builder()
                .id(1L)
                .code("MODEL001")
                .name("测试车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .build();
        List<ModelDto> models = Arrays.asList(model);

        when(modelAppService.search(any())).thenReturn(models);

        // When & Then
        mockMvc.perform(get("/api/mpt/model/v1/list")
                        .param("carLineCode", "CARLINE001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
