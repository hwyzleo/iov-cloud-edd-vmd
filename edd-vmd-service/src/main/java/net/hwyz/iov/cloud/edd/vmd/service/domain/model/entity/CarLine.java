package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 车系领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class CarLine implements DomainObj<CarLine> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 行版本号（持久化乐观锁版本，VMD-DSN-CR-075 新增）
     *
     * <p>由基础设施层在读取/插入时回填，更新入口必须携带可信原版本（CAS），
     * 成功后回传新版本（旧版本 + 1），冲突失败时丢弃或重读，不复用内存版本。</p>
     */
    private Integer rowVersion;

    /**
     * 品牌代码
     */
    private String brandCode;

    /**
     * 车系代码
     */
    private String code;

    /**
     * 车系名称
     */
    private String name;

    /**
     * 车系英文名称
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
