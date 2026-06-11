package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PartTypeTest {

    @Test
    void valOf_validValue_returnsEnum() {
        assertEquals(PartType.TBOX, PartType.valOf("TBOX"));
        assertEquals(PartType.BTM, PartType.valOf("BTM"));
        assertEquals(PartType.CCP, PartType.valOf("CCP"));
        assertEquals(PartType.IDCM, PartType.valOf("IDCM"));
        assertEquals(PartType.SIM, PartType.valOf("SIM"));
        assertEquals(PartType.OTHER, PartType.valOf("OTHER"));
    }

    @Test
    void valOf_invalidValue_returnsNull() {
        assertNull(PartType.valOf("INVALID"));
        assertNull(PartType.valOf(""));
        assertNull(PartType.valOf(null));
    }

    @Test
    void getValue_returnsCorrectValue() {
        assertEquals("TBOX", PartType.TBOX.getValue());
        assertEquals("BTM", PartType.BTM.getValue());
        assertEquals("CCP", PartType.CCP.getValue());
        assertEquals("IDCM", PartType.IDCM.getValue());
        assertEquals("SIM", PartType.SIM.getValue());
        assertEquals("OTHER", PartType.OTHER.getValue());
    }

    @Test
    void getLabel_returnsCorrectLabel() {
        assertEquals("车载终端", PartType.TBOX.getLabel());
        assertEquals("蓝牙模块", PartType.BTM.getLabel());
        assertEquals("域控制器", PartType.CCP.getLabel());
        assertEquals("智能驾驶控制", PartType.IDCM.getLabel());
        assertEquals("SIM卡", PartType.SIM.getLabel());
        assertEquals("其他", PartType.OTHER.getLabel());
    }
}
