package net.hwyz.iov.cloud.edd.vmd.service.application.vid;

import cn.hutool.json.JSONObject;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.TspDownstreamProcessor;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.OtaDownstreamProcessor;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl.IdkDownstreamProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DownstreamProcessor单元测试
 *
 * @author hwyz_leo
 */
class DownstreamProcessorTest {

    @Test
    @DisplayName("TspDownstreamProcessor应返回正确的车载节点代码")
    void testTspDownstreamProcessorGetSupportedVehicleNodeCode() {
        TspDownstreamProcessor processor = new TspDownstreamProcessor(null, null, null);
        assertEquals("TSP", processor.getSupportedVehicleNodeCode());
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
