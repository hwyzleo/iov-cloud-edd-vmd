package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivationRejectedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDerivedProviderUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyKmsUnavailableException;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyMaterialModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BusinessKeyMaterialRouter 单元测试（VMD-DSN-CR-061 RD-061-3/RD-061-10）。
 * <p>显式路由、禁猜测、禁隐式回退。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@ExtendWith(MockitoExtension.class)
class BusinessKeyMaterialRouterTest {

    @Mock
    private ObjectProvider<KeyProvisioningTemplate> derivedRootProvider;
    @Mock
    private ObjectProvider<BusinessKeyMaterialTemplate> randomMaterialProvider;

    private BusinessKeyMaterialRouter router;

    private KeyProvisioningTemplate derivedTemplate;
    private BusinessKeyMaterialTemplate randomTemplate;

    @BeforeEach
    void setUp() {
        router = new BusinessKeyMaterialRouter(derivedRootProvider, randomMaterialProvider);
        derivedTemplate = mock(KeyProvisioningTemplate.class);
        randomTemplate = mock(BusinessKeyMaterialTemplate.class);
    }

    /** framework 当前无任何真实 RANDOM_MATERIAL 条目，且 {@link BizType} 为 final 枚举不可 mock；直接以材料模型驱动路由核心。 */
    private KeyMaterialModel randomMaterialModel() {
        return KeyMaterialModel.RANDOM_MATERIAL;
    }

    @Test
    @DisplayName("materialModel 原样透传；null 不被猜测")
    void materialModel() {
        assertEquals(KeyMaterialModel.DERIVED_ROOT, router.materialModel(BizType.V2C_COMM_ROOT));
        assertNull(router.materialModel(BizType.IDK_KTS_SIGNING));
        assertThrows(NullPointerException.class, () -> router.materialModel(null));
    }

    @Test
    @DisplayName("DERIVED_ROOT → requireDerivedRoot 返回门面；RANDOM_MATERIAL / null 一律拒绝")
    void requireDerivedRoot_explicitRouting() {
        when(derivedRootProvider.getIfAvailable()).thenReturn(derivedTemplate);
        assertSame(derivedTemplate, router.requireDerivedRoot(BizType.V2C_COMM_ROOT));
        assertSame(derivedTemplate, router.requireDerivedRoot(BizType.TBOX_DEVICE_ROOT));

        assertThrows(BusinessKeyDerivationRejectedException.class,
                () -> router.requireDerivedRoot(randomMaterialModel(), false, "MOCK_RANDOM_MATERIAL"));
        // IDK_KTS_SIGNING materialModel=null（不适用）→ 两条路径均拒绝
        assertThrows(BusinessKeyDerivationRejectedException.class,
                () -> router.requireDerivedRoot(BizType.IDK_KTS_SIGNING));
    }

    @Test
    @DisplayName("DERIVED_ROOT 但 framework 门面未装配 → Provider 未就绪（不隐式回退）")
    void requireDerivedRoot_unassembled() {
        when(derivedRootProvider.getIfAvailable()).thenReturn(null);
        assertThrows(BusinessKeyDerivedProviderUnavailableException.class,
                () -> router.requireDerivedRoot(BizType.V2C_COMM_ROOT));
    }

    @Test
    @DisplayName("RANDOM_MATERIAL → requireRandomMaterial 返回门面；DERIVED_ROOT / null 一律拒绝")
    void requireRandomMaterial_explicitRouting() {
        when(randomMaterialProvider.getIfAvailable()).thenReturn(randomTemplate);
        assertSame(randomTemplate, router.requireRandomMaterial(randomMaterialModel(), true, "MOCK_RANDOM_MATERIAL"));

        assertThrows(BusinessKeyDerivationRejectedException.class,
                () -> router.requireRandomMaterial(BizType.V2C_COMM_ROOT));
        assertThrows(BusinessKeyDerivationRejectedException.class,
                () -> router.requireRandomMaterial(BizType.IDK_KTS_SIGNING));
    }

    @Test
    @DisplayName("RANDOM_MATERIAL 但 framework 门面未装配 → KMS 不可用")
    void requireRandomMaterial_unassembled() {
        when(randomMaterialProvider.getIfAvailable()).thenReturn(null);
        assertThrows(BusinessKeyKmsUnavailableException.class,
                () -> router.requireRandomMaterial(randomMaterialModel(), true, "MOCK_RANDOM_MATERIAL"));
    }

    @Test
    @DisplayName("isDerivedRoot / isRandomMaterial 与可用性诊断")
    void predicatesAndAvailability() {
        assertTrue(router.isDerivedRoot(BizType.V2C_COMM_ROOT));
        assertFalse(router.isDerivedRoot(BizType.IDK_KTS_SIGNING));
        assertFalse(router.isDerivedRoot(null));
        // framework 无 RANDOM_MATERIAL 条目：真实 BizType 均非随机材料型
        assertFalse(router.isRandomMaterial(BizType.V2C_COMM_ROOT));
        assertFalse(router.isRandomMaterial(BizType.IDK_KTS_SIGNING));

        when(derivedRootProvider.getIfAvailable()).thenReturn(derivedTemplate);
        assertTrue(router.derivedRootAvailable());
        when(randomMaterialProvider.getIfAvailable()).thenReturn(null);
        assertFalse(router.randomMaterialAvailable());
    }
}
