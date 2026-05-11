# Series-Brand Model-Platform Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Change Series entity from platformCode to brandCode, making Model's platformCode an independent input parameter.

**Architecture:** Replace platformCode with brandCode across Series layer (entity, PO, DTO, request/response, mapper). Model layer unchanged since platformCode already exists.

**Tech Stack:** Java 17, Spring Boot, MyBatis, MapStruct, Lombok

---

## File Structure

**Series Layer (10 files):**
- Domain Entity: `Series.java` - core domain object
- Persistence PO: `VehSeriesPo.java` - database mapping
- Application DTOs: `SeriesCmd.java`, `SeriesDto.java`, `SeriesQuery.java` - data transfer
- Web VOs: `SeriesRequest.java`, `SeriesResponse.java` - API layer
- Service: `SeriesAppService.java` - business logic
- Controller: `MptSeriesController.java` - API endpoints
- Mapper: `VehSeriesMapper.xml` - SQL mapping

---

### Task 1: Update Series Domain Entity

**Files:**
- Modify: `edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/domain/model/entity/Series.java`

- [ ] **Step 1: Replace platformCode with brandCode in Series.java**

```java
package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.Instant;

@Slf4j
@Getter
@Setter
@SuperBuilder
public class Series implements DomainObj<Series> {

    private Long id;

    private String brandCode;

    private String code;

    private String name;

    private String nameEn;

    private Boolean enable;

    private Integer sort;

}
```

- [ ] **Step 2: Verify no compilation errors**

Run: `mvn compile -pl edd-vmd-service -am`
Expected: Series.java compiles (may have errors in dependent files)

- [ ] **Step 3: Commit**

```bash
git add edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/domain/model/entity/Series.java
git commit -m "refactor(series): change platformCode to brandCode in entity"
```

---

### Task 2: Update Series Persistence PO

**Files:**
- Modify: `edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/infrastructure/persistence/po/VehSeriesPo.java`

- [ ] **Step 1: Replace platformCode with brandCode in VehSeriesPo.java**

```java
package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.experimental.SuperBuilder;
import lombok.*;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_series")
public class VehSeriesPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("brand_code")
    private String brandCode;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("name_en")
    private String nameEn;

    @TableField("enable")
    private Boolean enable;

    @TableField("sort")
    private Integer sort;
}
```

- [ ] **Step 2: Commit**

```bash
git add edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/infrastructure/persistence/po/VehSeriesPo.java
git commit -m "refactor(series): change platformCode to brandCode in PO"
```

---

### Task 3: Update Series DTOs (Cmd, Dto, Query)

**Files:**
- Modify: `edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/dto/cmd/SeriesCmd.java`
- Modify: `edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/dto/result/SeriesDto.java`
- Modify: `edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/dto/query/SeriesQuery.java`

- [ ] **Step 1: Update SeriesCmd.java**

```java
package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesCmd {

    private Long id;
    private String brandCode;
    private String code;
    private String name;
    private String nameEn;
    private Boolean enable;
    private Integer sort;
    private String description;

}
```

- [ ] **Step 2: Update SeriesDto.java**

```java
package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesDto {

    private Long id;
    private String brandCode;
    private String code;
    private String name;
    private String nameEn;
    private Boolean enable;
    private Integer sort;
    private String description;

}
```

- [ ] **Step 3: Update SeriesQuery.java**

```java
package net.hwyz.iov.cloud.edd.vmd.service.application.dto.query;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class SeriesQuery {

    private String brandCode;
    private String code;
    private String name;
    private Date beginTime;
    private Date endTime;

}
```

- [ ] **Step 4: Commit DTO changes**

```bash
git add edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/dto/cmd/SeriesCmd.java
git add edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/dto/result/SeriesDto.java
git add edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/dto/query/SeriesQuery.java
git commit -m "refactor(series): change platformCode to brandCode in DTOs"
```

---

### Task 4: Update Series Web VOs (Request, Response)

**Files:**
- Modify: `edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/adapter/web/vo/request/SeriesRequest.java`
- Modify: `edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/adapter/web/vo/response/SeriesResponse.java`

- [ ] **Step 1: Update SeriesRequest.java**

```java
package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SeriesRequest extends BaseRequest {

    private Long id;

    private String brandCode;

    private String code;

    private String name;

    private String nameEn;

    private Boolean enable;

    private Integer sort;

    private String description;

    private Date createTime;

}
```

- [ ] **Step 2: Update SeriesResponse.java**

```java
package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesResponse {

    private Long id;

    private String brandCode;

    private String code;

    private String name;

    private String nameEn;

    private Boolean enable;

    private Integer sort;

    private String description;

    private Date createTime;

}
```

- [ ] **Step 3: Commit VO changes**

```bash
git add edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/adapter/web/vo/request/SeriesRequest.java
git add edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/adapter/web/vo/response/SeriesResponse.java
git commit -m "refactor(series): change platformCode to brandCode in VOs"
```

---

### Task 5: Update Series Mapper XML

**Files:**
- Modify: `edd-vmd-service/src/main/resources/mappers/VehSeriesMapper.xml`

- [ ] **Step 1: Replace all platform_code with brand_code in VehSeriesMapper.xml**

Key changes:
- Line 15: `platform_code` → `brand_code`
- Line 25: `platform_code` → `brand_code`
- Lines 32, 36, 44, 50: `platform_code` → `brand_code`, `#{platformCode}` → `#{brandCode}`
- Lines 122-123, 167-168, 200-201: condition `platform_code` → `brand_code`

Full updated file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehSeriesMapper">

    <resultMap id="baseResultMap" type="net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSeriesPo">
        <id column="id" property="id" />
        <result column="description" property="description" />
        <result column="create_time" property="createTime" />
        <result column="create_by" property="createBy" />
        <result column="modify_time" property="modifyTime" />
        <result column="modify_by" property="modifyBy" />
        <result column="row_version" property="rowVersion" />
        <result column="row_valid" property="rowValid" />
        <result column="brand_code" property="brandCode" />
        <result column="code" property="code" />
        <result column="name" property="name" />
        <result column="name_en" property="nameEn" />
        <result column="enable" property="enable" />
        <result column="sort" property="sort" />
    </resultMap>

    <sql id="baseColumnList">
        id, brand_code, code, name, name_en, enable, sort, 
        description, create_time, create_by, modify_time, modify_by, row_version, row_valid
    </sql>

    <insert id="insertPo" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO tb_veh_series (
            id, brand_code, code, name, name_en, enable, sort, 
            description, create_time, create_by, modify_time, modify_by, row_version, row_valid
        )
        VALUES (
            #{id}, #{brandCode}, #{code}, #{name}, #{nameEn}, #{enable}, #{sort}, 
            #{description}, now(), #{createBy}, now(), #{modifyBy}, 1, 1
        )
    </insert>

    <insert id="batchInsertPo" useGeneratedKeys="true" keyProperty="id" parameterType="java.util.List">
        INSERT INTO tb_veh_series (
            id, brand_code, code, name, name_en, enable, sort, 
            description, create_time, create_by, modify_time, modify_by, row_version, row_valid
        )
        VALUES
        <foreach collection="list" item="item" index="index" separator=",">
        (
            #{item.id}, #{item.brandCode}, #{item.code}, #{item.name}, #{item.nameEn}, #{item.enable}, #{item.sort}, 
            #{item.description}, now(), #{item.createBy}, now(), #{item.modifyBy}, 1, 1
        )
        </foreach>
    </insert>

    <!-- Keep update section unchanged (no brandCode update condition needed) -->
    <update id="updatePo" parameterType="net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSeriesPo">
        UPDATE tb_veh_series t SET
        <if test="name != null and name != ''">
            t.name = #{name},
        </if>
        <if test="nameEn != null and nameEn != ''">
            t.name_en = #{nameEn},
        </if>
        <if test="enable != null">
            t.enable = #{enable},
        </if>
        <if test="sort != null">
            t.sort = #{sort},
        </if>
        <if test="description != null and description != ''">
            t.description = #{description},
        </if>
        <if test="modifyBy != null">
            t.modify_by = #{modifyBy},
        </if>
        t.row_version = t.row_version + 1,
        t.modify_time = now()
        WHERE id = #{id}
    </update>

    <!-- Update selectByExample: platformCode → brandCode -->
    <select id="selectPoByExample" parameterType="net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSeriesPo" resultMap="baseResultMap">
        SELECT <include refid="baseColumnList"/>
        FROM tb_veh_series t
        WHERE t.row_valid = 1
        <if test="id != null">
            AND t.id = #{id}
        </if>
        <if test="brandCode != null and brandCode != ''">
            AND t.brand_code = #{brandCode}
        </if>
        <if test="code != null and code != ''">
            AND t.code = #{code}
        </if>
        <if test="name != null and name != ''">
            AND t.name = #{name}
        </if>
        <if test="nameEn != null and nameEn != ''">
            AND t.name_en = #{nameEn}
        </if>
        <if test="enable != null">
            AND t.enable = #{enable}
        </if>
        <if test="sort != null">
            AND t.sort = #{sort}
        </if>
        <if test="description != null and description != ''">
            AND t.description = #{description}
        </if>
        <if test="createTime != null">
            AND t.create_time = #{createTime}
        </if>
        <if test="createBy != null and createBy != ''">
            AND t.create_by = #{createBy}
        </if>
        <if test="modifyTime != null">
            AND t.modify_time = #{modifyTime}
        </if>
        <if test="modifyBy != null and modifyBy != ''">
            AND t.modify_by = #{modifyBy}
        </if>
        <if test="rowVersion != null">
            AND t.row_version = #{rowVersion}
        </if>
        ORDER BY t.id DESC
    </select>

    <!-- Update selectPoByMap: platformCode → brandCode -->
    <select id="selectPoByMap" parameterType="java.util.Map" resultMap="baseResultMap">
        SELECT <include refid="baseColumnList"/>
        FROM tb_veh_series t
        WHERE t.row_valid = 1
        <if test="brandCode != null and brandCode != ''">
            AND t.brand_code = #{brandCode}
        </if>
        <if test="code != null and code != ''">
            AND t.code = #{code}
        </if>
        <if test="name != null and name != ''">
            AND (t.name LIKE #{name} OR t.name_en LIKE #{name})
        </if>
        <if test="beginTime != null">
            and date_format(t.create_time,'%Y%m%d') &gt;= date_format(#{beginTime},'%Y%m%d')
        </if>
        <if test="endTime != null">
            and date_format(t.create_time,'%Y%m%d') &lt;= date_format(#{endTime},'%Y%m%d')
        </if>
        ORDER BY t.sort ASC, t.id DESC
    </select>

    <!-- Update countPoByMap: platformCode → brandCode -->
    <select id="countPoByMap" parameterType="java.util.Map" resultType="int">
        SELECT COUNT(*)
        FROM tb_veh_series t
        WHERE t.row_valid = 1
        <if test="brandCode != null and brandCode != ''">
            AND t.brand_code = #{brandCode}
        </if>
    </select>
</mapper>
```

- [ ] **Step 2: Commit mapper changes**

```bash
git add edd-vmd-service/src/main/resources/mappers/VehSeriesMapper.xml
git commit -m "refactor(series): change platform_code to brand_code in mapper"
```

---

### Task 6: Update SeriesAppService

**Files:**
- Modify: `edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/service/SeriesAppService.java`

- [ ] **Step 1: Update query map key in SeriesAppService.java (line 44)**

Change line 44 from:
```java
map.put("platformCode", query.getPlatformCode());
```
to:
```java
map.put("brandCode", query.getBrandCode());
```

- [ ] **Step 2: Commit**

```bash
git add edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/application/service/SeriesAppService.java
git commit -m "refactor(series): update query filter from platformCode to brandCode"
```

---

### Task 7: Update MptSeriesController

**Files:**
- Modify: `edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/adapter/web/controller/mpt/MptSeriesController.java`

- [ ] **Step 1: Update list endpoint query builder (line 51)**

Change:
```java
.platformCode(series.getPlatformCode())
```
to:
```java
.brandCode(series.getBrandCode())
```

- [ ] **Step 2: Rename and update listByPlatformCode endpoint (lines 68-76)**

Replace the `listByPlatformCode` method with:
```java
/**
 * 获取指定品牌下的所有车系
 *
 * @param brandCode 品牌代码
 * @return 车系信息列表
 */
@RequiresPermissions("completeVehicle:product:series:list")
@GetMapping(value = "/listByBrandCode")
public ApiResponse<List<SeriesResponse>> listByBrandCode(@RequestParam String brandCode) {
    log.info("管理后台用户[{}]获取指定品牌[{}]下的所有车系", SecurityContextHolder.getUserName(), brandCode);
    SeriesQuery query = SeriesQuery.builder()
            .brandCode(brandCode)
            .build();
    List<SeriesDto> seriesDtoList = seriesAppService.search(query);
    return ApiResponse.ok(MptSeriesAssembler.INSTANCE.fromDtoList(seriesDtoList));
}
```

- [ ] **Step 3: Commit**

```bash
git add edd-vmd-service/src/main/java/net/hwyz/iov/cloud/edd/vmd/service/adapter/web/controller/mpt/MptSeriesController.java
git commit -m "refactor(series): update controller endpoints for brandCode"
```

---

### Task 8: Compile and Verify

- [ ] **Step 1: Compile the entire project**

Run: `mvn clean compile -pl edd-vmd-service -am`
Expected: BUILD SUCCESS, no compilation errors

- [ ] **Step 2: Fix any compilation errors if found**

If MapStruct generated classes have errors, run: `mvn clean compile` to regenerate.

- [ ] **Step 3: Commit if any fixes were needed**

```bash
git status
# If changes, commit: git commit -m "fix: resolve compilation errors after Series refactor"
```

---

### Task 9: Database Migration (Manual)

**Note:** User handles data migration manually. This task documents the SQL for reference.

- [ ] **Step 1: Add brand_code column (SQL reference)**

```sql
ALTER TABLE tb_veh_series ADD COLUMN brand_code VARCHAR(32) COMMENT '品牌代码';
CREATE INDEX idx_series_brand_code ON tb_veh_series(brand_code);
```

- [ ] **Step 2: Drop platform_code column (after user populates brand_code)**

```sql
ALTER TABLE tb_veh_series DROP COLUMN platform_code;
```

---

### Task 10: Final Verification

- [ ] **Step 1: Run full project compile**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: Verify no errors**

Check for any remaining `platformCode` references in Series-related files.

- [ ] **Step 3: Final commit if needed**

```bash
git status
git log --oneline -10
```

---

## Summary

Total files modified: 10
- Series.java (entity)
- VehSeriesPo.java (PO)
- SeriesCmd.java (DTO)
- SeriesDto.java (DTO)
- SeriesQuery.java (query)
- SeriesRequest.java (VO)
- SeriesResponse.java (VO)
- SeriesAppService.java (service)
- MptSeriesController.java (controller)
- VehSeriesMapper.xml (mapper)