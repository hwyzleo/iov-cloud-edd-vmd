package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InboundSourceTypeTest {

    @Test
    void valOf_validValue_returnsEnum() {
        assertEquals(InboundSourceType.MES, InboundSourceType.valOf("MES"));
        assertEquals(InboundSourceType.MANUAL, InboundSourceType.valOf("MANUAL"));
        assertEquals(InboundSourceType.WMS, InboundSourceType.valOf("WMS"));
        assertEquals(InboundSourceType.IQC, InboundSourceType.valOf("IQC"));
        assertEquals(InboundSourceType.OTHER, InboundSourceType.valOf("OTHER"));
    }

    @Test
    void valOf_invalidValue_returnsNull() {
        assertNull(InboundSourceType.valOf("INVALID"));
        assertNull(InboundSourceType.valOf(""));
        assertNull(InboundSourceType.valOf(null));
    }

    @Test
    void getValue_returnsCorrectValue() {
        assertEquals("MES", InboundSourceType.MES.getValue());
        assertEquals("MANUAL", InboundSourceType.MANUAL.getValue());
        assertEquals("WMS", InboundSourceType.WMS.getValue());
        assertEquals("IQC", InboundSourceType.IQC.getValue());
        assertEquals("OTHER", InboundSourceType.OTHER.getValue());
    }

    @Test
    void getLabel_returnsCorrectLabel() {
        assertEquals("制造执行系统", InboundSourceType.MES.getLabel());
        assertEquals("手动导入", InboundSourceType.MANUAL.getLabel());
        assertEquals("仓储管理系统", InboundSourceType.WMS.getLabel());
        assertEquals("来料检验", InboundSourceType.IQC.getLabel());
        assertEquals("其他", InboundSourceType.OTHER.getLabel());
    }
}
