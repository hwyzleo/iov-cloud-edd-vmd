package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 车辆生命周期节点时间线响应
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLifecycleNodeResponse {

    /**
     * 生命周期节点
     */
    private String node;

    /**
     * 触达时间
     */
    private Instant reachTime;

}
