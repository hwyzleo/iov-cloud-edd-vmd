package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ApplySoftwareManifestItemCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.VehicleSoftwareInventoryObservedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleSoftwareBindingResolver;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.SoftwareManifestItemInvalidException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InventoryObservedMapper 单元测试
 * <p>
 * VMD-DSN-CR-046: 事件 item → ApplySoftwareManifestItemCmd 字段映射
 *
 * @author hwyz_leo
 */
class InventoryObservedMapperTest {

    private final InventoryObservedMapper mapper = new InventoryObservedMapper();

    private VehicleSoftwareInventoryObservedEvent event() {
        return VehicleSoftwareInventoryObservedEvent.builder()
                .eventId("evt-001")
                .observationKey("obs-001")
                .vin("HWYZTEST000000001")
                .inventoryRevision("v1")
                .inventoryModel("MULTI_TARGET")
                .collectedAt(Instant.parse("2026-09-08T10:00:00Z"))
                .acceptedAt(Instant.parse("2026-09-08T10:01:00Z"))
                .canonicalizationVersion(1)
                .canonicalDigest("sha256-digest-001")
                .items(List.of())
                .build();
    }

    private VehicleSoftwareInventoryObservedEvent.Item item(String ecuId, String target, String version,
                                                            String slot, Boolean active, String digest) {
        return VehicleSoftwareInventoryObservedEvent.Item.builder()
                .ecuId(ecuId)
                .hardwarePartNumber("HW-01")
                .hwVersion("1.0")
                .softwareTargetCode(target)
                .softwarePartNumber("SW-PN-01")
                .swVersion(version)
                .slot(slot)
                .active(active)
                .digest(digest)
                .build();
    }

    @Test
    void toManifestItem_shouldMapAllFields() {
        // Given
        var e = event();
        var i = item("ECU_TBOX", "TBOX_APP", "1.2.0", "A", true, "abc123");
        var resolution = new VehicleSoftwareBindingResolver.BindingResolution(100L, 200L);

        // When
        ApplySoftwareManifestItemCmd cmd = mapper.toManifestItem(e, i, resolution);

        // Then
        assertEquals(100L, cmd.getBindingId());
        assertEquals(200L, cmd.getPartId());
        assertEquals("ECU_TBOX", cmd.getVehicleNodeCode());
        assertEquals("TBOX_APP", cmd.getSoftwareTargetCode());
        assertEquals("SW-PN-01", cmd.getSoftwarePartNo());
        assertEquals("1.2.0", cmd.getSoftwareVersion());
        assertEquals("A", cmd.getSlot());
        assertTrue(cmd.getIsActiveSlot());
        assertEquals("abc123", cmd.getDigest());
        assertEquals("INITIAL", cmd.getChangeType());
        assertEquals("obs-001", cmd.getObservationKey());
        assertEquals(1, cmd.getCanonicalizationVersion());
        assertEquals("sha256-digest-001", cmd.getCanonicalDigest());
    }

    @Test
    void toManifestItem_singleImageNoSlot_shouldDefaultActive() {
        // Given
        var e = event();
        var i = item("ECU_GW", "ECU_IMAGE", "1.0.0", null, null, null);
        var resolution = new VehicleSoftwareBindingResolver.BindingResolution(1L, 2L);

        // When
        ApplySoftwareManifestItemCmd cmd = mapper.toManifestItem(e, i, resolution);

        // Then
        assertNull(cmd.getSlot());
        assertTrue(cmd.getIsActiveSlot());
    }

    @Test
    void toManifestItem_multiSlotWithoutActive_shouldThrow() {
        // Given
        var e = event();
        var i = item("ECU_TBOX", "TBOX_APP", "1.2.0", "A", null, null);
        var resolution = new VehicleSoftwareBindingResolver.BindingResolution(1L, 2L);

        // When / Then
        assertThrows(SoftwareManifestItemInvalidException.class,
                () -> mapper.toManifestItem(e, i, resolution));
    }

    @Test
    void toManifestItem_missingRequiredFields_shouldThrow() {
        // Given
        var e = event();
        var i = item("", "", "", null, null, null);
        var resolution = new VehicleSoftwareBindingResolver.BindingResolution(1L, 2L);

        // When / Then
        assertThrows(SoftwareManifestItemInvalidException.class,
                () -> mapper.toManifestItem(e, i, resolution));
    }

    @Test
    void toManifestItem_standbySlot_shouldKeepFalse() {
        // Given
        var e = event();
        var i = item("ECU_TBOX", "TBOX_APP", "1.1.0", "B", false, "digest-b");
        var resolution = new VehicleSoftwareBindingResolver.BindingResolution(1L, 2L);

        // When
        ApplySoftwareManifestItemCmd cmd = mapper.toManifestItem(e, i, resolution);

        // Then
        assertEquals("B", cmd.getSlot());
        assertFalse(cmd.getIsActiveSlot());
    }
}
