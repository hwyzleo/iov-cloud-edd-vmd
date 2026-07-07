package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleNodeSchemaRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.ProvisioningResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartSecurityPresetAppServiceTest {

    @Mock
    private PartSecurityConstantRepository partSecurityConstantRepository;

    @Mock
    private PartImportDataRepository partImportDataRepository;

    @Mock
    private KeyProvisioningTemplate keyProvisioningTemplate;

    @Mock
    private VehicleNodeSchemaRegistry vehicleNodeSchemaRegistry;

    @InjectMocks
    private PartSecurityPresetAppService service;

    private static final String TEST_PART_CODE = "TBOX_001";
    private static final String TEST_SN = "SN_123456";
    private static final String TEST_CHIP_UID = "CHIP_UID_789";
    private static final String TEST_BATCH_NUM = "BATCH_001";
    private static final String TEST_VEHICLE_NODE_CODE = "TBOX_5G";

    private ProvisioningResult mockResult() {
        ProvisioningResult result = new ProvisioningResult();
        result.setKmsKeyRef("dev-root-master:sn:CHIP_UID_789");
        result.setKeySpec("256-bit");
        result.setProvider("Vault-Transit");
        result.setAlgorithm("HMAC-SHA256");
        result.setKcv(new byte[]{1, 2, 3, 4});
        result.setWrappedMaterial(null);
        return result;
    }

    @Test
    void shouldSkipPresetWhenAlreadyPreseted() {
        PartSecurityConstant existing = PartSecurityConstant.builder()
                .partCode(TEST_PART_CODE)
                .sn(TEST_SN)
                .presetState(SecurityConstantState.PRESET)
                .build();
        when(partSecurityConstantRepository.selectByPartCodeAndSn(TEST_PART_CODE, TEST_SN)).thenReturn(existing);

        service.preset(TEST_PART_CODE, TEST_SN, TEST_CHIP_UID, TEST_BATCH_NUM, TEST_VEHICLE_NODE_CODE);

        verify(partSecurityConstantRepository, never()).insert(any());
        verify(partSecurityConstantRepository, never()).update(any());
        verify(keyProvisioningTemplate, never()).deriveByUid(any(), any());
    }

    @Test
    void shouldCreateNewRecordAndCallKeyProvisioningTemplate() {
        when(partSecurityConstantRepository.selectByPartCodeAndSn(TEST_PART_CODE, TEST_SN)).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(vehicleNodeSchemaRegistry.getBizType(TEST_VEHICLE_NODE_CODE)).thenReturn(BizType.TBOX_DEVICE_ROOT);
        when(keyProvisioningTemplate.deriveByUid(TEST_CHIP_UID, BizType.TBOX_DEVICE_ROOT)).thenReturn(mockResult());

        service.preset(TEST_PART_CODE, TEST_SN, TEST_CHIP_UID, TEST_BATCH_NUM, TEST_VEHICLE_NODE_CODE);

        verify(partSecurityConstantRepository).insert(any(PartSecurityConstant.class));
        verify(keyProvisioningTemplate).deriveByUid(TEST_CHIP_UID, BizType.TBOX_DEVICE_ROOT);
        verify(partSecurityConstantRepository).update(argThat(constant ->
                constant.getPartCode().equals(TEST_PART_CODE) &&
                constant.getSn().equals(TEST_SN) &&
                constant.getChipUid().equals(TEST_CHIP_UID) &&
                constant.getPresetState() == SecurityConstantState.PRESET &&
                constant.getConstantType().equals("ROOT") &&
                constant.getKmsKeyRef().equals("dev-root-master:sn:CHIP_UID_789") &&
                constant.getKeySpec().equals("256-bit") &&
                constant.getKmsProvider().equals("Vault-Transit") &&
                constant.getAlgorithm().equals("HMAC-SHA256") &&
                constant.getKcv().equals("01020304")
        ));
    }

    @Test
    void shouldHandleFailureAndNotThrow() {
        when(partSecurityConstantRepository.selectByPartCodeAndSn(TEST_PART_CODE, TEST_SN)).thenReturn(null);
        when(partSecurityConstantRepository.insert(any())).thenReturn(1);
        when(vehicleNodeSchemaRegistry.getBizType(TEST_VEHICLE_NODE_CODE)).thenReturn(BizType.TBOX_DEVICE_ROOT);
        when(keyProvisioningTemplate.deriveByUid(TEST_CHIP_UID, BizType.TBOX_DEVICE_ROOT))
                .thenThrow(new RuntimeException("KMS unavailable"));
        when(partImportDataRepository.selectByBatchNum(TEST_BATCH_NUM)).thenReturn(null);
        when(partSecurityConstantRepository.update(any())).thenReturn(1);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                service.preset(TEST_PART_CODE, TEST_SN, TEST_CHIP_UID, TEST_BATCH_NUM, TEST_VEHICLE_NODE_CODE));

        verify(partSecurityConstantRepository).update(argThat(constant ->
                constant.getPresetState() == SecurityConstantState.FAILED &&
                constant.getFailReason().equals("KMS unavailable")
        ));
    }
}
