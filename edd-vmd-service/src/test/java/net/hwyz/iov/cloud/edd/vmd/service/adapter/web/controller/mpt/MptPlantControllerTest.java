package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ManufacturerDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PlantAppService;
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
 * MptPlantController集成测试
 *
 * @author hwyz_leo
 */
@AutoConfigureMockMvc
class MptPlantControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlantAppService plantAppService;

    @Test
    @DisplayName("GET /api/mpt/plant/v1/list 应返回分页工厂列表")
    void list_shouldReturnPaginatedPlantList() throws Exception {
        // Given
        ManufacturerDto plant1 = ManufacturerDto.builder()
                .id(1L)
                .code("PLANT001")
                .name("测试工厂1")
                .build();
        ManufacturerDto plant2 = ManufacturerDto.builder()
                .id(2L)
                .code("PLANT002")
                .name("测试工厂2")
                .build();
        List<ManufacturerDto> plants = Arrays.asList(plant1, plant2);

        when(plantAppService.search(any())).thenReturn(plants);

        // When & Then
        mockMvc.perform(get("/api/mpt/plant/v1/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/plant/v1/{plantId} 应返回工厂详情")
    void getInfo_shouldReturnPlantDetails() throws Exception {
        // Given
        Long plantId = 1L;
        ManufacturerDto plant = ManufacturerDto.builder()
                .id(plantId)
                .code("PLANT001")
                .name("测试工厂")
                .build();

        when(plantAppService.getPlantById(plantId)).thenReturn(plant);

        // When & Then
        mockMvc.perform(get("/api/mpt/plant/v1/{plantId}", plantId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(plantId))
                .andExpect(jsonPath("$.data.code").value("PLANT001"))
                .andExpect(jsonPath("$.data.name").value("测试工厂"));
    }

    @Test
    @DisplayName("GET /api/mpt/plant/v1/list 应支持查询参数")
    void list_shouldSupportQueryParameters() throws Exception {
        // Given
        ManufacturerDto plant = ManufacturerDto.builder()
                .id(1L)
                .code("PLANT001")
                .name("测试工厂")
                .build();
        List<ManufacturerDto> plants = Arrays.asList(plant);

        when(plantAppService.search(any())).thenReturn(plants);

        // When & Then
        mockMvc.perform(get("/api/mpt/plant/v1/list")
                        .param("code", "PLANT001")
                        .param("name", "测试")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/plant/v1/{plantId} 应返回404当工厂不存在时")
    void getInfo_shouldReturn404WhenPlantNotFound() throws Exception {
        // Given
        Long plantId = 999L;
        when(plantAppService.getPlantById(plantId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/mpt/plant/v1/{plantId}", plantId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}