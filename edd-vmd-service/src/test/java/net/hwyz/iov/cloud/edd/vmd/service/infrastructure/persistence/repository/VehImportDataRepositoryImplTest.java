package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehImportDataMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportDataPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VehImportDataRepository 单元测试
 * <p>
 * VMD-DSN-CR-027: 车辆导入数据仓储测试
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@ExtendWith(MockitoExtension.class)
class VehImportDataRepositoryImplTest {

    @Mock
    private VehImportDataMapper vehImportDataMapper;

    private VehImportDataRepositoryImpl vehImportDataRepository;

    @BeforeEach
    void setUp() {
        vehImportDataRepository = new VehImportDataRepositoryImpl(vehImportDataMapper);
    }

    @Test
    @DisplayName("应成功插入车辆导入数据记录")
    void insert_shouldSuccessfullyInsertVehImportData() {
        // Given
        VehImportData vehImportData = VehImportData.builder()
                .batchNum("TEST_VEH_001")
                .type("PRODUCE")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{},\"DATA\":{\"ITEMS\":[]}}}")
                .handle(false)
                .createTime(LocalDateTime.now())
                .build();

        when(vehImportDataMapper.insertPo(any(VehImportDataPo.class))).thenReturn(1);

        // When
        int result = vehImportDataRepository.insert(vehImportData);

        // Then
        assertEquals(1, result);
        verify(vehImportDataMapper).insertPo(any(VehImportDataPo.class));
    }

    @Test
    @DisplayName("应成功根据ID查询车辆导入数据")
    void selectById_shouldReturnVehImportDataWhenExists() {
        // Given
        Long id = 1L;
        VehImportDataPo po = VehImportDataPo.builder()
                .id(id)
                .batchNum("TEST_VEH_001")
                .type("PRODUCE")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{},\"DATA\":{\"ITEMS\":[]}}}")
                .handle(false)
                .build();

        when(vehImportDataMapper.selectPoById(id)).thenReturn(po);

        // When
        VehImportData result = vehImportDataRepository.selectById(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("TEST_VEH_001", result.getBatchNum());
        assertEquals("PRODUCE", result.getType());
    }

    @Test
    @DisplayName("应成功根据批次号查询车辆导入数据")
    void selectByBatchNum_shouldReturnVehImportDataWhenExists() {
        // Given
        String batchNum = "TEST_VEH_002";
        VehImportDataPo po = VehImportDataPo.builder()
                .id(1L)
                .batchNum(batchNum)
                .type("PRODUCE")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{},\"DATA\":{\"ITEMS\":[]}}}")
                .handle(false)
                .build();

        when(vehImportDataMapper.selectPoByBatchNum(batchNum)).thenReturn(po);

        // When
        VehImportData result = vehImportDataRepository.selectByBatchNum(batchNum);

        // Then
        assertNotNull(result);
        assertEquals(batchNum, result.getBatchNum());
    }

    @Test
    @DisplayName("应成功更新车辆导入数据")
    void update_shouldSuccessfullyUpdateVehImportData() {
        // Given
        VehImportData vehImportData = VehImportData.builder()
                .id(1L)
                .batchNum("TEST_VEH_003")
                .type("PRODUCE")
                .version("1.0")
                .data("{\"REQUEST\":{\"HEAD\":{},\"DATA\":{\"ITEMS\":[]}}}")
                .handle(true)
                .description("处理成功")
                .build();

        when(vehImportDataMapper.updatePo(any(VehImportDataPo.class))).thenReturn(1);

        // When
        int result = vehImportDataRepository.update(vehImportData);

        // Then
        assertEquals(1, result);
        verify(vehImportDataMapper).updatePo(any(VehImportDataPo.class));
    }

    @Test
    @DisplayName("应成功批量删除车辆导入数据")
    void deleteByIds_shouldSuccessfullyDeleteVehImportData() {
        // Given
        Long[] ids = {1L, 2L, 3L};
        when(vehImportDataMapper.batchPhysicalDeletePo(ids)).thenReturn(3);

        // When
        int result = vehImportDataRepository.deleteByIds(ids);

        // Then
        assertEquals(3, result);
        verify(vehImportDataMapper).batchPhysicalDeletePo(ids);
    }

    @Test
    @DisplayName("应成功查询车辆导入数据列表")
    void selectList_shouldReturnVehImportDataList() {
        // Given
        VehImportDataPo po1 = VehImportDataPo.builder()
                .id(1L)
                .batchNum("TEST_VEH_004")
                .type("PRODUCE")
                .version("1.0")
                .handle(false)
                .build();
        VehImportDataPo po2 = VehImportDataPo.builder()
                .id(2L)
                .batchNum("TEST_VEH_005")
                .type("PRODUCE")
                .version("1.0")
                .handle(true)
                .build();

        when(vehImportDataMapper.selectPoByMap(any())).thenReturn(Arrays.asList(po1, po2));

        // When
        VehImportData query = VehImportData.builder().type("PRODUCE").build();
        List<VehImportData> result = vehImportDataRepository.selectList(query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("TEST_VEH_004", result.get(0).getBatchNum());
        assertEquals("TEST_VEH_005", result.get(1).getBatchNum());
    }

    @Test
    @DisplayName("批次号唯一性检查应正确工作")
    void checkBatchNumUnique_shouldReturnCorrectResult() {
        // Given
        when(vehImportDataMapper.countPoByBatchNum("NEW_BATCH")).thenReturn(0L);
        when(vehImportDataMapper.countPoByBatchNum("EXIST_BATCH")).thenReturn(1L);
        when(vehImportDataMapper.countPoByBatchNumAndIdNot("EXIST_BATCH", 1L)).thenReturn(0L);

        // When & Then
        assertTrue(vehImportDataRepository.checkBatchNumUnique(null, "NEW_BATCH"));
        assertFalse(vehImportDataRepository.checkBatchNumUnique(null, "EXIST_BATCH"));
        assertTrue(vehImportDataRepository.checkBatchNumUnique(1L, "EXIST_BATCH"));
    }
}
