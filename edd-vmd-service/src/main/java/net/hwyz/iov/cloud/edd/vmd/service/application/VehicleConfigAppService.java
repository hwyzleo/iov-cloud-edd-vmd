package net.hwyz.iov.cloud.edd.vmd.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.VehicleConfigItemMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.VehicleConfigMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehicleConfigDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehicleConfigItemDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehicleConfigItemDo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehicleConfigDo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆配置应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleConfigAppService {

    private final VehicleConfigDao vehicleConfigDao;
    private final VehicleConfigItemDao vehicleConfigItemDao;

    /**
     * 查询车辆配置
     *
     * @param vin       车架号
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆零件列表
     */
    public List<VehicleConfigVo> search(String vin, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdVehicleConfigDo> vmdVehicleConfigDoList = vehicleConfigDao.selectPoByMap(map);
        return PageUtil.convert(vmdVehicleConfigDoList, VehicleConfigMapper.INSTANCE::fromDo);
    }

    /**
     * 查询车辆配置项
     *
     * @param vin       车架号
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆零件列表
     */
    public List<VehicleConfigItemVo> searchConfigItem(String vin, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdVehicleConfigItemDo> vmdVehicleConfigItemDoList = vehicleConfigItemDao.selectPoByMap(map);
        return PageUtil.convert(vmdVehicleConfigItemDoList, VehicleConfigItemMapper.INSTANCE::fromDo);
    }

    /**
     * 根据主键ID获取车辆配置
     *
     * @param id 主键ID
     * @return 车辆配置
     */
    public VmdVehicleConfigDo getVehicleConfigById(Long id) {
        return vehicleConfigDao.selectPoById(id);
    }

    /**
     * 根据主键ID获取车辆配置项
     *
     * @param id 主键ID
     * @return 车辆配置项
     */
    public VmdVehicleConfigItemDo getVehicleConfigItemById(String vin, Long id) {
        return vehicleConfigItemDao.selectPoById(id);
    }

}
