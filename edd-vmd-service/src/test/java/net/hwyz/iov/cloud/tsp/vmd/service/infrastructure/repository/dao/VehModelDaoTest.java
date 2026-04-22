package net.hwyz.iov.cloud.tsp.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehModelDao;
import net.hwyz.iov.cloud.tsp.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehModelDo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 车辆车型表 DAO 测试类
 *
 * @author hwyz_leo
 */
public class VehModelDaoTest extends BaseTest {

    @Autowired
    private VehModelDao vehModelDao;

    @Test
    @Order(1)
    @DisplayName("新增一条记录")
    public void testInsertPo() throws Exception {
        VmdVehModelDo vehModelPo = VmdVehModelDo.builder()
                .platformCode("H")
                .seriesCode("H01")
                .code("H0107")
                .name("寒01 7座")
                .nameEn("Han01")
                .enable(true)
                .sort(0)
                .build();
        vehModelDao.insertPo(vehModelPo);
    }

}
