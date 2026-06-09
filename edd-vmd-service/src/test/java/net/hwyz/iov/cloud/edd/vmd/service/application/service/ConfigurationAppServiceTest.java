package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.ConfigurationQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehConfigurationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationAppServiceTest {

    @Mock
    private VehConfigurationRepository vehConfigurationRepository;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @Mock
    private FeatureFamilyAppService featureFamilyAppService;

    @InjectMocks
    private ConfigurationAppService configurationAppService;

    @Test
    @DisplayName("search方法应返回匹配的配置列表")
    void testSearch() {
        ConfigurationQuery query = ConfigurationQuery.builder()
                .platformCode("P001")
                .carLineCode("CL001")
                .modelCode("M001")
                .variantCode("V001")
                .baseModelCode("BM001")
                .code("C001")
                .name("测试")
                .build();

        Configuration c1 = Configuration.builder().id(1L).code("C001").name("配置1").build();
        Configuration c2 = Configuration.builder().id(2L).code("C002").name("配置2").build();

        when(vehConfigurationRepository.selectByMap(any(Map.class))).thenReturn(Arrays.asList(c1, c2));

        List<ConfigurationDto> result = configurationAppService.search(query);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(vehConfigurationRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("search方法应返回空列表当无匹配时")
    void testSearch_noResult() {
        ConfigurationQuery query = ConfigurationQuery.builder()
                .platformCode("P001")
                .build();

        when(vehConfigurationRepository.selectByMap(any(Map.class))).thenReturn(Collections.emptyList());

        List<ConfigurationDto> result = configurationAppService.search(query);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(vehConfigurationRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("getConfigurationListByVariantCode应返回启用的配置列表")
    void testGetConfigurationListByVariantCode() {
        String variantCode = "V001";
        Configuration c1 = Configuration.builder().id(1L).code("C001").variantCode(variantCode).enable(true).build();
        Configuration c2 = Configuration.builder().id(2L).code("C002").variantCode(variantCode).enable(true).build();

        when(vehConfigurationRepository.selectByExample(any(Configuration.class))).thenReturn(Arrays.asList(c1, c2));

        List<ConfigurationDto> result = configurationAppService.getConfigurationListByVariantCode(variantCode);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(vehConfigurationRepository).selectByExample(any(Configuration.class));
    }

    @Test
    @DisplayName("getConfigurationListByVariantCode应返回空列表当无配置时")
    void testGetConfigurationListByVariantCode_noResult() {
        String variantCode = "V001";

        when(vehConfigurationRepository.selectByExample(any(Configuration.class))).thenReturn(Collections.emptyList());

        List<ConfigurationDto> result = configurationAppService.getConfigurationListByVariantCode(variantCode);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(vehConfigurationRepository).selectByExample(any(Configuration.class));
    }

    @Test
    @DisplayName("getConfigurationByCode应返回配置DTO")
    void testGetConfigurationByCode() {
        String code = "C001";
        Configuration configuration = Configuration.builder().id(1L).code(code).name("配置1").build();

        when(vehConfigurationRepository.selectByCode(code)).thenReturn(configuration);

        ConfigurationDto result = configurationAppService.getConfigurationByCode(code);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(vehConfigurationRepository).selectByCode(code);
    }

    @Test
    @DisplayName("getConfigurationByCode应返回null当配置不存在时")
    void testGetConfigurationByCode_notFound() {
        String code = "C001";

        when(vehConfigurationRepository.selectByCode(code)).thenReturn(null);

        ConfigurationDto result = configurationAppService.getConfigurationByCode(code);

        assertNull(result);
        verify(vehConfigurationRepository).selectByCode(code);
    }

    @Test
    @DisplayName("getConfigurationEntityByCode应返回配置实体")
    void testGetConfigurationEntityByCode() {
        String code = "C001";
        Configuration configuration = Configuration.builder().id(1L).code(code).name("配置1").build();

        when(vehConfigurationRepository.selectByCode(code)).thenReturn(configuration);

        Configuration result = configurationAppService.getConfigurationEntityByCode(code);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(vehConfigurationRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码唯一时")
    void testCheckCodeUnique() {
        String code = "C001";
        when(vehConfigurationRepository.selectByCode(code)).thenReturn(null);

        Boolean result = configurationAppService.checkCodeUnique(1L, code);

        assertTrue(result);
        verify(vehConfigurationRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当代码属于同一配置时")
    void testCheckCodeUnique_sameConfiguration() {
        String code = "C001";
        Configuration existing = Configuration.builder().id(1L).code(code).build();

        when(vehConfigurationRepository.selectByCode(code)).thenReturn(existing);

        Boolean result = configurationAppService.checkCodeUnique(1L, code);

        assertTrue(result);
        verify(vehConfigurationRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回false当代码已存在时")
    void testCheckCodeUnique_duplicate() {
        String code = "C001";
        Configuration existing = Configuration.builder().id(2L).code(code).build();

        when(vehConfigurationRepository.selectByCode(code)).thenReturn(existing);

        Boolean result = configurationAppService.checkCodeUnique(1L, code);

        assertFalse(result);
        verify(vehConfigurationRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkCodeUnique应返回true当configurationId为null时且代码不存在")
    void testCheckCodeUnique_nullId() {
        String code = "C001";
        when(vehConfigurationRepository.selectByCode(code)).thenReturn(null);

        Boolean result = configurationAppService.checkCodeUnique(null, code);

        assertTrue(result);
        verify(vehConfigurationRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkConfigurationVehicleExist应返回true当配置下存在车辆时")
    void testCheckConfigurationVehicleExist() {
        Long configurationId = 1L;
        Configuration configuration = Configuration.builder().id(configurationId).code("C001").build();

        when(vehConfigurationRepository.selectById(configurationId)).thenReturn(configuration);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(5);

        Boolean result = configurationAppService.checkConfigurationVehicleExist(configurationId);

        assertTrue(result);
        verify(vehConfigurationRepository).selectById(configurationId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkConfigurationVehicleExist应返回false当配置下无车辆时")
    void testCheckConfigurationVehicleExist_noVehicle() {
        Long configurationId = 1L;
        Configuration configuration = Configuration.builder().id(configurationId).code("C001").build();

        when(vehConfigurationRepository.selectById(configurationId)).thenReturn(configuration);
        when(vehBasicInfoRepository.countByMap(any(Map.class))).thenReturn(0);

        Boolean result = configurationAppService.checkConfigurationVehicleExist(configurationId);

        assertFalse(result);
        verify(vehConfigurationRepository).selectById(configurationId);
        verify(vehBasicInfoRepository).countByMap(any(Map.class));
    }

    @Test
    @DisplayName("getConfigurationById应返回配置DTO")
    void testGetConfigurationById() {
        Long id = 1L;
        Configuration configuration = Configuration.builder().id(id).code("C001").name("配置1").build();

        when(vehConfigurationRepository.selectById(id)).thenReturn(configuration);

        ConfigurationDto result = configurationAppService.getConfigurationById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(vehConfigurationRepository).selectById(id);
    }

    @Test
    @DisplayName("createConfiguration应成功创建配置")
    void testCreateConfiguration() {
        ConfigurationCmd cmd = ConfigurationCmd.builder()
                .code("C001")
                .name("新配置")
                .platformCode("P001")
                .carLineCode("CL001")
                .modelCode("M001")
                .baseModelCode("V001")
                .enable(true)
                .build();

        when(vehConfigurationRepository.insert(any(Configuration.class))).thenReturn(1);

        int result = configurationAppService.createConfiguration(cmd, "user1");

        assertEquals(1, result);
        verify(vehConfigurationRepository).insert(any(Configuration.class));
    }

    @Test
    @DisplayName("modifyConfiguration应成功修改配置")
    void testModifyConfiguration() {
        ConfigurationCmd cmd = ConfigurationCmd.builder()
                .id(1L)
                .code("C001")
                .name("修改后的配置")
                .platformCode("P001")
                .carLineCode("CL001")
                .modelCode("M001")
                .baseModelCode("V001")
                .enable(true)
                .build();

        when(vehConfigurationRepository.update(any(Configuration.class))).thenReturn(1);

        int result = configurationAppService.modifyConfiguration(cmd, "user1");

        assertEquals(1, result);
        verify(vehConfigurationRepository).update(any(Configuration.class));
    }

    @Test
    @DisplayName("deleteConfigurationByIds应成功删除配置")
    void testDeleteConfigurationByIds() {
        Long[] ids = {1L, 2L, 3L};

        when(vehConfigurationRepository.batchPhysicalDelete(ids)).thenReturn(3);

        int result = configurationAppService.deleteConfigurationByIds(ids);

        assertEquals(3, result);
        verify(vehConfigurationRepository).batchPhysicalDelete(ids);
    }

    @Test
    @DisplayName("deleteConfigurationByIds应返回0当无配置被删除时")
    void testDeleteConfigurationByIds_noDeleted() {
        Long[] ids = {999L};

        when(vehConfigurationRepository.batchPhysicalDelete(ids)).thenReturn(0);

        int result = configurationAppService.deleteConfigurationByIds(ids);

        assertEquals(0, result);
        verify(vehConfigurationRepository).batchPhysicalDelete(ids);
    }
}
