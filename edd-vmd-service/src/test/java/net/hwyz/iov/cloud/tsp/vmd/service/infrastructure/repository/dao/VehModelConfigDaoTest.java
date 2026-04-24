package net.hwyz.iov.cloud.tsp.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBuildConfigDao;
import net.hwyz.iov.cloud.tsp.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBuildConfigPo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 车辆车型配置表 DAO 测试类
 *
 * @author hwyz_leo
 */
public class VehModelConfigDaoTest extends BaseTest {

    @Autowired
    private VehBuildConfigDao vehBuildConfigDao;

    @Test
    @Order(1)
    @DisplayName("新增一条记录")
    public void testInsertPo() throws Exception {
        VehBuildConfigPo vehBuildConfigPo = VehBuildConfigPo.builder()
                .platformCode("H")
                .seriesCode("H01")
                .modelCode("H0107")
                .code("H01070102040102")
                .name("寒01七座版 墨玉黑 珊瑚橙 21寸轮毂（四季胎）枪灰色 有备胎 高阶智驾")
                .exteriorCode("WS01")
                .interiorCode("NS02")
                .wheelCode("CL04")
                .spareTireCode("XZ01")
                .adasCode("XZ02")
                .enable(true)
                .sort(99)
                .build();
        vehBuildConfigDao.insertPo(vehBuildConfigPo);
    }

}
