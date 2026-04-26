package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehiclePartVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehiclePartAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆零件应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehiclePartAppService {

    private final VehiclePartRepository vehiclePartRepository;

    /**
     * 查询车辆零件信息
     *
     * @param vin       车架号
     * @param pn        零件号
     * @param beginTime 开始时间
     * @param endTime    结束时间
     * @return 车辆零件列表
     */
    public List<VehiclePartVo> search(String vin, String pn, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("pn", pn);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VehiclePart> vehiclePartList = vehiclePartRepository.selectByMap(map);
        return PageUtil.convert(vehiclePartList, VehiclePartAssembler.INSTANCE::fromDomain);
    }

    /**
     * 查询车辆零件信息
     *
     * @param vin       车架号
     * @param pn        零件号
     * @param sn        序列号
     * @param partState 零件状态
     * @param beginTime 开始时间
     * @param endTime    结束时间
     * @return 车辆零件列表
     */
    public List<VehiclePartVo> search(String vin, String pn, String sn, Integer partState, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("pn", pn);
        map.put("sn", sn);
        map.put("partState", partState);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VehiclePart> vehiclePartList = vehiclePartRepository.selectByMap(map);
        return PageUtil.convert(vehiclePartList, VehiclePartAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查零件号和序列号是否唯一
     *
     * @param id 主键ID
     * @param pn 零件号
     * @param sn 序列号
     * @return 结果
     */
    public Boolean checkPnAndSnUnique(Long id, String pn, String sn) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        VehiclePart vehiclePart = vehiclePartRepository.selectByPnAndSn(pn, sn);
        return !ObjUtil.isNotNull(vehiclePart) || vehiclePart.getId().longValue() == id.longValue();
    }

    /**
     * 根据主键ID获取车辆零件信息
     *
     * @param id 主键ID
     * @return 车辆零件信息
     */
    public VehiclePartVo getVehiclePartById(Long id) {
        return VehiclePartAssembler.INSTANCE.fromDomain(vehiclePartRepository.selectById(id));
    }

    /**
     * 创建车辆零件
     *
     * @param vehiclePartList 车辆零件列表
     * @return 结果
     */
    public int createVehiclePart(List<VehiclePart> vehiclePartList) {
        return vehiclePartRepository.batchInsert(vehiclePartList);
    }

    /**
     * 新增车辆零件
     *
     * @param vehiclePartVo 车辆零件信息
     * @param userId        操作用户ID
     * @return 结果
     */
    public int createVehiclePart(VehiclePartVo vehiclePartVo, String userId) {
        VehiclePart vehiclePart = VehiclePartAssembler.INSTANCE.toDomain(vehiclePartVo);
        return vehiclePartRepository.insert(vehiclePart);
    }

    /**
     * 修改车辆零件
     *
     * @param vehiclePartVo 车辆零件信息
     * @param userId        操作用户ID
     * @return 结果
     */
    public int modifyVehiclePart(VehiclePartVo vehiclePartVo, String userId) {
        VehiclePart vehiclePart = VehiclePartAssembler.INSTANCE.toDomain(vehiclePartVo);
        return vehiclePartRepository.update(vehiclePart);
    }

    /**
     * 批量删除车辆零件
     *
     * @param ids 车辆零件ID数组
     * @return 结果
     */
    public int deleteVehiclePartByIds(Long[] ids) {
        return vehiclePartRepository.batchPhysicalDelete(ids);
    }

    /**
     * 绑定车辆零件
     *
     * @param vehiclePart 车辆零件
     */
    public void bindVehiclePart(VehiclePart vehiclePart) {
        vehiclePart.setPartState(1); // 1-在用
        vehiclePart.setBindTime(Instant.now());
        vehiclePartRepository.insert(vehiclePart);
    }

}
