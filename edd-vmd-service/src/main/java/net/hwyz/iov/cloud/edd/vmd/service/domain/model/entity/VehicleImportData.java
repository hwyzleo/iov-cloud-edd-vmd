package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.util.Date;

/**
 * 车辆导入数据领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehicleImportData extends BaseDo<Long> implements DomainObj<VehicleImportData> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 备注
     */
    private String description;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改者
     */
    private String modifyBy;

    /**
     * 修改时间
     */
    private Date modifyTime;

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
