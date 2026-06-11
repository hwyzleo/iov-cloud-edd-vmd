package net.hwyz.iov.cloud.edd.vmd.service.application.scheduler;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.MdmSyncMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * MdmPartSyncScheduler单元测试
 */
@ExtendWith(MockitoExtension.class)
class MdmPartSyncSchedulerTest {

    @Mock
    private MdmSyncAppService mdmSyncAppService;

    @Mock
    private MdmSyncMetrics mdmSyncMetrics;

    @InjectMocks
    private MdmPartSyncScheduler scheduler;

    @Test
    void testSyncPartData_Success() {
        // When
        scheduler.syncPartData();

        // Then
        verify(mdmSyncAppService).bootstrapPart();
        verify(mdmSyncMetrics).recordSuccess();
        verify(mdmSyncMetrics).recordDuration(anyLong());
        verify(mdmSyncMetrics, never()).recordFailure();
    }

    @Test
    void testSyncPartData_Failure() {
        // Given
        doThrow(new RuntimeException("Sync error")).when(mdmSyncAppService).bootstrapPart();

        // When
        scheduler.syncPartData();

        // Then
        verify(mdmSyncMetrics).recordFailure();
        verify(mdmSyncMetrics).recordDuration(anyLong());
        verify(mdmSyncMetrics, never()).recordSuccess();
    }

    @Test
    void testManualSync() {
        // When
        scheduler.manualSync();

        // Then
        verify(mdmSyncAppService).bootstrapPart();
        verify(mdmSyncMetrics).recordSuccess();
    }
}
