package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PlantCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PlantDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PlantQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehPlantRepository;
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
 * PlantAppService单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class PlantAppServiceTest {

    @Mock
    private VehPlantRepository vehPlantRepository;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @InjectMocks
    private PlantAppService plantAppService;

    @Test
    @DisplayName("search方法应返回匹配的工厂列表")
    void search_shouldReturnMatchingPlantList() {
        // Given
        PlantQuery query = PlantQuery.builder()
                .code("PLANT001")
                .name("测试")
                .build();

        Plant plant1 = Plant.builder().id(1L).code("PLANT001").name("测试工厂1").build();
        Plant plant2 = Plant.builder().id(2L).code("PLANT002").name("测试工厂2").build();
        List<Plant> plants = Arrays.asList(plant1, plant2);

        when(vehPlantRepository.selectByMap(any(Map.class))).thenReturn(plants);

        // When
        List<PlantDto> result = plantAppService.search(query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(vehPlantRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("search方法应返回空列表当无匹配时")
    void search_shouldReturnEmptyListWhenNoMatch() {
        // Given
        PlantQuery query = PlantQuery.builder()
                .code("NONEXISTENT")
                .build();

        when(vehPlantRepository.selectByMap(any(Map.class))).thenReturn(Collections.emptyList());

        // When
        List<PlantDto> result = plantAppService.search(query);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(vehPlantRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码唯一时")
    void checkCodeUnique_shouldReturnTrueWhenCodeIsUnique() {
        // Given
        String code = "PLANT001";
        when(vehPlantRepository.selectByCode(code)).thenReturn(null);

        // When
        Boolean result = plantAppService.checkCodeUnique(1L, code);

        // Then
        assertTrue(result);
        verify(vehPlantRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码属于同一工厂时")
    void checkCodeUnique_shouldReturnTrueWhenCodeBelongsToSamePlant() {
        // Given
        Long plantId = 1L;
        String code = "PLANT001";
        Plant existingPlant = Plant.builder().id(plantId).code(code).build();

        when(vehPlantRepository.selectByCode(code)).thenReturn(existingPlant);

        // When
        Boolean result = plantAppService.checkCodeUnique(plantId, code);

        // Then
        assertTrue(result);
        verify(vehPlantRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回false当代码已存在时")
    void checkCodeUnique_shouldReturnFalseWhenCodeAlreadyExists() {
        // Given
        Long plantId = 1L;
        String code = "PLANT001";
        Plant existingPlant = Plant.builder().id(2L).code(code).build();

        when(vehPlantRepository.selectByCode(code)).thenReturn(existingPlant);

        // When
        Boolean result = plantAppService.checkCodeUnique(plantId, code);

        // Then
        assertFalse(result);
        verify(vehPlantRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkPlantVehicleExist应返回true当工厂下有车辆时")
    void checkPlantVehicleExist_shouldReturnTrueWhenVehiclesExist() {
        // Given
        Long plantId = 1L;
        Plant plant = Plant.builder().id(plantId).code("PLANT001").build();

        when(vehPlantRepository.selectById(plantId)).thenReturn(plant);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(5);

        // When
        Boolean result = plantAppService.checkPlantVehicleExist(plantId);

        // Then
        assertTrue(result);
        verify(vehPlantRepository).selectById(plantId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkPlantVehicleExist应返回false当工厂下无车辆时")
    void checkPlantVehicleExist_shouldReturnFalseWhenNoVehicles() {
        // Given
        Long plantId = 1L;
        Plant plant = Plant.builder().id(plantId).code("PLANT001").build();

        when(vehPlantRepository.selectById(plantId)).thenReturn(plant);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(0);

        // When
        Boolean result = plantAppService.checkPlantVehicleExist(plantId);

        // Then
        assertFalse(result);
        verify(vehPlantRepository).selectById(plantId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("getPlantById应返回工厂DTO")
    void getPlantById_shouldReturnPlantDto() {
        // Given
        Long plantId = 1L;
        Plant plant = Plant.builder()
                .id(plantId)
                .code("PLANT001")
                .name("测试工厂")
                .build();

        when(vehPlantRepository.selectById(plantId)).thenReturn(plant);

        // When
        PlantDto result = plantAppService.getPlantById(plantId);

        // Then
        assertNotNull(result);
        assertEquals(plantId, result.getId());
        assertEquals("PLANT001", result.getCode());
        assertEquals("测试工厂", result.getName());
        verify(vehPlantRepository).selectById(plantId);
    }

    @Test
    @DisplayName("getPlantByCode应返回工厂领域对象")
    void getPlantByCode_shouldReturnPlantEntity() {
        // Given
        String code = "PLANT001";
        Plant plant = Plant.builder()
                .id(1L)
                .code(code)
                .name("测试工厂")
                .build();

        when(vehPlantRepository.selectByCode(code)).thenReturn(plant);

        // When
        Plant result = plantAppService.getPlantByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(vehPlantRepository).selectByCode(code);
    }

    @Test
    @DisplayName("createPlant应成功创建工厂")
    void createPlant_shouldSuccessfullyCreatePlant() {
        // Given
        PlantCmd cmd = PlantCmd.builder()
                .code("PLANT001")
                .name("新工厂")
                .build();

        when(vehPlantRepository.insert(any(Plant.class))).thenReturn(1);

        // When
        int result = plantAppService.createPlant(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(vehPlantRepository).insert(any(Plant.class));
    }

    @Test
    @DisplayName("modifyPlant应成功修改工厂")
    void modifyPlant_shouldSuccessfullyModifyPlant() {
        // Given
        PlantCmd cmd = PlantCmd.builder()
                .id(1L)
                .code("PLANT001")
                .name("修改后的工厂")
                .build();

        when(vehPlantRepository.update(any(Plant.class))).thenReturn(1);

        // When
        int result = plantAppService.modifyPlant(cmd, "user1");

        // Then
        assertEquals(1, result);
        verify(vehPlantRepository).update(any(Plant.class));
    }

    @Test
    @DisplayName("deletePlantByIds应成功删除工厂")
    void deletePlantByIds_shouldSuccessfullyDeletePlants() {
        // Given
        Long[] ids = {1L, 2L, 3L};
        when(vehPlantRepository.batchPhysicalDelete(ids)).thenReturn(3);

        // When
        int result = plantAppService.deletePlantByIds(ids);

        // Then
        assertEquals(3, result);
        verify(vehPlantRepository).batchPhysicalDelete(ids);
    }

    @Test
    @DisplayName("getPlantByExternalRefId应返回工厂领域对象")
    void getPlantByExternalRefId_shouldReturnPlantEntity() {
        // Given
        String externalRefId = "ext-001";
        Plant plant = Plant.builder()
                .id(1L)
                .code("PLANT001")
                .externalRefId(externalRefId)
                .build();

        when(vehPlantRepository.selectByExternalRefId(externalRefId)).thenReturn(plant);

        // When
        Plant result = plantAppService.getPlantByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        verify(vehPlantRepository).selectByExternalRefId(externalRefId);
    }

    @Test
    @DisplayName("countBySource应返回指定来源的工厂数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        String source = "MDM";
        when(vehPlantRepository.countBySource(source)).thenReturn(5);

        // When
        int result = plantAppService.countBySource(source);

        // Then
        assertEquals(5, result);
        verify(vehPlantRepository).countBySource(source);
    }
}
