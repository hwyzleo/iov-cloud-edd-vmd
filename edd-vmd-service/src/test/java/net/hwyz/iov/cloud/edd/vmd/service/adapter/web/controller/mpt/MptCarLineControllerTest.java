package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CarLineDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.CarLineAppService;
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
 * MptCarLineController集成测试
 * 
 * <p>CR-014：CarLine 投影采用按需最小化只读投影，保留 brand_code 冗余字段。</p>
 *
 * @author hwyz_leo
 */
@AutoConfigureMockMvc
class MptCarLineControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarLineAppService carLineAppService;

    @Test
    @DisplayName("GET /api/mpt/carLine/v1/list 应返回分页车系列表")
    void list_shouldReturnPaginatedCarLineList() throws Exception {
        // Given
        CarLineDto carLine1 = CarLineDto.builder()
                .id(1L)
                .code("CARLINE001")
                .name("测试车系1")
                .brandCode("BRAND001")
                .build();
        CarLineDto carLine2 = CarLineDto.builder()
                .id(2L)
                .code("CARLINE002")
                .name("测试车系2")
                .brandCode("BRAND001")
                .build();
        List<CarLineDto> carLines = Arrays.asList(carLine1, carLine2);

        when(carLineAppService.search(any())).thenReturn(carLines);

        // When & Then
        mockMvc.perform(get("/api/mpt/carLine/v1/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/carLine/v1/listByBrandCode 应返回指定品牌下的车系列表")
    void listByBrandCode_shouldReturnCarLinesByBrandCode() throws Exception {
        // Given
        String brandCode = "BRAND001";
        CarLineDto carLine1 = CarLineDto.builder()
                .id(1L)
                .code("CARLINE001")
                .name("测试车系1")
                .brandCode(brandCode)
                .build();
        List<CarLineDto> carLines = Arrays.asList(carLine1);

        when(carLineAppService.search(any())).thenReturn(carLines);

        // When & Then
        mockMvc.perform(get("/api/mpt/carLine/v1/listByBrandCode")
                        .param("brandCode", brandCode)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/carLine/v1/{carLineId} 应返回车系详情")
    void getInfo_shouldReturnCarLineDetails() throws Exception {
        // Given
        Long carLineId = 1L;
        CarLineDto carLine = CarLineDto.builder()
                .id(carLineId)
                .code("CARLINE001")
                .name("测试车系")
                .brandCode("BRAND001")
                .build();

        when(carLineAppService.getSeriesById(carLineId)).thenReturn(carLine);

        // When & Then
        mockMvc.perform(get("/api/mpt/carLine/v1/{carLineId}", carLineId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(carLineId))
                .andExpect(jsonPath("$.data.code").value("CARLINE001"))
                .andExpect(jsonPath("$.data.name").value("测试车系"))
                .andExpect(jsonPath("$.data.brandCode").value("BRAND001"));
    }

    @Test
    @DisplayName("GET /api/mpt/carLine/v1/list 应支持查询参数")
    void list_shouldSupportQueryParameters() throws Exception {
        // Given
        CarLineDto carLine = CarLineDto.builder()
                .id(1L)
                .code("CARLINE001")
                .name("测试车系")
                .brandCode("BRAND001")
                .build();
        List<CarLineDto> carLines = Arrays.asList(carLine);

        when(carLineAppService.search(any())).thenReturn(carLines);

        // When & Then
        mockMvc.perform(get("/api/mpt/carLine/v1/list")
                        .param("code", "CARLINE001")
                        .param("name", "测试")
                        .param("brandCode", "BRAND001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/mpt/carLine/v1/{carLineId} 应返回空当车系不存在时")
    void getInfo_shouldReturnEmptyWhenCarLineNotFound() throws Exception {
        // Given
        Long carLineId = 999L;
        when(carLineAppService.getSeriesById(carLineId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/mpt/carLine/v1/{carLineId}", carLineId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/mpt/carLine/v1/list 应支持按brandCode过滤")
    void list_shouldSupportBrandCodeFilter() throws Exception {
        // Given
        String brandCode = "BRAND001";
        CarLineDto carLine1 = CarLineDto.builder()
                .id(1L)
                .code("CARLINE001")
                .name("测试车系1")
                .brandCode(brandCode)
                .build();
        CarLineDto carLine2 = CarLineDto.builder()
                .id(2L)
                .code("CARLINE002")
                .name("测试车系2")
                .brandCode(brandCode)
                .build();
        List<CarLineDto> carLines = Arrays.asList(carLine1, carLine2);

        when(carLineAppService.search(any())).thenReturn(carLines);

        // When & Then
        mockMvc.perform(get("/api/mpt/carLine/v1/list")
                        .param("brandCode", brandCode)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
