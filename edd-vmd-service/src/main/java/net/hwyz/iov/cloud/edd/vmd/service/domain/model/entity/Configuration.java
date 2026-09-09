package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;

@Slf4j
@Getter
@Setter
@SuperBuilder
public class Configuration implements DomainObj<Configuration> {

    private Long id;

    /**
     * 版本代码（CR-047：Configuration 唯一直接产品树父引用，层级沿产品树派生）
     */
    private String variantCode;

    /**
     * 配置编码（承接原 buildConfigCode 语义）
     */
    private String code;

    private String name;

    /**
     * 本地化名称（对齐 MDM nameLocal 契约，RD-047-6）
     */
    private String nameLocal;

    private String description;

    /**
     * 数据来源: MDM/MANUAL
     */
    private SourceType source;

    /**
     * MDM 侧实体主键 ID
     */
    private String externalRefId;

    /**
     * MDM 侧实体版本号
     */
    private Long externalVersion;

    /**
     * 最后一次同步时间
     */
    private java.time.LocalDateTime lastSyncTime;

}