package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateDeviceUidUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * BoundDeviceIdentityResolver 单元测试（CR-054）
 * <p>
 * 覆盖权威 HSM UID 解析优先级、双来源一致性核对、缺失/冲突 fail-closed。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@ExtendWith(MockitoExtension.class)
class BoundDeviceIdentityResolverTest {

    private static final String VIN = "HWYZTEST900000001";
    private static final String DEVICE_SN = "00000005AA00000001";
    private static final String HSM_UID = "00000000000000000000000000000001";
    private static final String PART_CODE = "TBOX5G";

    @Mock
    private PartInfoRepository partInfoRepository;

    @Mock
    private VehiclePartRepository vehiclePartRepository;

    @Mock
    private PartSecurityConstantRepository partSecurityConstantRepository;

    private BoundDeviceIdentityResolver resolver;

    private PartInfo partInfo;
    private VehiclePart activeBinding;

    @BeforeEach
    void setUp() {
        resolver = new BoundDeviceIdentityResolver(partInfoRepository, vehiclePartRepository, partSecurityConstantRepository);
        partInfo = PartInfo.builder()
                .id(1L)
                .partCode(PART_CODE)
                .sn(DEVICE_SN)
                .build();
        activeBinding = VehiclePart.builder()
                .id(10L)
                .vin(VIN)
                .partId(1L)
                .build();
        lenient().when(partInfoRepository.selectBySn(DEVICE_SN)).thenReturn(partInfo);
        lenient().when(vehiclePartRepository.selectActiveByVinAndPartId(VIN, 1L)).thenReturn(activeBinding);
    }

    @Test
    void resolve_extra与chipUid一致_应返回BOTH来源() {
        // Given：part_info.extra.HSM 与 part_security_constant.chip_uid 一致
        partInfo.setExtra("{\"hsm\":\"" + HSM_UID + "\"}");
        when(partSecurityConstantRepository.selectByPartCodeAndSn(PART_CODE, DEVICE_SN))
                .thenReturn(PartSecurityConstant.builder().sn(DEVICE_SN).chipUid(HSM_UID).build());

        // When
        BoundDeviceIdentity id = resolver.resolve(VIN, DEVICE_SN, "TBOX");

        // Then
        assertEquals(HSM_UID, id.hsmUid());
        assertEquals("BOTH", id.uidSource());
        assertEquals(10L, id.bindingId());
        assertEquals(1L, id.partId());
    }

    @Test
    void resolve_仅extra有HSM_应使用PART_INFO_EXTRA() {
        // Given：part_info.extra.HSM 有值，chip_uid 无
        partInfo.setExtra("{\"hsm\":\"" + HSM_UID + "\"}");
        when(partSecurityConstantRepository.selectByPartCodeAndSn(PART_CODE, DEVICE_SN)).thenReturn(null);

        // When
        BoundDeviceIdentity id = resolver.resolve(VIN, DEVICE_SN, "TBOX");

        // Then
        assertEquals(HSM_UID, id.hsmUid());
        assertEquals("PART_INFO_EXTRA", id.uidSource());
    }

    @Test
    void resolve_仅chipUid有值_应作为存量受控兜底() {
        // Given：part_info.extra 无 HSM，chip_uid 有值（存量数据受控兜底）
        partInfo.setExtra(null);
        when(partSecurityConstantRepository.selectByPartCodeAndSn(PART_CODE, DEVICE_SN))
                .thenReturn(PartSecurityConstant.builder().sn(DEVICE_SN).chipUid(HSM_UID).build());

        // When
        BoundDeviceIdentity id = resolver.resolve(VIN, DEVICE_SN, "TBOX");

        // Then
        assertEquals(HSM_UID, id.hsmUid());
        assertEquals("SECURITY_CONSTANT", id.uidSource());
    }

    @Test
    void resolve_双来源规范化后不一致_应failClosed() {
        // Given：extra.HSM 与 chip_uid 不同（数据冲突），不得任选其一
        partInfo.setExtra("{\"hsm\":\"" + HSM_UID + "\"}");
        when(partSecurityConstantRepository.selectByPartCodeAndSn(PART_CODE, DEVICE_SN))
                .thenReturn(PartSecurityConstant.builder().sn(DEVICE_SN).chipUid("00000000000000000000000000000009").build());

        // When & Then：806065
        CertificateDeviceUidUnavailableException ex = assertThrows(CertificateDeviceUidUnavailableException.class,
                () -> resolver.resolve(VIN, DEVICE_SN, "TBOX"));
        assertEquals("806065", ex.getErrorCode().getCode());
    }

    @Test
    void resolve_双来源均缺失_应failClosed且不得退回deviceSn() {
        // Given：两处均无 HSM UID（身份数据不完整）
        partInfo.setExtra(null);
        when(partSecurityConstantRepository.selectByPartCodeAndSn(PART_CODE, DEVICE_SN)).thenReturn(null);

        // When & Then：806065，不得把 device_sn 当作 hsm_uid
        CertificateDeviceUidUnavailableException ex = assertThrows(CertificateDeviceUidUnavailableException.class,
                () -> resolver.resolve(VIN, DEVICE_SN, "TBOX"));
        assertEquals("806065", ex.getErrorCode().getCode());
    }

    @Test
    void resolve_大小写与前缀规范化等价_应一致() {
        // Given：extra.HSM 带 0x 前缀小写，chip_uid 大写——规范化后相等
        partInfo.setExtra("{\"hsm\":\"0x" + HSM_UID.toLowerCase() + "\"}");
        when(partSecurityConstantRepository.selectByPartCodeAndSn(PART_CODE, DEVICE_SN))
                .thenReturn(PartSecurityConstant.builder().sn(DEVICE_SN).chipUid(HSM_UID).build());

        // When
        BoundDeviceIdentity id = resolver.resolve(VIN, DEVICE_SN, "TBOX");

        // Then
        assertEquals(HSM_UID, id.hsmUid());
        assertEquals("BOTH", id.uidSource());
    }

    @Test
    void resolve_设备不存在_应抛参数异常() {
        // Given
        when(partInfoRepository.selectBySn(DEVICE_SN)).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(VIN, DEVICE_SN, "TBOX"));
    }

    @Test
    void resolve_active绑定不存在_应抛状态异常() {
        // Given
        when(vehiclePartRepository.selectActiveByVinAndPartId(VIN, 1L)).thenReturn(null);

        // When & Then
        assertThrows(IllegalStateException.class, () -> resolver.resolve(VIN, DEVICE_SN, "TBOX"));
    }

    @Test
    void resolve_extra非法JSON_应按缺失处理() {
        // Given：extra 非 JSON，视为无 extra.HSM；chip_uid 有值兜底
        partInfo.setExtra("NOT_JSON");
        when(partSecurityConstantRepository.selectByPartCodeAndSn(anyString(), anyString()))
                .thenReturn(PartSecurityConstant.builder().sn(DEVICE_SN).chipUid(HSM_UID).build());

        // When
        BoundDeviceIdentity id = resolver.resolve(VIN, DEVICE_SN, "TBOX");

        // Then
        assertEquals(HSM_UID, id.hsmUid());
        assertEquals("SECURITY_CONSTANT", id.uidSource());
    }

}
