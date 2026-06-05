package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BrandDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BrandAppService;
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
 * MptBrandController集成测试
 *
 * @author hwyz_leo
 */
@AutoConfigureMockMvc
class MptBrandControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BrandAppService brandAppService;

    @Test
    @DisplayName("GET /api/mpt/brand/v1/list 应返回分页品牌列表")
    void list_shouldReturnPaginatedBrandList() throws Exception {
        // Given
        BrandDto brand1 = BrandDto.builder()
                .id(1L)
                .code("BRAND001")
                .name("测试品牌1")
                .build();
        BrandDto brand2 = BrandDto.builder()
                .id(2L)
                .code("BRAND002")
                .name("测试品牌2")
                .build();
        List<BrandDto> brands = Arrays.asList(brand1, brand2);

        when(brandAppService.search(any())).thenReturn(brands);

        // When & Then
        mockMvc.perform(get("/api/mpt/brand/v1/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/brand/v1/listAll 应返回所有品牌列表")
    void listAll_shouldReturnAllBrandList() throws Exception {
        // Given
        BrandDto brand1 = BrandDto.builder()
                .id(1L)
                .code("BRAND001")
                .name("测试品牌1")
                .build();
        BrandDto brand2 = BrandDto.builder()
                .id(2L)
                .code("BRAND002")
                .name("测试品牌2")
                .build();
        List<BrandDto> brands = Arrays.asList(brand1, brand2);

        when(brandAppService.search(any())).thenReturn(brands);

        // When & Then
        mockMvc.perform(get("/api/mpt/brand/v1/listAll")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/brand/v1/{brandId} 应返回品牌详情")
    void getInfo_shouldReturnBrandDetails() throws Exception {
        // Given
        Long brandId = 1L;
        BrandDto brand = BrandDto.builder()
                .id(brandId)
                .code("BRAND001")
                .name("测试品牌")
                .build();

        when(brandAppService.getBrandById(brandId)).thenReturn(brand);

        // When & Then
        mockMvc.perform(get("/api/mpt/brand/v1/{brandId}", brandId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(brandId))
                .andExpect(jsonPath("$.data.code").value("BRAND001"))
                .andExpect(jsonPath("$.data.name").value("测试品牌"));
    }

    @Test
    @DisplayName("GET /api/mpt/brand/v1/list 应支持查询参数")
    void list_shouldSupportQueryParameters() throws Exception {
        // Given
        BrandDto brand = BrandDto.builder()
                .id(1L)
                .code("BRAND001")
                .name("测试品牌")
                .build();
        List<BrandDto> brands = Arrays.asList(brand);

        when(brandAppService.search(any())).thenReturn(brands);

        // When & Then
        mockMvc.perform(get("/api/mpt/brand/v1/list")
                        .param("code", "BRAND001")
                        .param("name", "测试")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/brand/v1/{brandId} 应返回空当品牌不存在时")
    void getInfo_shouldReturnEmptyWhenBrandNotFound() throws Exception {
        // Given
        Long brandId = 999L;
        when(brandAppService.getBrandById(brandId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/mpt/brand/v1/{brandId}", brandId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
