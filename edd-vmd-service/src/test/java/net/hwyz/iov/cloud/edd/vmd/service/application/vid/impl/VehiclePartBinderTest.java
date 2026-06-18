package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolPartBoundEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiclePartBinderTest {

    @Mock
    private PartInfoAppService partInfoAppService;

    @Mock
    private VehiclePartAppService vehiclePartAppService;

    @Mock
    private VehicleNodeAppService vehicleNodeAppService;

    @InjectMocks
    private VehiclePartBinder binder;

    private static final String VIN = "HWYZTEST000000001";
    private static final String BATCH_NUM = "BATCH001";

    @Test
    void bindParts_validParts_bindsAndReturnsMetaList() {
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.set("DEVICE_CODE", "TBOX001");
        part.set("VIN", VIN);
        part.set("PART_NO", "PN001");
        part.set("PART_SN", "SN001");
        part.set("SUPPLIER_CODE", "SUP001");
        part.set("CONFIG_WORD", "CW01");
        part.set("HARDWARE_VERSION", "HW1.0");
        part.set("SOFTWARE_VERSION", "SW1.0");
        part.set("HARDWARE_PN", "HPN001");
        part.set("SOFTWARE_PN", "SPN001");
        part.set("ICCID1", "IC001");
        part.set("ICCID2", "IC002");
        parts.add(part);

        VehicleNode device = VehicleNode.builder().code("TBOX001").deviceCategory("TBOX").build();
        when(vehicleNodeAppService.getVehicleNodeByCode("TBOX001")).thenReturn(device);

        List<VehicleEolPartBoundEvent.PartMeta> result = binder.bindParts(parts, VIN, BATCH_NUM);

        assertEquals(1, result.size());
        VehicleEolPartBoundEvent.PartMeta meta = result.get(0);
        assertEquals("SN001", meta.getSn());
        assertEquals("PN001", meta.getCode());
        assertEquals("TBOX001", meta.getVehicleNodeCode());
        assertEquals("TBOX", meta.getDeviceItem());
        verify(partInfoAppService).upsertPartInfo(any(PartInfo.class));
        verify(vehiclePartAppService).bindVehiclePart(any(VehiclePart.class));
    }

    @Test
    void bindParts_blankDeviceCode_skipsPart() {
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.set("DEVICE_CODE", "");
        part.set("VIN", VIN);
        parts.add(part);

        List<VehicleEolPartBoundEvent.PartMeta> result = binder.bindParts(parts, VIN, BATCH_NUM);

        assertTrue(result.isEmpty());
        verify(partInfoAppService, never()).upsertPartInfo(any());
        verify(vehiclePartAppService, never()).bindVehiclePart(any());
    }

    @Test
    void bindParts_vinMismatch_skipsPart() {
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.set("DEVICE_CODE", "TBOX001");
        part.set("VIN", "DIFFERENT_VIN");
        parts.add(part);

        List<VehicleEolPartBoundEvent.PartMeta> result = binder.bindParts(parts, VIN, BATCH_NUM);

        assertTrue(result.isEmpty());
        verify(partInfoAppService, never()).upsertPartInfo(any());
        verify(vehiclePartAppService, never()).bindVehiclePart(any());
    }

    @Test
    void bindParts_deviceNotFound_stillBindsWithNullDeviceItem() {
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.set("DEVICE_CODE", "UNKNOWN");
        part.set("VIN", VIN);
        part.set("PART_NO", "PN001");
        part.set("PART_SN", "SN001");
        parts.add(part);

        when(vehicleNodeAppService.getVehicleNodeByCode("UNKNOWN")).thenReturn(null);

        List<VehicleEolPartBoundEvent.PartMeta> result = binder.bindParts(parts, VIN, BATCH_NUM);

        assertEquals(1, result.size());
        assertNull(result.get(0).getDeviceItem());
        verify(partInfoAppService).upsertPartInfo(any(PartInfo.class));
        verify(vehiclePartAppService).bindVehiclePart(any(VehiclePart.class));
    }

    @Test
    void bindParts_bindException_continuesWithNextPart() {
        JSONArray parts = new JSONArray();
        JSONObject part1 = new JSONObject();
        part1.set("DEVICE_CODE", "DEV1");
        part1.set("VIN", VIN);
        part1.set("PART_NO", "PN1");
        part1.set("PART_SN", "SN1");
        parts.add(part1);

        JSONObject part2 = new JSONObject();
        part2.set("DEVICE_CODE", "DEV2");
        part2.set("VIN", VIN);
        part2.set("PART_NO", "PN2");
        part2.set("PART_SN", "SN2");
        parts.add(part2);

        VehicleNode device1 = VehicleNode.builder().code("DEV1").deviceCategory("TBOX").build();
        VehicleNode device2 = VehicleNode.builder().code("DEV2").deviceCategory("CCP").build();
        when(vehicleNodeAppService.getVehicleNodeByCode("DEV1")).thenReturn(device1);
        when(vehicleNodeAppService.getVehicleNodeByCode("DEV2")).thenReturn(device2);
        doThrow(new RuntimeException("bind error")).when(vehiclePartAppService).bindVehiclePart(any());

        List<VehicleEolPartBoundEvent.PartMeta> result = binder.bindParts(parts, VIN, BATCH_NUM);

        // Both parts still added to meta list (add happens before bind attempt)
        assertEquals(2, result.size());
        verify(partInfoAppService, times(2)).upsertPartInfo(any(PartInfo.class));
        verify(vehiclePartAppService, times(2)).bindVehiclePart(any(VehiclePart.class));
    }
}
