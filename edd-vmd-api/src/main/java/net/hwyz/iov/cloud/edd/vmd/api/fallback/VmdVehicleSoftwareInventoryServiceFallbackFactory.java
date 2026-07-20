package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.request.SoftwareManifestRequest;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.SoftwareInventoryExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.SoftwareManifestApplyResponse;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleSoftwareInventoryService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 车辆软件实装清单服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdVehicleSoftwareInventoryServiceFallbackFactory implements FallbackFactory<VmdVehicleSoftwareInventoryService> {

    @Override
    public VmdVehicleSoftwareInventoryService create(Throwable throwable) {
        return new VmdVehicleSoftwareInventoryService() {
            @Override
            public List<SoftwareInventoryExResponse> getCurrentSoftwareInventory(String vin) {
                log.error("软件实装清单服务查询车辆[{}]当前软件清单调用失败", vin, throwable);
                throw new RuntimeException("软件实装清单服务查询车辆[" + vin + "]当前软件清单调用失败", throwable);
            }

            @Override
            public List<SoftwareInventoryExResponse> getSoftwareHistory(String vin, String vehicleNodeCode, String softwareTargetCode) {
                log.error("软件实装清单服务查询车辆[{}]软件历史调用失败, vehicleNodeCode={}, softwareTargetCode={}", vin, vehicleNodeCode, softwareTargetCode, throwable);
                throw new RuntimeException("软件实装清单服务查询车辆[" + vin + "]软件历史调用失败", throwable);
            }

            @Override
            public List<SoftwareInventoryExResponse> listCurrentSoftwareInventory(Long cursor, String updatedAfter, Integer size) {
                log.error("软件实装清单服务全量分页拉取软件清单调用失败, cursor={}, updatedAfter={}, size={}", cursor, updatedAfter, size, throwable);
                throw new RuntimeException("软件实装清单服务全量分页拉取软件清单调用失败", throwable);
            }

            @Override
            public SoftwareManifestApplyResponse applySoftwareManifest(SoftwareManifestRequest request) {
                log.error("软件实装清单服务写入软件实装清单调用失败, vin={}, source={}", request.getVin(), request.getSource(), throwable);
                throw new RuntimeException("软件实装清单服务写入软件实装清单调用失败", throwable);
            }
        };
    }
}
