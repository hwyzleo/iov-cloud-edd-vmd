package net.hwyz.iov.cloud.tsp.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBasicInfoDao;
import net.hwyz.iov.cloud.tsp.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 车辆基础信息表 DAO 测试类
 *
 * @author hwyz_leo
 */
public class VehBasicInfoDaoTest extends BaseTest {

    @Autowired
    private VehBasicInfoDao vehBasicInfoDao;

    @Test
    @Order(1)
    @DisplayName("新增一条记录")
    public void testInsertPo() throws Exception {
        VehBasicInfoPo vehBasicInfoPo = VehBasicInfoPo.builder()
                .vin("HWYZTEST000000001")
                .manufacturerCode("HWYZ")
                .brandCode("HWYZ")
                .platformCode("H")
                .seriesCode("H01")
                .modelCode("H0106")
                .buildConfigCode("H0106000111")
                .build();
        vehBasicInfoDao.insertPo(vehBasicInfoPo);
    }

}
