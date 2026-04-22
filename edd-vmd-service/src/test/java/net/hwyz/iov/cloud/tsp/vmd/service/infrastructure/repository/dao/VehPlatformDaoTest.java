package net.hwyz.iov.cloud.tsp.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehPlatformDao;
import net.hwyz.iov.cloud.tsp.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehPlatformDo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 车辆平台表 DAO 测试类
 *
 * @author hwyz_leo
 */
public class VehPlatformDaoTest extends BaseTest {

    @Autowired
    private VehPlatformDao vehPlatformDao;

    @Test
    @Order(1)
    @DisplayName("新增一条记录")
    public void testInsertPo() throws Exception {
        VmdVehPlatformDo vehPlatformPo = VmdVehPlatformDo.builder()
                .code("H")
                .name("H平台")
                .nameEn("H")
                .enable(true)
                .sort(0)
                .build();
        vehPlatformDao.insertPo(vehPlatformPo);
    }

}
