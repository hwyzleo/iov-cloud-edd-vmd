# CR-012: Brand 主数据重构为 MDM Brand 本地投影

> **Date**: 2026-06-05  
> **Status**: Approved  
> **Requirements**: `specs/vehicle-master-data-platform/requirements.md` (CR-012)  
> **Design**: `specs/vehicle-master-data-platform/design.md` (CR-012)

## 1. Overview

### 1.1 Objective

Transform Brand from local maintenance entity to MDM Brand read-only projection consumer, aligning with the "按需最小化投影" (minimal projection) principle.

### 1.2 Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **No new Flyway migration** | Reuse V3's `source`/`external_ref_id`/`external_version`/`last_sync_time` fields | Brand 无命名迁移驱动，与 CR-011 Plant 不同 |
| **No column rename** | `veh_brand.code` remains as `brandCode`关联键 | Requirements 明确「保留 brandCode，不改名、不删除」 |
| **Minimal projection** | VMD Brand ⊂ MDM Brand | 只同步 VMD 业务场景所需字段 |
| **Source-based access control** | `source=MDM` → read-only, `source=MANUAL` → legacy CRUD | 渐进收敛，兼容期保留 |

## 2. Architecture Changes

### 2.1 Component Overview

| Layer | Component | Change Type | Description |
|-------|-----------|-------------|-------------|
| Infrastructure | `VehBrandMapper.xml` | **Fix** | Add MDM fields to SQL mappings |
| Domain | `Brand.java` | **Enhance** | Add JavaDoc explaining projection semantics |
| Application | `BrandAppService` | **Enhance** | Add `@Deprecated` to legacy CRUD, update comments |
| Application | `MdmSyncAppService` | **Enhance** | Improve Brand sync logging |
| Adapter | `MptBrandController` | **Update** | Update API comments for "兼容期遗留" |
| API | `MdmBrandQueryClient` | **Keep** | Stub for future MDM integration |

### 2.2 Data Flow

```
MDM Brand Events (Kafka)
    ↓
MdmEventSubscribe
    ↓
MdmSyncAppService.handleBrandEvent()
    ↓
VehBrandRepository.insert/update (source=MDM)
    ↓
tb_veh_brand (本地投影)

MPT 后台操作
    ↓
MptBrandController
    ↓
BrandAppService.create/modify/delete
    ↓
Source check: if source=MDM → ProductDataReadOnlyException
    ↓
VehBrandRepository (only for source=MANUAL)
```

## 3. Data Model

### 3.1 Brand Entity Enhancement

**Current Fields**:
- `id`, `code`, `name`, `nameEn`, `enable`, `sort`
- `source`, `externalRefId`, `externalVersion`, `lastSyncTime` (V3 已添加)

**Proposed JavaDoc**:
```java
/**
 * 品牌实体 - MDM Brand 主数据在 VMD 的按需最小化只读投影
 * 
 * <p>Brand 主数据的权威来源（SSOT）为 edd-mdm，VMD 仅保留本地投影副本。
 * 本实体面向车辆主数据上下文（bounded context），用于车辆查询、详情展示、
 * 导入校验、产品树关联和历史追溯。</p>
 * 
 * <p>数据来源规则：</p>
 * <ul>
 *   <li>source=MDM：只读，禁止通过 MPT 后台修改/删除</li>
 *   <li>source=MANUAL：兼容期遗留数据，允许有限维护</li>
 * </ul>
 */
```

### 3.2 VehBrandPo Enhancement

**No schema changes needed** - V3 migration already added all required columns.

**Proposed JavaDoc**:
```java
/**
 * 品牌持久化对象 - 对应 tb_veh_brand 表
 * 
 * <p>该表同时承载 MDM Brand 投影数据（source=MDM）和历史手动维护数据（source=MANUAL）。
 * MDM 投影字段（source, external_ref_id, external_version, last_sync_time）
 * 由 MdmSyncAppService 在事件订阅和 Bootstrap 时写入。</p>
 */
```

### 3.3 Repository Method Updates

**Add to `VehBrandRepository` interface**:
```java
/**
 * 按 external_ref_id 查询品牌（MDM 同步专用）
 */
Brand selectByExternalRefId(String externalRefId);
```

## 4. MyBatis XML Fix

### 4.1 `baseColumnList` Update

**Current** (missing MDM fields):
```xml
<sql id="baseColumnList">
    id, code, name, name_en, enable, sort, description, create_by, create_time, modify_by, modify_time
</sql>
```

**Proposed**:
```xml
<sql id="baseColumnList">
    id, code, name, name_en, enable, sort, description, 
    source, external_ref_id, external_version, last_sync_time,
    create_by, create_time, modify_by, modify_time
</sql>
```

### 4.2 `resultMap` Update

**Current** (missing MDM field mappings):
```xml
<resultMap id="brandResultMap" type="VehBrandPo">
    <id property="id" column="id"/>
    <result property="code" column="code"/>
    <result property="name" column="name"/>
    <result property="nameEn" column="name_en"/>
    <result property="enable" column="enable"/>
    <result property="sort" column="sort"/>
</resultMap>
```

**Proposed**:
```xml
<resultMap id="brandResultMap" type="VehBrandPo">
    <id property="id" column="id"/>
    <result property="code" column="code"/>
    <result property="name" column="name"/>
    <result property="nameEn" column="name_en"/>
    <result property="enable" column="enable"/>
    <result property="sort" column="sort"/>
    <result property="source" column="source"/>
    <result property="externalRefId" column="external_ref_id"/>
    <result property="externalVersion" column="external_version"/>
    <result property="lastSyncTime" column="last_sync_time"/>
</resultMap>
```

### 4.3 `insertPo` Update

**Current** (missing MDM fields):
```xml
<insert id="insertPo" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO tb_veh_brand (code, name, name_en, enable, sort, description, create_by, create_time)
    VALUES (#{code}, #{name}, #{nameEn}, #{enable}, #{sort}, #{description}, #{createBy}, NOW())
</insert>
```

**Proposed**:
```xml
<insert id="insertPo" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO tb_veh_brand 
    (code, name, name_en, enable, sort, description, 
     source, external_ref_id, external_version, last_sync_time,
     create_by, create_time)
    VALUES 
    (#{code}, #{name}, #{nameEn}, #{enable}, #{sort}, #{description},
     #{source}, #{externalRefId}, #{externalVersion}, #{lastSyncTime},
     #{createBy}, NOW())
</insert>
```

### 4.4 `updatePo` Update

**Current** (missing MDM fields):
```xml
<update id="updatePo">
    UPDATE tb_veh_brand
    SET code = #{code}, name = #{name}, name_en = #{nameEn}, 
        enable = #{enable}, sort = #{sort}, description = #{description},
        modify_by = #{modifyBy}, modify_time = NOW()
    WHERE id = #{id}
</update>
```

**Proposed**:
```xml
<update id="updatePo">
    UPDATE tb_veh_brand
    SET code = #{code}, name = #{name}, name_en = #{nameEn}, 
        enable = #{enable}, sort = #{sort}, description = #{description},
        source = #{source}, external_ref_id = #{externalRefId}, 
        external_version = #{externalVersion}, last_sync_time = #{lastSyncTime},
        modify_by = #{modifyBy}, modify_time = NOW()
    WHERE id = #{id}
</update>
```

## 5. Application Layer Changes

### 5.1 BrandAppService Enhancement

**Add `@Deprecated` to Legacy CRUD Methods**:

```java
/**
 * 创建品牌（兼容期遗留能力）
 * 
 * <p>仅可作用于 source=MANUAL 过渡数据。对 source=MDM 记录，
 * 请通过 MDM 事件订阅或 Bootstrap 同步。</p>
 * 
 * @deprecated CR-012 后 Brand 定位为 MDM 只读投影，此方法仅保留兼容性。
 *             最终下线由后续兼容性清理 CR 完成。
 */
@Deprecated
public Long createBrand(BrandCmd cmd, Long userId) {
    // Existing implementation with MDM read-only guard
}

/**
 * 修改品牌（兼容期遗留能力）
 * 
 * @deprecated CR-012 后 Brand 定位为 MDM 只读投影，此方法仅保留兼容性。
 */
@Deprecated
public Boolean modifyBrand(BrandCmd cmd, Long userId) {
    // Existing implementation with MDM read-only guard
}

/**
 * 删除品牌（兼容期遗留能力）
 * 
 * @deprecated CR-012 后 Brand 定位为 MDM 只读投影，此方法仅保留兼容性。
 */
@Deprecated
public Boolean deleteBrandByIds(Long[] ids) {
    // Existing implementation with MDM read-only guard
}
```

**Enhance MDM Read-only Guard Comments**:

```java
// Source=MDM 只读限制（CR-012）
if (brand.getSource() == SourceType.MDM) {
    throw new ProductDataReadOnlyException("品牌", brand.getCode());
}
```

### 5.2 MdmSyncAppService Enhancement

**Improve Brand Sync Logging**:

```java
private void handleBrandEvent(MdmBrandEvent event) {
    log.debug("处理 MDM 品牌事件: eventType={}, entityId={}, code={}", 
        event.getEventType(), event.getEntityId(), event.getCode());
    
    Brand localBrand = brandRepository.selectByExternalRefId(event.getEntityId());
    
    if (localBrand == null) {
        // 新增 Brand 投影
        log.info("新增 MDM 品牌投影: code={}, name={}", event.getCode(), event.getName());
        Brand newBrand = createBrandFromEvent(event);
        brandRepository.insert(newBrand);
    } else if (event.getVersion() > localBrand.getExternalVersion()) {
        // 更新 Brand 投影（版本更高）
        log.info("更新 MDM 品牌投影: code={}, oldVersion={}, newVersion={}", 
            event.getCode(), localBrand.getExternalVersion(), event.getVersion());
        updateBrandFromEvent(localBrand, event);
        brandRepository.update(localBrand);
    } else {
        // 忽略乱序事件
        log.debug("忽略 MDM 品牌事件（版本不满足）: code={}, eventVersion={}, localVersion={}", 
            event.getCode(), event.getVersion(), localBrand.getExternalVersion());
    }
}
```

### 5.3 Bootstrap Stub Enhancement

**Update `bootstrapBrand()` with Clear TODO**:

```java
/**
 * Bootstrap 品牌数据从 MDM 全量同步
 * 
 * <p>当本地 source=MDM 的品牌记录数为 0 时，自动调用 MDM Brand 全量快照接口
 * 拉取数据并 upsert 本地副本。</p>
 * 
 * <p>TODO: MDM Brand 全量快照接口待就绪，当前为 stub 实现。
 * 接口路径和返回格式由「edd-mdm 接入规范」定义。</p>
 */
public void bootstrapBrand() {
    long mdmCount = brandRepository.countBySource(SourceType.MDM);
    if (mdmCount == 0) {
        log.info("本地无 MDM 品牌记录，启动 Bootstrap 同步");
        // TODO: 调用 MDM Brand 全量快照接口
        // List<Brand> mdmBrands = mdmBrandQueryClient.listAll();
        // for (Brand brand : mdmBrands) {
        //     brandRepository.upsertByExternalRefId(brand);
        // }
        log.warn("MDM Brand 全量快照接口待实现，Bootstrap 跳过");
    }
}
```

## 6. Adapter Layer Changes

### 6.1 MptBrandController Enhancement

**Update API Comments for "兼容期遗留" Semantics**:

```java
/**
 * 品牌管理控制器
 * 
 * <p>CR-012 语义重构：Brand 自 CR-012 起定位为 MDM Brand 主数据本地只读投影的消费方。</p>
 * <ul>
 *   <li>list/listAll/query/export：长期保留的查询能力</li>
 *   <li>add/edit/remove：兼容期遗留，仅可作用于 source=MANUAL 过渡数据，
 *       对 source=MDM 记录一律拒绝（ProductDataReadOnlyException）。
 *       最终下线由后续兼容性清理 CR 完成。</li>
 * </ul>
 * 
 * @see ProductDataReadOnlyException
 */
@RestController
@RequestMapping("/api/mpt/brand/v1")
public class MptBrandController {
    
    /**
     * 创建品牌（兼容期遗留）
     * 
     * <p>仅可作用于 source=MANUAL 过渡数据。对 source=MDM 记录抛出
     * ProductDataReadOnlyException（错误码 202014）。</p>
     * 
     * @deprecated CR-012 后 Brand 定位为 MDM 只读投影，此端点仅保留兼容性。
     */
    @Deprecated
    @PostMapping
    @Log(title = "品牌管理", businessType = BusinessType.INSERT)
    public ApiResponse<Long> add(@Validated @RequestBody BrandRequest request) {
        // Implementation
    }
    
    /**
     * 修改品牌（兼容期遗留）
     * 
     * @deprecated CR-012 后 Brand 定位为 MDM 只读投影，此端点仅保留兼容性。
     */
    @Deprecated
    @PutMapping
    @Log(title = "品牌管理", businessType = BusinessType.UPDATE)
    public ApiResponse<Boolean> edit(@Validated @RequestBody BrandRequest request) {
        // Implementation
    }
    
    /**
     * 删除品牌（兼容期遗留）
     * 
     * @deprecated CR-012 后 Brand 定位为 MDM 只读投影，此端点仅保留兼容性。
     */
    @Deprecated
    @DeleteMapping("/{brandIds}")
    @Log(title = "品牌管理", businessType = BusinessType.DELETE)
    public ApiResponse<Boolean> remove(@PathVariable Long[] brandIds) {
        // Implementation
    }
}
```

### 6.2 MptMdmSyncController Enhancement

**Update Bootstrap Endpoint Comments**:

```java
/**
 * MDM 数据同步控制器
 * 
 * <p>提供从 MDM 全量快照同步数据到 VMD 本地投影的能力。</p>
 * 
 * <p>支持实体：brand（品牌）、carLine（车系）、platform（平台）、plant（工厂）、all（全部）</p>
 */
@RestController
@RequestMapping("/api/mpt/mdmSync/v1")
public class MptMdmSyncController {
    
    /**
     * Bootstrap 全量同步
     * 
     * <p>从 MDM 拉取指定实体的全量快照并 upsert 本地投影副本。
     * 不删除本地已有记录，按 external_ref_id / external_version 幂等。</p>
     * 
     * @param entity 同步实体类型：brand|carLine|platform|plant|all
     */
    @PostMapping("/bootstrap")
    @Log(title = "MDM 同步", businessType = BusinessType.OTHER)
    public ApiResponse<String> bootstrap(@RequestParam String entity) {
        // Implementation
    }
}
```

## 7. Testing Strategy

### 7.1 Unit Tests

**BrandAppService Tests**:
- `createBrand_shouldRejectMdmSource()`
- `modifyBrand_shouldRejectMdmSource()`
- `deleteBrandByIds_shouldRejectMdmSource()`

**MdmSyncAppService Tests**:
- `handleBrandEvent_shouldCreateNewProjection()`
- `handleBrandEvent_shouldUpdateExistingProjection()`
- `handleBrandEvent_shouldIgnoreOlderVersion()`

**VehBrandMapper Tests**:
- `insertPo_shouldPersistMdmFields()`
- `updatePo_shouldPersistMdmFields()`

### 7.2 Integration Tests

**Bootstrap Integration Test**:
- Skip if MDM API is still stub
- Test full sync flow when MDM API is ready

## 8. Implementation Checklist

- [ ] Update `VehBrandMapper.xml` - Add MDM fields to SQL mappings
- [ ] Add JavaDoc to `Brand.java` entity
- [ ] Add JavaDoc to `VehBrandPo.java`
- [ ] Add `selectByExternalRefId()` to `VehBrandRepository` interface
- [ ] Implement `selectByExternalRefId()` in `VehBrandRepositoryImpl`
- [ ] Add `@Deprecated` to `BrandAppService` legacy CRUD methods
- [ ] Update MDM read-only guard comments in `BrandAppService`
- [ ] Enhance logging in `MdmSyncAppService.handleBrandEvent()`
- [ ] Update `bootstrapBrand()` with TODO stub
- [ ] Update `MptBrandController` API comments
- [ ] Update `MptMdmSyncController` API comments
- [ ] Write unit tests for BrandAppService
- [ ] Write unit tests for MdmSyncAppService
- [ ] Write unit tests for VehBrandMapper
- [ ] Verify compilation passes
- [ ] Run existing tests to ensure no regressions

## 9. References

- **Requirements**: `specs/vehicle-master-data-platform/requirements.md` (CR-012)
- **Design**: `specs/vehicle-master-data-platform/design.md` (CR-012)
- **Flyway Migration**: `V3__Add_mdm_source_to_product_tree.sql`
- **Exception**: `ProductDataReadOnlyException.java` (错误码 202014)
- **Enum**: `SourceType.java` (MDM / MANUAL)
