package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OptionCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OptionFamilyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.OptionCodeQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.OptionFamilyQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmOptionFamilyRepository;
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
class OptionFamilyAppServiceTest {

    @Mock
    private MdmOptionFamilyRepository mdmOptionFamilyRepository;

    @InjectMocks
    private OptionFamilyAppService optionFamilyAppService;

    // ==================== 选装族 ====================

    @Test
    @DisplayName("search方法应返回匹配的选装族列表")
    void testSearch() {
        OptionFamilyQuery query = OptionFamilyQuery.builder()
                .code("OF001")
                .name("测试")
                .type("EXTERIOR")
                .build();

        OptionFamily of1 = OptionFamily.builder().id(1L).code("OF001").name("选装族1").build();
        OptionFamily of2 = OptionFamily.builder().id(2L).code("OF002").name("选装族2").build();

        when(mdmOptionFamilyRepository.selectByMap(any(Map.class))).thenReturn(Arrays.asList(of1, of2));

        List<OptionFamilyDto> result = optionFamilyAppService.search(query);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mdmOptionFamilyRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("search方法应返回空列表当无匹配时")
    void testSearch_noResult() {
        OptionFamilyQuery query = OptionFamilyQuery.builder()
                .code("OF001")
                .build();

        when(mdmOptionFamilyRepository.selectByMap(any(Map.class))).thenReturn(Collections.emptyList());

        List<OptionFamilyDto> result = optionFamilyAppService.search(query);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mdmOptionFamilyRepository).selectByMap(any(Map.class));
    }

    @Test
    @DisplayName("checkOptionFamilyCodeUnique应返回true当代码唯一时")
    void testCheckOptionFamilyCodeUnique_noConflict() {
        String code = "OF001";
        when(mdmOptionFamilyRepository.selectByCode(code)).thenReturn(null);

        Boolean result = optionFamilyAppService.checkOptionFamilyCodeUnique(1L, code);

        assertTrue(result);
        verify(mdmOptionFamilyRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkOptionFamilyCodeUnique应返回true当更新自身时")
    void testCheckOptionFamilyCodeUnique_sameId() {
        String code = "OF001";
        OptionFamily existing = OptionFamily.builder().id(1L).code(code).build();

        when(mdmOptionFamilyRepository.selectByCode(code)).thenReturn(existing);

        Boolean result = optionFamilyAppService.checkOptionFamilyCodeUnique(1L, code);

        assertTrue(result);
        verify(mdmOptionFamilyRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkOptionFamilyCodeUnique应返回false当代码冲突时")
    void testCheckOptionFamilyCodeUnique_conflict() {
        String code = "OF001";
        OptionFamily existing = OptionFamily.builder().id(2L).code(code).build();

        when(mdmOptionFamilyRepository.selectByCode(code)).thenReturn(existing);

        Boolean result = optionFamilyAppService.checkOptionFamilyCodeUnique(1L, code);

        assertFalse(result);
        verify(mdmOptionFamilyRepository).selectByCode(code);
    }

    @Test
    @DisplayName("checkOptionFamilyCodeUnique应返回true当optionFamilyId为null且代码不存在")
    void testCheckOptionFamilyCodeUnique_nullId() {
        String code = "OF001";
        when(mdmOptionFamilyRepository.selectByCode(code)).thenReturn(null);

        Boolean result = optionFamilyAppService.checkOptionFamilyCodeUnique(null, code);

        assertTrue(result);
        verify(mdmOptionFamilyRepository).selectByCode(code);
    }

    @Test
    @DisplayName("getOptionFamilyById应返回选装族DTO")
    void testGetOptionFamilyById() {
        Long id = 1L;
        OptionFamily optionFamily = OptionFamily.builder().id(id).code("OF001").name("选装族1").build();

        when(mdmOptionFamilyRepository.selectById(id)).thenReturn(optionFamily);

        OptionFamilyDto result = optionFamilyAppService.getOptionFamilyById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(mdmOptionFamilyRepository).selectById(id);
    }

    @Test
    @DisplayName("getOptionFamilyByCode应返回选装族DTO")
    void testGetOptionFamilyByCode() {
        String code = "OF001";
        OptionFamily optionFamily = OptionFamily.builder().id(1L).code(code).name("选装族1").build();

        when(mdmOptionFamilyRepository.selectByCode(code)).thenReturn(optionFamily);

        OptionFamilyDto result = optionFamilyAppService.getOptionFamilyByCode(code);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(mdmOptionFamilyRepository).selectByCode(code);
    }

    @Test
    @DisplayName("createOptionFamily应成功创建选装族")
    void testCreateOptionFamily() {
        OptionFamilyCmd cmd = OptionFamilyCmd.builder()
                .code("OF001")
                .name("新选装族")
                .type("EXTERIOR")
                .build();

        when(mdmOptionFamilyRepository.insert(any(OptionFamily.class))).thenReturn(1);

        int result = optionFamilyAppService.createOptionFamily(cmd, "user1");

        assertEquals(1, result);
        verify(mdmOptionFamilyRepository).insert(any(OptionFamily.class));
    }

    @Test
    @DisplayName("modifyOptionFamily应成功修改非MDM来源的选装族")
    void testModifyOptionFamily() {
        OptionFamilyCmd cmd = OptionFamilyCmd.builder()
                .id(1L)
                .code("OF001")
                .name("修改后的选装族")
                .type("EXTERIOR")
                .build();

        OptionFamily existing = OptionFamily.builder().id(1L).code("OF001").source("VMD").build();

        when(mdmOptionFamilyRepository.selectById(1L)).thenReturn(existing);
        when(mdmOptionFamilyRepository.update(any(OptionFamily.class))).thenReturn(1);

        int result = optionFamilyAppService.modifyOptionFamily(cmd, "user1");

        assertEquals(1, result);
        verify(mdmOptionFamilyRepository).selectById(1L);
        verify(mdmOptionFamilyRepository).update(any(OptionFamily.class));
    }

    @Test
    @DisplayName("modifyOptionFamily应抛出ProductDataReadOnlyException当来源为MDM时")
    void testModifyOptionFamily_mdmSourceThrowsException() {
        OptionFamilyCmd cmd = OptionFamilyCmd.builder()
                .id(1L)
                .code("OF001")
                .name("修改后的选装族")
                .build();

        OptionFamily existing = OptionFamily.builder().id(1L).code("OF001").source("MDM").build();

        when(mdmOptionFamilyRepository.selectById(1L)).thenReturn(existing);

        assertThrows(ProductDataReadOnlyException.class, () ->
                optionFamilyAppService.modifyOptionFamily(cmd, "user1"));
        verify(mdmOptionFamilyRepository).selectById(1L);
        verify(mdmOptionFamilyRepository, never()).update(any(OptionFamily.class));
    }

    @Test
    @DisplayName("deleteOptionFamilyByIds应成功删除非MDM来源的选装族")
    void testDeleteOptionFamilyByIds() {
        Long[] ids = {1L, 2L};

        OptionFamily of1 = OptionFamily.builder().id(1L).code("OF001").source("VMD").build();
        OptionFamily of2 = OptionFamily.builder().id(2L).code("OF002").source("VMD").build();

        when(mdmOptionFamilyRepository.selectById(1L)).thenReturn(of1);
        when(mdmOptionFamilyRepository.selectById(2L)).thenReturn(of2);
        when(mdmOptionFamilyRepository.batchPhysicalDelete(ids)).thenReturn(2);

        int result = optionFamilyAppService.deleteOptionFamilyByIds(ids);

        assertEquals(2, result);
        verify(mdmOptionFamilyRepository).batchPhysicalDelete(ids);
    }

    @Test
    @DisplayName("deleteOptionFamilyByIds应抛出ProductDataReadOnlyException当存在MDM来源时")
    void testDeleteOptionFamilyByIds_mdmSourceThrowsException() {
        Long[] ids = {1L, 2L};

        OptionFamily of1 = OptionFamily.builder().id(1L).code("OF001").source("VMD").build();
        OptionFamily of2 = OptionFamily.builder().id(2L).code("OF002").source("MDM").build();

        when(mdmOptionFamilyRepository.selectById(1L)).thenReturn(of1);
        when(mdmOptionFamilyRepository.selectById(2L)).thenReturn(of2);

        assertThrows(ProductDataReadOnlyException.class, () ->
                optionFamilyAppService.deleteOptionFamilyByIds(ids));
        verify(mdmOptionFamilyRepository, never()).batchPhysicalDelete(any(Long[].class));
    }

    // ==================== 选装值 ====================

    @Test
    @DisplayName("checkOptionCodeUnique应返回true当代码唯一时")
    void testCheckOptionCodeUnique_noConflict() {
        String code = "OC001";
        when(mdmOptionFamilyRepository.selectOptionCodeByCode(code)).thenReturn(null);

        Boolean result = optionFamilyAppService.checkOptionCodeUnique(1L, code);

        assertTrue(result);
        verify(mdmOptionFamilyRepository).selectOptionCodeByCode(code);
    }

    @Test
    @DisplayName("checkOptionCodeUnique应返回true当更新自身时")
    void testCheckOptionCodeUnique_sameId() {
        String code = "OC001";
        OptionCode existing = OptionCode.builder().id(1L).code(code).build();

        when(mdmOptionFamilyRepository.selectOptionCodeByCode(code)).thenReturn(existing);

        Boolean result = optionFamilyAppService.checkOptionCodeUnique(1L, code);

        assertTrue(result);
        verify(mdmOptionFamilyRepository).selectOptionCodeByCode(code);
    }

    @Test
    @DisplayName("checkOptionCodeUnique应返回false当代码冲突时")
    void testCheckOptionCodeUnique_conflict() {
        String code = "OC001";
        OptionCode existing = OptionCode.builder().id(2L).code(code).build();

        when(mdmOptionFamilyRepository.selectOptionCodeByCode(code)).thenReturn(existing);

        Boolean result = optionFamilyAppService.checkOptionCodeUnique(1L, code);

        assertFalse(result);
        verify(mdmOptionFamilyRepository).selectOptionCodeByCode(code);
    }

    @Test
    @DisplayName("checkOptionCodeUnique应返回true当optionCodeId为null且代码不存在")
    void testCheckOptionCodeUnique_nullId() {
        String code = "OC001";
        when(mdmOptionFamilyRepository.selectOptionCodeByCode(code)).thenReturn(null);

        Boolean result = optionFamilyAppService.checkOptionCodeUnique(null, code);

        assertTrue(result);
        verify(mdmOptionFamilyRepository).selectOptionCodeByCode(code);
    }

    @Test
    @DisplayName("modifyOptionCode应抛出ProductDataReadOnlyException当来源为MDM时")
    void testModifyOptionCode_mdmSourceThrowsException() {
        OptionCodeCmd cmd = OptionCodeCmd.builder()
                .id(1L)
                .code("OC001")
                .name("修改后的选装值")
                .build();

        OptionCode existing = OptionCode.builder().id(1L).code("OC001").source("MDM").build();

        when(mdmOptionFamilyRepository.selectOptionCodeById(1L)).thenReturn(existing);

        assertThrows(ProductDataReadOnlyException.class, () ->
                optionFamilyAppService.modifyOptionCode(1L, cmd, "user1"));
        verify(mdmOptionFamilyRepository).selectOptionCodeById(1L);
        verify(mdmOptionFamilyRepository, never()).updateOptionCode(any(OptionCode.class));
    }

    @Test
    @DisplayName("modifyOptionCode应成功修改非MDM来源的选装值")
    void testModifyOptionCode() {
        OptionCodeCmd cmd = OptionCodeCmd.builder()
                .id(1L)
                .code("OC001")
                .name("修改后的选装值")
                .build();

        OptionCode existing = OptionCode.builder().id(1L).code("OC001").source("VMD").build();

        when(mdmOptionFamilyRepository.selectOptionCodeById(1L)).thenReturn(existing);
        when(mdmOptionFamilyRepository.updateOptionCode(any(OptionCode.class))).thenReturn(1);

        int result = optionFamilyAppService.modifyOptionCode(1L, cmd, "user1");

        assertEquals(1, result);
        verify(mdmOptionFamilyRepository).selectOptionCodeById(1L);
        verify(mdmOptionFamilyRepository).updateOptionCode(any(OptionCode.class));
    }

    @Test
    @DisplayName("deleteOptionCodeByIds应抛出ProductDataReadOnlyException当存在MDM来源时")
    void testDeleteOptionCodeByIds_mdmSourceThrowsException() {
        Long[] ids = {1L, 2L};

        OptionCode oc1 = OptionCode.builder().id(1L).code("OC001").source("VMD").build();
        OptionCode oc2 = OptionCode.builder().id(2L).code("OC002").source("MDM").build();

        when(mdmOptionFamilyRepository.selectOptionCodeById(1L)).thenReturn(oc1);
        when(mdmOptionFamilyRepository.selectOptionCodeById(2L)).thenReturn(oc2);

        assertThrows(ProductDataReadOnlyException.class, () ->
                optionFamilyAppService.deleteOptionCodeByIds(1L, ids));
        verify(mdmOptionFamilyRepository, never()).batchPhysicalDeleteOptionCode(any(Long[].class));
    }

    @Test
    @DisplayName("deleteOptionCodeByIds应成功删除非MDM来源的选装值")
    void testDeleteOptionCodeByIds() {
        Long[] ids = {1L, 2L};

        OptionCode oc1 = OptionCode.builder().id(1L).code("OC001").source("VMD").build();
        OptionCode oc2 = OptionCode.builder().id(2L).code("OC002").source("VMD").build();

        when(mdmOptionFamilyRepository.selectOptionCodeById(1L)).thenReturn(oc1);
        when(mdmOptionFamilyRepository.selectOptionCodeById(2L)).thenReturn(oc2);
        when(mdmOptionFamilyRepository.batchPhysicalDeleteOptionCode(ids)).thenReturn(2);

        int result = optionFamilyAppService.deleteOptionCodeByIds(1L, ids);

        assertEquals(2, result);
        verify(mdmOptionFamilyRepository).batchPhysicalDeleteOptionCode(ids);
    }

    @Test
    @DisplayName("getOptionCodeById应返回选装值DTO")
    void testGetOptionCodeById() {
        Long id = 1L;
        OptionCode optionCode = OptionCode.builder().id(id).code("OC001").name("选装值1").build();

        when(mdmOptionFamilyRepository.selectOptionCodeById(id)).thenReturn(optionCode);

        OptionCodeDto result = optionFamilyAppService.getOptionCodeById(1L, id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(mdmOptionFamilyRepository).selectOptionCodeById(id);
    }

    @Test
    @DisplayName("getOptionCodeByCode应返回选装值DTO")
    void testGetOptionCodeByCode() {
        String code = "OC001";
        OptionCode optionCode = OptionCode.builder().id(1L).code(code).name("选装值1").build();

        when(mdmOptionFamilyRepository.selectOptionCodeByCode(code)).thenReturn(optionCode);

        OptionCodeDto result = optionFamilyAppService.getOptionCodeByCode(code);

        assertNotNull(result);
        assertEquals(code, result.getCode());
        verify(mdmOptionFamilyRepository).selectOptionCodeByCode(code);
    }

    @Test
    @DisplayName("listAllOptionCodeByOptionFamilyCode应返回选装值列表")
    void testListAllOptionCodeByOptionFamilyCode() {
        String familyCode = "OF001";
        OptionCode oc1 = OptionCode.builder().id(1L).code("OC001").optionFamilyCode(familyCode).build();
        OptionCode oc2 = OptionCode.builder().id(2L).code("OC002").optionFamilyCode(familyCode).build();

        when(mdmOptionFamilyRepository.selectOptionCodeByOptionFamilyCode(familyCode)).thenReturn(Arrays.asList(oc1, oc2));

        List<OptionCodeDto> result = optionFamilyAppService.listAllOptionCodeByOptionFamilyCode(familyCode);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mdmOptionFamilyRepository).selectOptionCodeByOptionFamilyCode(familyCode);
    }

    @Test
    @DisplayName("searchOptionCode应返回选装值列表")
    void testSearchOptionCode() {
        OptionCodeQuery query = OptionCodeQuery.builder()
                .optionFamilyCode("OF001")
                .build();

        OptionCode oc1 = OptionCode.builder().id(1L).code("OC001").optionFamilyCode("OF001").build();

        when(mdmOptionFamilyRepository.selectOptionCodeByOptionFamilyCode("OF001")).thenReturn(Arrays.asList(oc1));

        List<OptionCodeDto> result = optionFamilyAppService.searchOptionCode(query);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mdmOptionFamilyRepository).selectOptionCodeByOptionFamilyCode("OF001");
    }
}
