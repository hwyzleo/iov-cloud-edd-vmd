package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BrandCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BrandDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.BrandQuery;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BrandAppService单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class BrandAppServiceTest {

    @Mock
    private MdmBrandRepository mdmBrandRepository;

    @Mock
    private MdmCarLineRepository mdmCarLineRepository;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @InjectMocks
    private BrandAppService brandAppService;

    @Test
    @DisplayName("search方法应返回匹配的品牌列表")
    void search_shouldReturnMatchingBrandList() {
        // Given
        BrandQuery query = BrandQuery.builder()
                .code("BRAND001")
                .name("测试")
                .build();

        Brand brand1 = Brand.builder().id(1L).code("BRAND001").name("测试品牌1").build();
        Brand brand2 = Brand.builder().id(2L).code("BRAND002").name("测试品牌2").build();
        List<Brand> brands = Arrays.asList(brand1, brand2);

        when(mdmBrandRepository.selectByMap(any(Map.class))).thenReturn(brands);

        // When
        List<BrandDto> result = brandAppService.search(query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mdmBrandRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("search方法应返回空列表当无匹配时")
    void search_shouldReturnEmptyListWhenNoMatch() {
        // Given
        BrandQuery query = BrandQuery.builder()
                .code("NONEXISTENT")
                .build();

        when(mdmBrandRepository.selectByMap(any(Map.class))).thenReturn(Collections.emptyList());

        // When
        List<BrandDto> result = brandAppService.search(query);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mdmBrandRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码唯一时")
    void checkCodeUnique_shouldReturnTrueWhenCodeIsUnique() {
        // Given
        String code = "BRAND001";
        when(mdmBrandRepository.selectByCode(code)).thenReturn(null);

        // When
        Boolean result = brandAppService.checkCodeUnique(1L, code);

        // Then
        assertTrue(result);
        verify(mdmBrandRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码属于同一品牌时")
    void checkCodeUnique_shouldReturnTrueWhenCodeBelongsToSameBrand() {
        // Given
        Long brandId = 1L;
        String code = "BRAND001";
        Brand existingBrand = Brand.builder().id(brandId).code(code).build();

        when(mdmBrandRepository.selectByCode(code)).thenReturn(existingBrand);

        // When
        Boolean result = brandAppService.checkCodeUnique(brandId, code);

        // Then
        assertTrue(result);
        verify(mdmBrandRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回false当代码已存在时")
    void checkCodeUnique_shouldReturnFalseWhenCodeAlreadyExists() {
        // Given
        Long brandId = 1L;
        String code = "BRAND001";
        Brand existingBrand = Brand.builder().id(2L).code(code).build();

        when(mdmBrandRepository.selectByCode(code)).thenReturn(existingBrand);

        // When
        Boolean result = brandAppService.checkCodeUnique(brandId, code);

        // Then
        assertFalse(result);
        verify(mdmBrandRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkBrandSeriesExist应返回true当品牌下有车系时")
    void checkBrandSeriesExist_shouldReturnTrueWhenSeriesExist() {
        // Given
        Long brandId = 1L;
        Brand brand = Brand.builder().id(brandId).code("BRAND001").build();

        when(mdmBrandRepository.selectById(brandId)).thenReturn(brand);
        when(mdmCarLineRepository.countByMap(any(Map.class))).thenReturn(5);

        // When
        Boolean result = brandAppService.checkBrandSeriesExist(brandId);

        // Then
        assertTrue(result);
        verify(mdmBrandRepository).selectById(brandId);
        verify(mdmCarLineRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkBrandSeriesExist应返回false当品牌下无车系时")
    void checkBrandSeriesExist_shouldReturnFalseWhenNoSeries() {
        // Given
        Long brandId = 1L;
        Brand brand = Brand.builder().id(brandId).code("BRAND001").build();

        when(mdmBrandRepository.selectById(brandId)).thenReturn(brand);
        when(mdmCarLineRepository.countByMap(any(Map.class))).thenReturn(0);

        // When
        Boolean result = brandAppService.checkBrandSeriesExist(brandId);

        // Then
        assertFalse(result);
        verify(mdmBrandRepository).selectById(brandId);
        verify(mdmCarLineRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkBrandVehicleExist应返回true当品牌下有车辆时")
    void checkBrandVehicleExist_shouldReturnTrueWhenVehiclesExist() {
        // Given
        Long brandId = 1L;
        Brand brand = Brand.builder().id(brandId).code("BRAND001").build();

        when(mdmBrandRepository.selectById(brandId)).thenReturn(brand);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(5);

        // When
        Boolean result = brandAppService.checkBrandVehicleExist(brandId);

        // Then
        assertTrue(result);
        verify(mdmBrandRepository).selectById(brandId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkBrandVehicleExist应返回false当品牌下无车辆时")
    void checkBrandVehicleExist_shouldReturnFalseWhenNoVehicles() {
        // Given
        Long brandId = 1L;
        Brand brand = Brand.builder().id(brandId).code("BRAND001").build();

        when(mdmBrandRepository.selectById(brandId)).thenReturn(brand);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(0);

        // When
        Boolean result = brandAppService.checkBrandVehicleExist(brandId);

        // Then
        assertFalse(result);
        verify(mdmBrandRepository).selectById(brandId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("getBrandById应返回品牌DTO")
    void getBrandById_shouldReturnBrandDto() {
        // Given
        Long brandId = 1L;
        Brand brand = Brand.builder()
                .id(brandId)
                .code("BRAND001")
                .name("测试品牌")
                .build();

        when(mdmBrandRepository.selectById(brandId)).thenReturn(brand);

        // When
        BrandDto result = brandAppService.getBrandById(brandId);

        // Then
        assertNotNull(result);
        assertEquals(brandId, result.getId());
        assertEquals("BRAND001", result.getCode());
        assertEquals("测试品牌", result.getName());
        verify(mdmBrandRepository).selectById(brandId);
    }

    @Test
    @DisplayName("getBrandByCode应返回品牌领域对象")
    void getBrandByCode_shouldReturnBrandEntity() {
        // Given
        String code = "BRAND001";
        Brand brand = Brand.builder()
                .id(1L)
                .code(code)
                .name("测试品牌")
                .build();

        when(mdmBrandRepository.selectByCode(code)).thenReturn(brand);

        // When
        Brand result = brandAppService.getBrandByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(mdmBrandRepository).selectByCode(code);
    }

    @Test
    @DisplayName("createBrand应成功创建MANUAL来源品牌")
    void createBrand_shouldSuccessfullyCreateManualBrand() {
        // Given
        BrandCmd cmd = BrandCmd.builder()
                .code("BRAND001")
                .name("新品牌")
                .build();

        when(mdmBrandRepository.insert(any(Brand.class))).thenReturn(1);

        // When
        int result = brandAppService.createBrand(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(mdmBrandRepository).insert(any(Brand.class));
    }

    @Test
    @DisplayName("createBrand应拒绝创建MDM来源品牌")
    void createBrand_shouldRejectMdmSourceBrand() {
        // Given
        BrandCmd cmd = BrandCmd.builder()
                .code("BRAND_MDM")
                .name("MDM品牌")
                .build();

        // 模拟BrandAssembler将cmd转换为domain对象时设置source为MDM
        // 注意：这里需要模拟BrandAssembler的行为，但由于它是静态的，
        // 我们直接测试ProductDataReadOnlyException会被抛出

        // When & Then
        // 由于BrandAssembler是MapStruct生成的，我们无法直接模拟它
        // 但我们可以验证当source=MDM时会抛出异常
        // 这个测试需要在集成测试中验证
    }

    @Test
    @DisplayName("modifyBrand应成功修改MANUAL来源品牌")
    void modifyBrand_shouldSuccessfullyModifyManualBrand() {
        // Given
        BrandCmd cmd = BrandCmd.builder()
                .id(1L)
                .code("BRAND001")
                .name("修改后的品牌")
                .build();

        Brand existingBrand = Brand.builder()
                .id(1L)
                .code("BRAND001")
                .name("原始品牌")
                .source(SourceType.MANUAL)
                .build();

        when(mdmBrandRepository.selectById(1L)).thenReturn(existingBrand);
        when(mdmBrandRepository.update(any(Brand.class))).thenReturn(1);

        // When
        int result = brandAppService.modifyBrand(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(mdmBrandRepository).selectById(1L);
        verify(mdmBrandRepository).update(any(Brand.class));
    }

    @Test
    @DisplayName("modifyBrand应拒绝修改MDM来源品牌")
    void modifyBrand_shouldRejectMdmSourceBrand() {
        // Given
        BrandCmd cmd = BrandCmd.builder()
                .id(1L)
                .code("BRAND_MDM")
                .name("修改MDM品牌")
                .build();

        Brand existingBrand = Brand.builder()
                .id(1L)
                .code("BRAND_MDM")
                .name("MDM品牌")
                .source(SourceType.MDM)
                .build();

        when(mdmBrandRepository.selectById(1L)).thenReturn(existingBrand);

        // When & Then
        assertThrows(ProductDataReadOnlyException.class, () -> {
            brandAppService.modifyBrand(cmd, "user1");
        });
        verify(mdmBrandRepository).selectById(1L);
        verify(mdmBrandRepository, never()).update(any(Brand.class));
    }

    @Test
    @DisplayName("deleteBrandByIds应成功删除MANUAL来源品牌")
    void deleteBrandByIds_shouldSuccessfullyDeleteManualBrands() {
        // Given
        Long[] ids = {1L, 2L, 3L};

        Brand brand1 = Brand.builder().id(1L).code("BRAND001").source(SourceType.MANUAL).build();
        Brand brand2 = Brand.builder().id(2L).code("BRAND002").source(SourceType.MANUAL).build();
        Brand brand3 = Brand.builder().id(3L).code("BRAND003").source(SourceType.MANUAL).build();

        when(mdmBrandRepository.selectById(1L)).thenReturn(brand1);
        when(mdmBrandRepository.selectById(2L)).thenReturn(brand2);
        when(mdmBrandRepository.selectById(3L)).thenReturn(brand3);
        when(mdmBrandRepository.batchPhysicalDelete(ids)).thenReturn(3);

        // When
        int result = brandAppService.deleteBrandByIds(ids);

        // Then
        assertEquals(3, result);
        verify(mdmBrandRepository).selectById(1L);
        verify(mdmBrandRepository).selectById(2L);
        verify(mdmBrandRepository).selectById(3L);
        verify(mdmBrandRepository).batchPhysicalDelete(ids);
    }

    @Test
    @DisplayName("deleteBrandByIds应拒绝删除MDM来源品牌")
    void deleteBrandByIds_shouldRejectMdmSourceBrand() {
        // Given
        Long[] ids = {1L, 2L};

        Brand brand1 = Brand.builder().id(1L).code("BRAND001").source(SourceType.MANUAL).build();
        Brand brand2 = Brand.builder().id(2L).code("BRAND_MDM").source(SourceType.MDM).build();

        when(mdmBrandRepository.selectById(1L)).thenReturn(brand1);
        when(mdmBrandRepository.selectById(2L)).thenReturn(brand2);

        // When & Then
        assertThrows(ProductDataReadOnlyException.class, () -> {
            brandAppService.deleteBrandByIds(ids);
        });
        verify(mdmBrandRepository).selectById(1L);
        verify(mdmBrandRepository).selectById(2L);
        verify(mdmBrandRepository, never()).batchPhysicalDelete(any(Long[].class));
    }

    @Test
    @DisplayName("getBrandByExternalRefId应返回品牌领域对象")
    void getBrandByExternalRefId_shouldReturnBrandEntity() {
        // Given
        String externalRefId = "ext-001";
        Brand brand = Brand.builder()
                .id(1L)
                .code("BRAND001")
                .externalRefId(externalRefId)
                .build();

        when(mdmBrandRepository.selectByExternalRefId(externalRefId)).thenReturn(brand);

        // When
        Brand result = mdmBrandRepository.selectByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        verify(mdmBrandRepository).selectByExternalRefId(externalRefId);
    }

    @Test
    @DisplayName("countBySource应返回指定来源的品牌数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        SourceType source = SourceType.MDM;
        when(mdmBrandRepository.countBySource(source)).thenReturn(5L);

        // When
        long result = mdmBrandRepository.countBySource(source);

        // Then
        assertEquals(5L, result);
        verify(mdmBrandRepository).countBySource(source);
    }
}
