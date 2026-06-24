package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.KmsHsmClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.dto.KmsHsmResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartSecurityPresetAppServiceTest {

    @Mock
    private PartSecurityConstantRepository partSecurityConstantRepository;

    @Mock
    private PartImportDataRepository partImportDataRepository;

    @Mock
    private KmsHsmClient kmsHsmClient;

    @InjectMocks
    private PartSecurityPresetAppService service;

    private static final String TEST_PART_CODE = "TBOX_001";
    private static final String TEST_SN = "SN_123456";
    private static final String TEST_CHIP_UID = "CHIP_UID_789";
    private static final String TEST_BATCH_NUM = "BATCH_001";

    @Test
    void shouldSkipPresetWhenAlreadyPreseted() throws Exception {
        // Given
        PartSecurityConstant existing = PartSecurityConstant.builder()
                .partCode(TEST_PART_CODE)
                .sn(TEST_SN)
                .presetState(SecurityConstantState.PRESET)
                .build();
        when(partSecurityConstantRepository.selectByPartCodeAndSn(TEST_PART_CODE, TEST_SN)).thenReturn(existing);

        // When
        service.preset(TEST_PART_CODE, TEST_SN, TEST_CHIP_UID, TEST_BATCH_NUM);

        // Then
        verify(partSecurityConstantRepository, never()).insert(any());
        verify(partSecurityConstantRepository, never()).update(any());
        verify(kmsHsmClient, never()).generatePerDeviceConstant(any(), any(), any());
    }

    @Test
    void shouldCreateNewRecordAndCallKmsHsm() throws Exception {
        // Given
        when(partSecurityConstantRepository.selectByPartCodeAndSn(TEST_PART_CODE, TEST_SN)).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        KmsHsmResult kmsResult = KmsHsmResult.builder()
                .kmsKeyRef("test_key_ref")
                .keySpec("AES-256")
                .provider("TestKMS")
                .algorithm("AES")
                .build();
        when(kmsHsmClient.generatePerDeviceConstant(TEST_PART_CODE, TEST_SN, "ROOT")).thenReturn(kmsResult);

        // When
        service.preset(TEST_PART_CODE, TEST_SN, TEST_CHIP_UID, TEST_BATCH_NUM);

        // Then
        verify(partSecurityConstantRepository).insert(any(PartSecurityConstant.class));
        verify(kmsHsmClient).generatePerDeviceConstant(TEST_PART_CODE, TEST_SN, "ROOT");
        verify(partSecurityConstantRepository).update(argThat(constant ->
                constant.getPartCode().equals(TEST_PART_CODE) &&
                constant.getSn().equals(TEST_SN) &&
                constant.getChipUid().equals(TEST_CHIP_UID) &&
                constant.getPresetState() == SecurityConstantState.PRESET &&
                constant.getConstantType().equals("ROOT") &&
                constant.getKmsKeyRef().equals("test_key_ref") &&
                constant.getKeySpec().equals("AES-256") &&
                constant.getKmsProvider().equals("TestKMS") &&
                constant.getAlgorithm().equals("AES")
        ));
    }
}