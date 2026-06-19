package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.LocalDateTime;

/**
 * 单车选项值快照实体
 * <p>
 * 物理车域 per-VIN 选项值快照表，与 vehicle_config 无关。
 * 快照值取自车辆生产数据报文，独立冻结生产时点取值。
 * </p>
 *
 * @author VMD-DSN-CR-030 / US-043
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VehicleOption implements DomainObj<VehicleOption> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车辆识别码
     */
    private String vin;

    /**
     * 选项族编码
     */
    private String optionFamilyCode;

    /**
     * 选项值编码
     */
    private String optionCode;

    /**
     * 数据来源
     */
    private String source;

    /**
     * 导入批次号
     */
    private String batchNum;

    /**
     * 快照时间
     */
    private LocalDateTime snapshotTime;

    /**
     * 创建副本
     *
     * @return 副本对象
     */
    public VehicleOption copy() {
        return VehicleOption.builder()
            .id(this.id)
            .vin(this.vin)
            .optionFamilyCode(this.optionFamilyCode)
            .optionCode(this.optionCode)
            .source(this.source)
            .batchNum(this.batchNum)
            .snapshotTime(this.snapshotTime)
            .build();
    }
}
