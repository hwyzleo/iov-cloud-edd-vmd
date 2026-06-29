package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehiclePartBindingChangedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BindingChangeType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmVehicleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VehiclePartBindingPublisher 单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class VehiclePartBindingPublisherTest {

    @Mock
    private ApplicationContext ctx;

    @Mock
    private PartInfoRepository partInfoRepository;

    @Mock
    private MdmVehicleNodeRepository vehicleNodeRepository;

    @InjectMocks
    private VehiclePartBindingPublisher publisher;

    private static final String VIN = "LVSHFFAN5KF000001";
    private static final Long BINDING_ID = 100L;
    private static final Long PART_ID = 200L;
    private static final String PART_CODE = "TBOX-001";
    private static final String SN = "SN001";
    private static final String VEHICLE_NODE_CODE = "TBOX_5G";
    private static final String DEVICE_CATEGORY_TBOX = "TBOX";
    private static final String DEVICE_CATEGORY_CCP = "CCP";

    private VehiclePart vehiclePart;
    private PartInfo partInfo;
    private VehicleNode vehicleNodeTbox;
    private VehicleNode vehicleNodeCcp;

    @BeforeEach
    void setUp() {
        vehiclePart = VehiclePart.builder()
                .id(BINDING_ID)
                .vin(VIN)
                .partId(PART_ID)
                .vehicleNodeCode(VEHICLE_NODE_CODE)
                .bindTime(Instant.now())
                .build();

        Map<String, String> extraFields = new HashMap<>();
        extraFields.put("iccid1", "ICCID001");
        extraFields.put("iccid2", "ICCID002");
        extraFields.put("imei", "IMEI001");
        extraFields.put("hsm", "HSM001");

        partInfo = PartInfo.builder()
                .id(PART_ID)
                .partCode(PART_CODE)
                .sn(SN)
                .extra(JSONUtil.toJsonStr(extraFields))
                .build();

        vehicleNodeTbox = VehicleNode.builder()
                .code(VEHICLE_NODE_CODE)
                .deviceCategory(DEVICE_CATEGORY_TBOX)
                .build();

        vehicleNodeCcp = VehicleNode.builder()
                .code("CCP_01")
                .deviceCategory(DEVICE_CATEGORY_CCP)
                .build();
    }

    @Test
    void publishBindingChanged_tboxBinding_extractsIccid() {
        // Given
        when(partInfoRepository.selectById(PART_ID)).thenReturn(partInfo);
        when(vehicleNodeRepository.selectByCode(VEHICLE_NODE_CODE)).thenReturn(vehicleNodeTbox);

        // When
        publisher.publishBindingChanged(vehiclePart, BindingChangeType.BIND);

        // Then
        ArgumentCaptor<VehiclePartBindingChangedEvent> eventCaptor = ArgumentCaptor.forClass(VehiclePartBindingChangedEvent.class);
        verify(ctx).publishEvent(eventCaptor.capture());

        VehiclePartBindingChangedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(VIN, capturedEvent.getVin());
        assertEquals(BINDING_ID, capturedEvent.getBindingId());
        assertEquals(PART_CODE, capturedEvent.getPartCode());
        assertEquals(SN, capturedEvent.getSn());
        assertEquals(DEVICE_CATEGORY_TBOX, capturedEvent.getDeviceCategory());
        assertEquals("ICCID001", capturedEvent.getIccid1());
        assertEquals("ICCID002", capturedEvent.getIccid2());
        assertEquals(BindingChangeType.BIND, capturedEvent.getChangeType());
    }

    @Test
    void publishBindingChanged_nonTboxBinding_setsIccidToNull() {
        // Given
        when(partInfoRepository.selectById(PART_ID)).thenReturn(partInfo);
        when(vehicleNodeRepository.selectByCode(VEHICLE_NODE_CODE)).thenReturn(vehicleNodeCcp);

        // When
        publisher.publishBindingChanged(vehiclePart, BindingChangeType.BIND);

        // Then
        ArgumentCaptor<VehiclePartBindingChangedEvent> eventCaptor = ArgumentCaptor.forClass(VehiclePartBindingChangedEvent.class);
        verify(ctx).publishEvent(eventCaptor.capture());

        VehiclePartBindingChangedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(DEVICE_CATEGORY_CCP, capturedEvent.getDeviceCategory());
        assertNull(capturedEvent.getIccid1());
        assertNull(capturedEvent.getIccid2());
    }

    @Test
    void publishBindingChanged_tboxBindingNoExtra_setsIccidToNull() {
        // Given
        PartInfo partInfoNoExtra = PartInfo.builder()
                .id(PART_ID)
                .partCode(PART_CODE)
                .sn(SN)
                .extra(null)
                .build();
        when(partInfoRepository.selectById(PART_ID)).thenReturn(partInfoNoExtra);
        when(vehicleNodeRepository.selectByCode(VEHICLE_NODE_CODE)).thenReturn(vehicleNodeTbox);

        // When
        publisher.publishBindingChanged(vehiclePart, BindingChangeType.BIND);

        // Then
        ArgumentCaptor<VehiclePartBindingChangedEvent> eventCaptor = ArgumentCaptor.forClass(VehiclePartBindingChangedEvent.class);
        verify(ctx).publishEvent(eventCaptor.capture());

        VehiclePartBindingChangedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(DEVICE_CATEGORY_TBOX, capturedEvent.getDeviceCategory());
        assertNull(capturedEvent.getIccid1());
        assertNull(capturedEvent.getIccid2());
    }

    @Test
    void publishBindingChanged_tboxBindingEmptyExtra_setsIccidToNull() {
        // Given
        PartInfo partInfoEmptyExtra = PartInfo.builder()
                .id(PART_ID)
                .partCode(PART_CODE)
                .sn(SN)
                .extra("{}")
                .build();
        when(partInfoRepository.selectById(PART_ID)).thenReturn(partInfoEmptyExtra);
        when(vehicleNodeRepository.selectByCode(VEHICLE_NODE_CODE)).thenReturn(vehicleNodeTbox);

        // When
        publisher.publishBindingChanged(vehiclePart, BindingChangeType.BIND);

        // Then
        ArgumentCaptor<VehiclePartBindingChangedEvent> eventCaptor = ArgumentCaptor.forClass(VehiclePartBindingChangedEvent.class);
        verify(ctx).publishEvent(eventCaptor.capture());

        VehiclePartBindingChangedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(DEVICE_CATEGORY_TBOX, capturedEvent.getDeviceCategory());
        assertNull(capturedEvent.getIccid1());
        assertNull(capturedEvent.getIccid2());
    }

    @Test
    void publishBindingChanged_tboxBindingOnlyIccid1_extractsIccid1Only() {
        // Given
        Map<String, String> extraFields = new HashMap<>();
        extraFields.put("iccid1", "ICCID001");
        extraFields.put("imei", "IMEI001");

        PartInfo partInfoOnlyIccid1 = PartInfo.builder()
                .id(PART_ID)
                .partCode(PART_CODE)
                .sn(SN)
                .extra(JSONUtil.toJsonStr(extraFields))
                .build();
        when(partInfoRepository.selectById(PART_ID)).thenReturn(partInfoOnlyIccid1);
        when(vehicleNodeRepository.selectByCode(VEHICLE_NODE_CODE)).thenReturn(vehicleNodeTbox);

        // When
        publisher.publishBindingChanged(vehiclePart, BindingChangeType.BIND);

        // Then
        ArgumentCaptor<VehiclePartBindingChangedEvent> eventCaptor = ArgumentCaptor.forClass(VehiclePartBindingChangedEvent.class);
        verify(ctx).publishEvent(eventCaptor.capture());

        VehiclePartBindingChangedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(DEVICE_CATEGORY_TBOX, capturedEvent.getDeviceCategory());
        assertEquals("ICCID001", capturedEvent.getIccid1());
        assertNull(capturedEvent.getIccid2());
    }

    @Test
    void publishBindingChanged_partInfoNotFound_setsIccidToNull() {
        // Given
        when(partInfoRepository.selectById(PART_ID)).thenReturn(null);
        when(vehicleNodeRepository.selectByCode(VEHICLE_NODE_CODE)).thenReturn(vehicleNodeTbox);

        // When
        publisher.publishBindingChanged(vehiclePart, BindingChangeType.BIND);

        // Then
        ArgumentCaptor<VehiclePartBindingChangedEvent> eventCaptor = ArgumentCaptor.forClass(VehiclePartBindingChangedEvent.class);
        verify(ctx).publishEvent(eventCaptor.capture());

        VehiclePartBindingChangedEvent capturedEvent = eventCaptor.getValue();
        assertNull(capturedEvent.getPartCode());
        assertNull(capturedEvent.getSn());
        assertNull(capturedEvent.getIccid1());
        assertNull(capturedEvent.getIccid2());
    }

    @Test
    void publishBindingChanged_vehicleNodeNotFound_setsIccidToNull() {
        // Given
        when(partInfoRepository.selectById(PART_ID)).thenReturn(partInfo);
        when(vehicleNodeRepository.selectByCode(VEHICLE_NODE_CODE)).thenReturn(null);

        // When
        publisher.publishBindingChanged(vehiclePart, BindingChangeType.BIND);

        // Then
        ArgumentCaptor<VehiclePartBindingChangedEvent> eventCaptor = ArgumentCaptor.forClass(VehiclePartBindingChangedEvent.class);
        verify(ctx).publishEvent(eventCaptor.capture());

        VehiclePartBindingChangedEvent capturedEvent = eventCaptor.getValue();
        assertNull(capturedEvent.getDeviceCategory());
        assertNull(capturedEvent.getIccid1());
        assertNull(capturedEvent.getIccid2());
    }
}
