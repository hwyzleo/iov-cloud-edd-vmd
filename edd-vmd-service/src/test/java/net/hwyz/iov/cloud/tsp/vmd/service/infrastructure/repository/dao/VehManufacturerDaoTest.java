package net.hwyz.iov.cloud.tsp.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehManufacturerDao;
import net.hwyz.iov.cloud.tsp.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehManufacturerPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 车辆生产厂商表 DAO 测试类
 *
 * @author hwyz_leo
 */
public class VehManufacturerDaoTest extends BaseTest {

    @Autowired
    private VehManufacturerDao vehManufacturerDao;

    @Test
    @Order(1)
    @DisplayName("新增一条记录")
    public void testInsertPo() throws Exception {
        VehManufacturerPo vehManufacturerPo = VehManufacturerPo.builder()
                .code("MANU001")
                .name("开源汽车制造厂有限公司")
                .nameEn("Open Source AutoMotor Manufacturing Co., Ltd.")
                .enable(true)
                .sort(0)
                .build();
        vehManufacturerDao.insertPo(vehManufacturerPo);
    }

}
