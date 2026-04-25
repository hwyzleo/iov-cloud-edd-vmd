package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.util.Date;

/**
 * 配置项领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class ConfigItem extends BaseDo<Long> implements DomainObj<ConfigItem> {

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
     * 配置项大类
     */
    private String family;

    /**
     * 配置项编码
     */
    private String code;

    /**
     * 配置项名称
     */
    private String name;

    /**
     * 配置项类型
     */
    private String type;

    /**
     * 配置项单位
     */
    private String unit;

    /**
     * 是否车辆能力
     */
    private Boolean capability;

    /**
     * 端上是否展示
     */
    private Boolean display;

    /**
     * 端上是否缓存
     */
    private Boolean cache;

}
