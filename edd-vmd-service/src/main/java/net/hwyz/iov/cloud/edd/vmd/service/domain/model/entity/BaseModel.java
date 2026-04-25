package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.util.Date;

/**
 * 基础车型领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class BaseModel extends BaseDo<Long> implements DomainObj<BaseModel> {

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
     * 平台代码
     */
    private String platformCode;

    /**
     * 车系代码
     */
    private String seriesCode;

    /**
     * 车型代码
     */
    private String modelCode;

    /**
     * 基础车型代码
     */
    private String code;

    /**
     * 基础车型名称
     */
    private String name;

    /**
     * 基础车型英文名称
     */
    private String nameEn;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

}
