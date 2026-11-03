package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CarLineCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ModelCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmCarLineEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.CarLineAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.MdmSyncAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ModelAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VMD-DSN-CR-075 代表性兼容回归：CarLine / Model 实际业务写入口（真实 MySQL）
 *
 * <p>Brand 的业务测试不能代表另外两个入口——本测试覆盖 CarLineAppService.modifySeries、
 * MdmSyncAppService.handleSeriesEvent 与 ModelAppService.modifyModel 的真实 DB 路径（CAS 版本
 * 递增、externalVersion 门禁、MDM 来源拒绝）。</p>
 *
 * @author hwyz_leo
 */
class MdmCarLineModelCr075IntegrationTest extends BaseTest {

    @Autowired
    private CarLineAppService carLineAppService;

    @Autowired
    private ModelAppService modelAppService;

    @Autowired
    private MdmSyncAppService mdmSyncAppService;

    @Autowired
    private MdmCarLineRepository mdmCarLineRepository;

    @Autowired
    private MdmModelRepository mdmModelRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_mdm_car_line WHERE code LIKE 'TEST_CR075_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_model WHERE code LIKE 'TEST_CR075_%'");
    }

    @Test
    @DisplayName("CarLine 管理端修改入口：modifySeries CAS 版本递增（CR-075）")
    void carLineModifySeries_shouldCasUpdateWithVersionIncrement() {
        // Given：MANUAL 行
        CarLine carLine = CarLine.builder()
                .code("TEST_CR075_CARLINE")
                .name("手动车系")
                .brandCode("BRAND001")
                .enable(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();
        mdmCarLineRepository.insert(carLine);
        assertEquals(1, carLine.getRowVersion());

        // When：管理端修改入口（携带可信原版本）
        CarLineCmd cmd = CarLineCmd.builder()
                .id(carLine.getId())
                .code("TEST_CR075_CARLINE")
                .name("修改后车系")
                .build();
        int result = carLineAppService.modifySeries(cmd, "user1");

        // Then：CAS 成功、版本递增
        assertEquals(1, result);
        CarLine updated = mdmCarLineRepository.selectById(carLine.getId());
        assertEquals("修改后车系", updated.getName());
        assertEquals(2, updated.getRowVersion());
    }

    @Test
    @DisplayName("CarLine MdmSync 投影写入口：handleSeriesEvent 新增/更新/门禁（CR-075）")
    void carLineSyncWrite_shouldInsertAndUpdateWithVersionGate() {
        // 新增
        MdmCarLineEvent createEvent = new MdmCarLineEvent("CREATED", "mdm-cl-1", 1L,
                "TEST_CR075_CLSYNC", "MDM车系1", "BRAND001", LocalDateTime.now());
        mdmSyncAppService.handleSeriesEvent(createEvent);
        CarLine inserted = mdmCarLineRepository.selectByCode("TEST_CR075_CLSYNC");
        assertNotNull(inserted);
        assertEquals(1L, inserted.getExternalVersion());
        assertEquals(1, inserted.getRowVersion());

        // 更新（更高版本 → CAS 成功）
        MdmCarLineEvent updateEvent = new MdmCarLineEvent("UPDATED", "mdm-cl-1", 2L,
                "TEST_CR075_CLSYNC", "MDM车系1-新", "BRAND001", LocalDateTime.now());
        mdmSyncAppService.handleSeriesEvent(updateEvent);
        CarLine updated = mdmCarLineRepository.selectByCode("TEST_CR075_CLSYNC");
        assertEquals("MDM车系1-新", updated.getName());
        assertEquals(2L, updated.getExternalVersion());
        assertEquals(2, updated.getRowVersion());

        // 低版本门禁忽略
        MdmCarLineEvent staleEvent = new MdmCarLineEvent("UPDATED", "mdm-cl-1", 1L,
                "TEST_CR075_CLSYNC", "不应覆盖", "BRAND001", LocalDateTime.now());
        mdmSyncAppService.handleSeriesEvent(staleEvent);
        assertEquals("MDM车系1-新", mdmCarLineRepository.selectByCode("TEST_CR075_CLSYNC").getName());

        // 管理端修改 MDM 来源车系应拒绝
        CarLineCmd mdmCmd = CarLineCmd.builder()
                .id(inserted.getId())
                .code("TEST_CR075_CLSYNC")
                .name("不应成功")
                .build();
        assertThrows(ProductDataReadOnlyException.class, () ->
                carLineAppService.modifySeries(mdmCmd, "user1"));
    }

    @Test
    @DisplayName("Model 管理端修改入口：modifyModel CAS 版本递增 + MDM 拒绝（CR-075）")
    void modelModifyModel_shouldCasUpdateAndRejectMdmSource() {
        // Given：MANUAL 行（Model 投影写走 CR-048 ProjectionMapper，此处回归管理端修改入口）
        Model model = Model.builder()
                .code("TEST_CR075_MODEL")
                .name("手动车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .source(SourceType.MANUAL)
                .build();
        mdmModelRepository.insert(model);
        assertEquals(1, model.getRowVersion());

        // When：管理端修改（携带可信原版本）
        ModelCmd cmd = ModelCmd.builder()
                .id(model.getId())
                .code("TEST_CR075_MODEL")
                .name("修改后车型")
                .build();
        int result = modelAppService.modifyModel(cmd, "user1");

        // Then：CAS 成功、版本递增
        assertEquals(1, result);
        Model updated = mdmModelRepository.selectById(model.getId());
        assertEquals("修改后车型", updated.getName());
        assertEquals(2, updated.getRowVersion());

        // MDM 来源拒绝（制造 MDM 行）
        Model mdmModel = Model.builder()
                .code("TEST_CR075_MODEL_MDM")
                .name("MDM车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .source(SourceType.MDM)
                .externalRefId("mdm-model-1")
                .externalVersion(1L)
                .build();
        mdmModelRepository.insert(mdmModel);
        ModelCmd mdmCmd = ModelCmd.builder()
                .id(mdmModel.getId())
                .code("TEST_CR075_MODEL_MDM")
                .name("不应成功")
                .build();
        assertThrows(ProductDataReadOnlyException.class, () ->
                modelAppService.modifyModel(mdmCmd, "user1"));
    }
}
