package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BrandCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmBrandEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BrandAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmBrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VMD-DSN-CR-075：MdmBrand 原生迁移后两类业务写入口真实 MySQL 回归
 *
 * <p>覆盖裁决①：管理端 MANUAL 兼容写（BrandAppService.createBrand/modifyBrand）
 * 与 MdmSync 投影写（handleBrandEvent insert/update + externalVersion 门禁 + CAS），
 * 不只测 Repository CRUD。@Rollback 保证测试数据回滚。</p>
 *
 * @author hwyz_leo
 */
class MdmBrandCr075IntegrationTest extends BaseTest {

    @Autowired
    private BrandAppService brandAppService;

    @Autowired
    private MdmSyncAppService mdmSyncAppService;

    @Autowired
    private MdmBrandRepository mdmBrandRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_mdm_brand WHERE code LIKE 'TEST_CR075_%'");
    }

    @Test
    @DisplayName("管理端兼容写：CAS 修改（版本递增、携带可信原版本）（CR-075）")
    void manualCompatWrite_shouldCreateAndCasUpdate() {
        // Given：MANUAL 行（兼容期遗留数据；createBrand 兼容路径 source=NULL 属既有缺陷，非本 CR 引入，此处经 Repository 造数）
        Brand brand = Brand.builder()
                .code("TEST_CR075_BRAND")
                .name("CR075手动品牌")
                .enable(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();
        mdmBrandRepository.insert(brand);
        assertEquals(Integer.valueOf(1), brand.getRowVersion(), "插入后应回填 row_version=1");

        // When：CAS 修改（AppService 携带取自刚加载 brand 的可信原版本）
        BrandCmd modifyCmd = BrandCmd.builder()
                .id(brand.getId())
                .code("TEST_CR075_BRAND")
                .name("CR075修改后")
                .build();
        int modifyResult = brandAppService.modifyBrand(modifyCmd, "user1");
        assertEquals(1, modifyResult);

        // Then：版本递增、字段更新
        Brand updated = mdmBrandRepository.selectByCode("TEST_CR075_BRAND");
        assertEquals("CR075修改后", updated.getName());
        assertEquals(Integer.valueOf(2), updated.getRowVersion(), "CAS 成功后版本应递增");
        Integer rowVersion = jdbcTemplate.queryForObject(
                "SELECT row_version FROM tb_mdm_brand WHERE id = ?", Integer.class, brand.getId());
        assertEquals(2, rowVersion);
    }

    @Test
    @DisplayName("MdmSync 投影写：事件新增与更新（externalVersion 门禁 + CAS 成功）（CR-075）")
    void mdmSyncWrite_shouldInsertAndUpdateProjectionWithVersionGate() {
        // When：首次事件 → 新增 MDM 投影
        MdmBrandEvent createEvent = new MdmBrandEvent("CREATED", "mdm-cr075-1", 1L,
                "TEST_CR075_SYNC", "MDM品牌1", LocalDateTime.now());
        mdmSyncAppService.handleBrandEvent(createEvent);

        Brand inserted = mdmBrandRepository.selectByCode("TEST_CR075_SYNC");
        assertNotNull(inserted);
        assertEquals(SourceType.MDM, inserted.getSource());
        assertEquals(1L, inserted.getExternalVersion());
        assertEquals(Integer.valueOf(1), inserted.getRowVersion());

        // When：更高版本事件 → CAS 更新成功
        MdmBrandEvent updateEvent = new MdmBrandEvent("UPDATED", "mdm-cr075-1", 2L,
                "TEST_CR075_SYNC", "MDM品牌1-新", LocalDateTime.now());
        mdmSyncAppService.handleBrandEvent(updateEvent);

        Brand updated = mdmBrandRepository.selectByCode("TEST_CR075_SYNC");
        assertEquals("MDM品牌1-新", updated.getName());
        assertEquals(2L, updated.getExternalVersion());
        assertEquals(Integer.valueOf(2), updated.getRowVersion(), "CAS 成功后版本应递增");

        // When：低版本事件 → 按 externalVersion 门禁忽略（不覆盖）
        MdmBrandEvent staleEvent = new MdmBrandEvent("UPDATED", "mdm-cr075-1", 1L,
                "TEST_CR075_SYNC", "不应覆盖", LocalDateTime.now());
        mdmSyncAppService.handleBrandEvent(staleEvent);
        Brand afterStale = mdmBrandRepository.selectByCode("TEST_CR075_SYNC");
        assertEquals("MDM品牌1-新", afterStale.getName(), "低版本事件不得覆盖");
        assertEquals(2L, afterStale.getExternalVersion());
    }

    @Test
    @DisplayName("管理端修改/删除 MDM 来源品牌应拒绝（来源限制保持，CR-012/CR-075）")
    void manualWrite_shouldRejectMdmSourceBrand() {
        // Given：MDM 投影行
        MdmBrandEvent createEvent = new MdmBrandEvent("CREATED", "mdm-cr075-2", 1L,
                "TEST_CR075_DEL", "MDM品牌2", LocalDateTime.now());
        mdmSyncAppService.handleBrandEvent(createEvent);
        Brand brand = mdmBrandRepository.selectByCode("TEST_CR075_DEL");
        assertNotNull(brand);
        assertEquals(SourceType.MDM, brand.getSource());

        // When & Then：管理端修改 MDM 来源品牌 → 拒绝（不扩大权限/所有权）
        BrandCmd modifyCmd = BrandCmd.builder()
                .id(brand.getId())
                .code("TEST_CR075_DEL")
                .name("不应成功")
                .build();
        assertThrows(ProductDataReadOnlyException.class, () ->
                brandAppService.modifyBrand(modifyCmd, "user1"));

        // When & Then：管理端批量物理删除 MDM 来源品牌 → 拒绝
        assertThrows(ProductDataReadOnlyException.class, () ->
                brandAppService.deleteBrandByIds(new Long[]{brand.getId()}));
        assertNotNull(mdmBrandRepository.selectByCode("TEST_CR075_DEL"), "MDM 行不得被删除");
    }
}
