package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

/**
 * 车辆导入事件补发请求
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReplayVehicleImportEventRequest extends BaseRequest {

    /**
     * 补发原因
     */
    private String reason;

    /**
     * 请求ID（可选，作为replayId，未提供时由服务端生成）
     */
    private String requestId;
}
