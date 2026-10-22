package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.mpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningBlockingDiagnosisResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ProvisioningTerminationResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyProvisioningMaintenanceAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MPT 一键清理阻塞 Controller Web/MockMvc 契约测试（VMD-DSN-CR-065 §6）。
 * <p>
 * standalone MockMvc 校验 method/path、请求体→命令映射与响应 JSON 结构；
 * RBAC 权限（vmd:security:businessKey:provisioning:query/terminate）由 framework AOP 承载，不在本层验证。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
class MptBusinessKeyProvisioningControllerWebTest {

    private static final String BASE = "/api/mpt/business-key/provisioning";
    private static final String REQUEST_ID = "kp-cr065-0001";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private BusinessKeyProvisioningMaintenanceAppService maintenanceAppService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        maintenanceAppService = mock(BusinessKeyProvisioningMaintenanceAppService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MptBusinessKeyProvisioningController(maintenanceAppService)).build();
    }

    @Test
    @DisplayName("GET blocking-diagnosis 返回页面诊断字段")
    void blockingDiagnosis() throws Exception {
        when(maintenanceAppService.inspectTermination(REQUEST_ID))
                .thenReturn(ProvisioningBlockingDiagnosisResult.builder()
                        .requestId(REQUEST_ID)
                        .deviceSn("TBOX-1")
                        .businessDomain("V2C_COMM")
                        .status("PREPARING")
                        .clearable(true)
                        .certificateChanged(true)
                        .pendingDirectoryCount(2)
                        .uniqueConstraintOccupied(true)
                        .suggestedAction("CLEARABLE")
                        .operationSummary(ProvisioningBlockingDiagnosisResult.OperationSummary.builder()
                                .notStarted(2).confirmedFailed(0).succeeded(0).inProgressOrUnknown(0).build())
                        .blockingReasons(List.of())
                        .reopenAllowedAfterClear(true)
                        .build());

        mockMvc.perform(get(BASE + "/" + REQUEST_ID + "/blocking-diagnosis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.data.clearable").value(true))
                .andExpect(jsonPath("$.data.certificateChanged").value(true))
                .andExpect(jsonPath("$.data.pendingDirectoryCount").value(2))
                .andExpect(jsonPath("$.data.suggestedAction").value("CLEARABLE"))
                .andExpect(jsonPath("$.data.operationSummary.notStarted").value(2));
    }

    @Test
    @DisplayName("POST clear-blocking 映射请求体→命令并返回终止结果")
    void clearBlocking() throws Exception {
        LocalDateTime modifyTime = LocalDateTime.of(2026, 10, 22, 10, 30, 0);
        when(maintenanceAppService.terminateStale(any(), any()))
                .thenReturn(ProvisioningTerminationResult.builder()
                        .requestId(REQUEST_ID)
                        .beforeState("PREPARING")
                        .afterState("FAILED")
                        .changed(true)
                        .blockingCleared(true)
                        .reopenAllowed(true)
                        .failedPendingKeyCount(2)
                        .blockingReasons(List.of())
                        .operatedAt(LocalDateTime.now())
                        .build());

        String body = "{"
                + "\"maintenanceRequestId\":\"op-0001\","
                + "\"expectedState\":\"PREPARING\","
                + "\"expectedModifyTime\":\"2026-10-22T10:30:00\","
                + "\"reasonCode\":\"CERTIFICATE_CHANGED_DIGEST_CONFLICT\","
                + "\"reason\":\"证书已升级\","
                + "\"ticketNo\":\"T-001\""
                + "}";

        mockMvc.perform(post(BASE + "/" + REQUEST_ID + "/clear-blocking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.data.changed").value(true))
                .andExpect(jsonPath("$.data.blockingCleared").value(true))
                .andExpect(jsonPath("$.data.reopenAllowed").value(true))
                .andExpect(jsonPath("$.data.beforeState").value("PREPARING"))
                .andExpect(jsonPath("$.data.afterState").value("FAILED"))
                .andExpect(jsonPath("$.data.failedPendingKeyCount").value(2));

        verify(maintenanceAppService).terminateStale(any(), any());
    }
}
