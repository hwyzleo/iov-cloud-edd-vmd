package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import lombok.Data;

/**
 * 零件设备类别一致性差异投影（SEC-VIAM-DSN-CR-006 / D-VIAM-036）
 * <p>
 * 只读投影：`tb_part_info.device_category`（派生冗余列）与 `tb_mdm_vehicle_node.device_category`
 * （事实源）不一致的零件行，用于巡检展示与受控修复。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-24
 */
@Data
public class PartCategoryInconsistencyPo {

    /** 零件实例ID（tb_part_info.id） */
    private Long id;

    /** 零件编码 */
    private String partCode;

    /** 零件序列号 */
    private String sn;

    /** 车载节点代码 */
    private String vehicleNodeCode;

    /** 当前冗余列值 */
    private String currentCategory;

    /** MDM 节点期望值（事实源） */
    private String expectedCategory;

    /** 当前业务版本（乐观锁） */
    private Integer rowVersion;
}
