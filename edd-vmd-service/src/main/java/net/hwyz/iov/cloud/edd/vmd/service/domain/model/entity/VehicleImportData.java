package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.Instant;

/**
 * 车辆导入数据领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehicleImportData implements DomainObj<VehicleImportData> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 批次号
     */
    private String batchNum;

    /**
     * 数据类型
     */
    private String type;

    /**
     * 数据版本
     */
    private String version;

    /**
     * 车辆导入数据
     */
    private String data;

    /**
     * 是否处理
     */
    private Boolean handle;

}
