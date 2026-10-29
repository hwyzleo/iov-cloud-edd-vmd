package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartInfoPatchCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoMutationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleNodeAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 车辆零件绑定组件单元测试（CR-071 §4.2/§4.4 已绑同件补列/不同件拒绝）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@ExtendWith(MockitoExtension.class)
class VehiclePartBinderTest {

    @Mock
    private PartInfoAppService partInfoAppService;
    @Mock
    private VehiclePartAppService vehiclePartAppService;
    @Mock
    private VehicleNodeAppService vehicleNodeAppService;
    @Mock
    private PartInfoRepository partInfoRepository;
    @Mock
    private PartInfoMutationAppService partInfoMutationAppService;

    private VehiclePartBinder binder;

    @BeforeEach
    void setUp() {
        binder = new VehiclePartBinder(partInfoAppService, vehiclePartAppService,
                vehicleNodeAppService, partInfoRepository, partInfoMutationAppService);
    }

    private JSONArray parts(String sn, String pn) {
        JSONObject part = new JSONObject();
        part.set("VIN", "VIN001");
        part.set("DEVICE_CODE", "TBOX_5G");
        part.set("PART_NO", pn);
        part.set("PART_SN", sn);
        part.set("HARDWARE_PN", "HW1");
        part.set("HARDWARE_VERSION", "H1.0");
        JSONArray array = new JSONArray();
        array.add(part);
        return array;
    }

    @Test
    @DisplayName("已绑同件：经 mutation 补列（FILL_MISSING），不重复绑定")
    void alreadyBoundSamePart_patchColumnsWithoutRebind() {
        PartInfo partInfo = PartInfo.builder().id(1L).partCode("PN1").sn("SN1").build();
        VehiclePart binding = VehiclePart.builder().id(10L).partId(1L).bindState(1).build();
        when(vehiclePartAppService.findByVinAndPosition("VIN001", "TBOX_5G")).thenReturn(binding);
        when(partInfoRepository.selectByPartCodeAndSn("PN1", "SN1")).thenReturn(partInfo);
        when(partInfoMutationAppService.patch(any()))
                .thenReturn(new PartInfoMutationAppService.MutationResult("APPLIED", 2, java.util.Map.of("hardwareVer", "H1.0")));

        binder.bindParts(parts("SN1", "PN1"), "VIN001", "BATCH-1");

        ArgumentCaptor<PartInfoPatchCmd> captor = ArgumentCaptor.forClass(PartInfoPatchCmd.class);
        verify(partInfoMutationAppService).patch(captor.capture());
        assertThat(captor.getValue().getHardwareVer()).isEqualTo("H1.0");
        assertThat(captor.getValue().getRepairPolicy()).isEqualTo(PartInfoMutationAppService.POLICY_FILL_MISSING);
        // 不重复绑定
        verify(vehiclePartAppService, never()).bindVehiclePart(any());
    }

    @Test
    @DisplayName("已绑不同实例：拒绝补列/换绑")
    void alreadyBoundDifferentPart_reject() {
        VehiclePart binding = VehiclePart.builder().id(10L).partId(99L).bindState(1).build();
        when(vehiclePartAppService.findByVinAndPosition("VIN001", "TBOX_5G")).thenReturn(binding);
        when(partInfoRepository.selectByPartCodeAndSn("PN1", "SN1")).thenReturn(
                PartInfo.builder().id(1L).partCode("PN1").sn("SN1").build());

        binder.bindParts(parts("SN1", "PN1"), "VIN001", "BATCH-1");

        verify(partInfoMutationAppService, never()).patch(any());
        verify(vehiclePartAppService, never()).bindVehiclePart(any());
        verify(partInfoAppService, never()).upsertPartInfo(any());
    }

    @Test
    @DisplayName("未绑定：走 mutation create + 绑定（含来源通道）")
    void notBound_upsertAndBind() {
        when(vehiclePartAppService.findByVinAndPosition("VIN001", "TBOX_5G")).thenReturn(null);
        when(partInfoMutationAppService.create(any(), any())).thenAnswer(invocation -> {
            PartInfo partInfo = invocation.getArgument(0);
            partInfo.setId(1L);
            return new PartInfoMutationAppService.MutationResult("APPLIED", 1, java.util.Map.of());
        });

        binder.bindParts(parts("SN1", "PN1"), "VIN001", "BATCH-1");

        verify(partInfoMutationAppService).create(any(PartInfo.class), any());
        verify(vehiclePartAppService).bindVehiclePart(any());
    }

}
