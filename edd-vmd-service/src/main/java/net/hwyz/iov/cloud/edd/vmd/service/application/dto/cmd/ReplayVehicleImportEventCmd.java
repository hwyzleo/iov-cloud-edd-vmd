package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车辆导入事件补发命令
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展为按 ImportType 路由的动作补偿
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplayVehicleImportEventCmd {

    /**
     * 请求ID（可选，作为 replayId，未提供时由服务端生成）
     */
    private String requestId;

    /**
     * 补发原因
     */
    private String reason;

    /**
     * 动作类型子集（空表示执行该导入类型的全部默认动作；前端只能提交预检返回的可选动作）
     */
    private List<String> actionTypes;
}
