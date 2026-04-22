package net.hwyz.iov.cloud.tsp.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehSeriesDao;
import net.hwyz.iov.cloud.tsp.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehSeriesDo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 车辆车系表 DAO 测试类
 *
 * @author hwyz_leo
 */
public class VehSeriesDaoTest extends BaseTest {

    @Autowired
    private VehSeriesDao vehSeriesDao;

    @Test
    @Order(1)
    @DisplayName("新增一条记录")
    public void testInsertPo() throws Exception {
        VmdVehSeriesDo vehSeriesPo = VmdVehSeriesDo.builder()
                .platformCode("H")
                .code("H01")
                .name("寒01")
                .nameEn("Han01")
                .enable(true)
                .sort(0)
                .build();
        vehSeriesDao.insertPo(vehSeriesPo);
    }

}
