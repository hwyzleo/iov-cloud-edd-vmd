package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import java.time.LocalDateTime;

/**
 * 车载节点领域对象（原Device，CR-020重命名）
 *
 * <p>车载节点字典/类型层主数据，来自edd-mdm EEAD子域。
 * 仅处理「车载节点字典/类型层」主数据（节点定义、类型、功能域等「车上应有什么」），
 * 物理设备实例+绑定关系仍为VMD自有事务/实例数据，不上移、不投影化。</p>
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class VehicleNode implements DomainObj<VehicleNode> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车载节点代码（原deviceCode，CR-020重命名）
     */
    private String code;

    /**
     * 车载节点名称
     */
    private String name;

    /**
     * 车载节点本地化名称
     */
    private String nameLocal;

    /**
     * 设备分类
     */
    private String deviceCategory;

    /**
     * 功能域
     */
    private String funcDomain;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * OTA支持类型
     */
    private String otaSupport;

    /**
     * 是否核心设备
     */
    private Boolean core;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 数据来源：MDM=来自MDM系统，MANUAL=本地手动维护
     */
    private SourceType source;

    /**
     * MDM侧实体主键ID
     */
    private String externalRefId;

    /**
     * MDM侧实体版本号
     */
    private Long externalVersion;

    /**
     * 最后一次同步时间
     */
    private LocalDateTime lastSyncTime;

}
