package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleOption;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehicleOptionMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleOptionPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VehicleOptionRepository 单元测试
 *
 * @author VMD-DSN-CR-030 / US-043
 */
@ExtendWith(MockitoExtension.class)
class VehicleOptionRepositoryImplTest {

    @Mock
    private VehicleOptionMapper mapper;

    @InjectMocks
    private VehicleOptionRepositoryImpl repository;

    @Test
    @DisplayName("应成功批量插入或更新选项值快照")
    void shouldBatchUpsertVehicleOptions() {
        List<VehicleOption> options = Arrays.asList(
            VehicleOption.builder()
                .vin("WVWZZZ3CZWE123456")
                .optionFamilyCode("COLOR")
                .optionCode("RED")
                .source("PRODUCE")
                .batchNum("BATCH001")
                .snapshotTime(LocalDateTime.now())
                .build(),
            VehicleOption.builder()
                .vin("WVWZZZ3CZWE123456")
                .optionFamilyCode("INTERIOR")
                .optionCode("BLACK")
                .source("PRODUCE")
                .batchNum("BATCH001")
                .snapshotTime(LocalDateTime.now())
                .build()
        );

        when(mapper.selectPoByMap(any())).thenReturn(Collections.emptyList());
        when(mapper.insertPo(any(VehicleOptionPo.class))).thenReturn(1);

        repository.batchUpsert(options);

        verify(mapper, times(2)).insertPo(any(VehicleOptionPo.class));
    }

    @Test
    @DisplayName("应成功根据VIN查询选项值快照列表")
    void shouldFindByVin() {
        String vin = "WVWZZZ3CZWE123456";
        List<VehicleOptionPo> poList = Arrays.asList(
            VehicleOptionPo.builder()
                .id(1L)
                .vin(vin)
                .optionFamilyCode("COLOR")
                .optionCode("RED")
                .build()
        );

        when(mapper.selectPoByMap(any())).thenReturn(poList);

        List<VehicleOption> result = repository.findByVin(vin);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(vin, result.get(0).getVin());
    }

    @Test
    @DisplayName("应成功根据VIN和选项族编码查询选项值快照")
    void shouldFindByVinAndOptionFamilyCode() {
        String vin = "WVWZZZ3CZWE123456";
        String optionFamilyCode = "COLOR";
        VehicleOptionPo po = VehicleOptionPo.builder()
            .id(1L)
            .vin(vin)
            .optionFamilyCode(optionFamilyCode)
            .optionCode("RED")
            .build();

        when(mapper.selectPoByExample(any())).thenReturn(Arrays.asList(po));

        VehicleOption result = repository.findByVinAndOptionFamilyCode(vin, optionFamilyCode);

        assertNotNull(result);
        assertEquals(vin, result.getVin());
        assertEquals(optionFamilyCode, result.getOptionFamilyCode());
    }

    @Test
    @DisplayName("当VIN和选项族编码不存在时应返回null")
    void shouldReturnNullWhenNotFound() {
        String vin = "NONEXISTENT";
        String optionFamilyCode = "NONEXISTENT";

        when(mapper.selectPoByExample(any())).thenReturn(Collections.emptyList());

        VehicleOption result = repository.findByVinAndOptionFamilyCode(vin, optionFamilyCode);

        assertNull(result);
    }
}
