package net.hwyz.iov.cloud.edd.vmd.service.application.vid;

import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.Tbox5gDownstreamProcessor;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.OtaDownstreamProcessor;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.IdkDownstreamProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DownstreamProcessor单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class DownstreamProcessorTest {

    @Test
    @DisplayName("Tbox5gDownstreamProcessor应返回正确的车载节点代码")
    void testTbox5gDownstreamProcessorGetSupportedVehicleNodeCode() {
        // Tbox5gDownstreamProcessor继承BaseProcessor，需要使用mock
        Tbox5gDownstreamProcessor processor = new Tbox5gDownstreamProcessor(null);
        assertEquals("TBOX_5G", processor.getSupportedVehicleNodeCode());
    }

    @Test
    @DisplayName("OtaDownstreamProcessor应返回正确的车载节点代码")
    void testOtaDownstreamProcessorGetSupportedVehicleNodeCode() {
        OtaDownstreamProcessor processor = new OtaDownstreamProcessor(null);
        assertEquals("OTA", processor.getSupportedVehicleNodeCode());
    }

    @Test
    @DisplayName("IdkDownstreamProcessor应返回正确的车载节点代码")
    void testIdkDownstreamProcessorGetSupportedVehicleNodeCode() {
        IdkDownstreamProcessor processor = new IdkDownstreamProcessor();
        assertEquals("IDK", processor.getSupportedVehicleNodeCode());
    }
}
