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
 * 平台领域对象 - MDM Platform 主数据在 VMD 的按需最小化只读投影
 * 
 * <p>Platform 主数据的权威来源（SSOT）为 edd-mdm，VMD 仅保留本地投影副本。
 * 本实体面向车辆主数据上下文（bounded context），用于车辆查询、详情展示、
 * 导入校验、产品树关联和历史追溯。</p>
 * 
 * <p>与 Brand（CR-012）完全同构、区别于 Plant（CR-011）的命名迁移：
 * 平台实体命名不变、platformCode 关联键不变，不涉及表/列重命名，
 * 直接复用 CR-010/V3 已建的 source/external_ref_id/external_version/last_sync_time 字段。</p>
 * 
 * <p>数据来源规则：</p>
 * <ul>
 *   <li>source=MDM：只读，禁止通过 MPT 后台修改/删除</li>
 *   <li>source=MANUAL：兼容期遗留数据，允许有限维护</li>
 * </ul>
 * 
 * <p>字段范围原则（VMD Platform ⊂ MDM Platform）：</p>
 * <ul>
 *   <li>VMD 只保留支撑车辆主数据业务闭环所需的 Platform 字段</li>
 *   <li>VMD 不复制 MDM Platform 的完整治理模型、审批字段、生命周期状态等</li>
 *   <li>MDM Platform 字段变化时，只有影响 VMD 业务时才需同步调整</li>
 * </ul>
 * 
 * @see SourceType
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class Platform implements DomainObj<Platform> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 平台代码（platformCode 关联键）
     * 
     * <p>作为车辆主档（veh_basic_info.platform_code）与产品树
     * （veh_model.platform_code / veh_base_model.platform_code）的平台关联编码
     * 长期保留，不因维护权迁移而改名或删除。</p>
     */
    private String code;

    /**
     * 平台名称
     */
    private String name;

    /**
     * 平台英文名称
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
     * 
     * <p>source=MDM 的记录禁止通过 MPT 后台修改/删除，
     * 违规操作将抛出 ProductDataReadOnlyException（错误码 202014）。</p>
     */
    private SourceType source;

    /**
     * MDM侧实体主键ID
     * 
     * <p>用于 MDM 事件订阅时的幂等 upsert 逻辑。
     * source=MANUAL 时为 NULL。</p>
     */
    private String externalRefId;

    /**
     * MDM侧实体版本号
     * 
     * <p>用于乱序事件处理：event.version > local.external_version 时才更新。
     * source=MANUAL 时为 NULL。</p>
     */
    private Long externalVersion;

    /**
     * 最后一次同步时间
     * 
     * <p>记录 MDM 事件订阅或 Bootstrap 同步的最后时间。
     * source=MANUAL 时为 NULL。</p>
     */
    private LocalDateTime lastSyncTime;

}
