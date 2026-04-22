package net.hwyz.iov.cloud.edd.vmd.service.application;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehiclePartVo;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.VehiclePartState;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception.PartNotAllowBindException;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.*;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehiclePartDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehiclePartHistoryDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehiclePartHistoryDo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehiclePartDo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 车辆零件应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehiclePartAppService {

    private final VehiclePartDao vehiclePartDao;
    private final VehiclePartHistoryDao vehiclePartHistoryDao;

    /**
     * 查询车辆零件
     *
     * @param vin       车架号
     * @param pn        零件号
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆零件列表
     */
    public List<VehiclePartVo> search(String vin, String pn, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        map.put("pn", StringUtils.isBlank(pn) ? null : pn.trim() + "%");
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdVehiclePartDo> vmdVehiclePartDoList = vehiclePartDao.selectPoByMap(map);
        return PageUtil.convert(vmdVehiclePartDoList, VehiclePartMapper.INSTANCE::fromDo);
    }

    /**
     * 检查零件号与序列号是否唯一
     *
     * @param vehiclePartId 车辆零件ID
     * @param pn            零件号
     * @param sn            序列号
     * @return 结果
     */
    public Boolean checkPnAndSnUnique(Long vehiclePartId, String pn, String sn) {
        if (ObjUtil.isNull(vehiclePartId)) {
            vehiclePartId = -1L;
        }
        VmdVehiclePartDo vehiclePartPo = getVehiclePartByPnAndSn(pn, sn);
        return !ObjUtil.isNotNull(vehiclePartPo) || vehiclePartPo.getId().longValue() == vehiclePartId.longValue();
    }

    /**
     * 根据主键ID获取车辆零件
     *
     * @param id 主键ID
     * @return 车辆零件
     */
    public VmdVehiclePartDo getVehiclePartById(Long id) {
        return vehiclePartDao.selectPoById(id);
    }

    /**
     * 根据零件号与序列号获取车辆零件
     *
     * @param pn 零件号
     * @param sn 序列号
     * @return 车辆零件
     */
    public VmdVehiclePartDo getVehiclePartByPnAndSn(String pn, String sn) {
        return vehiclePartDao.selectPoByPnAndSn(pn, sn);
    }

    /**
     * 新增车辆零件
     *
     * @param vehiclePart 车辆零件
     * @return 影响行数
     */
    public int createVehiclePart(VmdVehiclePartDo vehiclePart) {
        if (StrUtil.isNotBlank(vehiclePart.getSn()) && StrUtil.isBlank(vehiclePart.getVin())) {
            VmdVehiclePartDo vehiclePartTmp = vehiclePartDao.selectPoByPnAndSn(vehiclePart.getPn(), vehiclePart.getSn());
            if (vehiclePartTmp == null) {
                vehiclePart.setPartState(VehiclePartState.UNBOUND.value);
                vehiclePartHistoryDao.insertPo(VehiclePartMapper.INSTANCE.toHistory(vehiclePart));
                return vehiclePartDao.insertPo(vehiclePart);
            } else {
                log.warn("车辆零件[{}]已存在[pn={},sn={}]", vehiclePartTmp.getDeviceCode(), vehiclePart.getPn(), vehiclePart.getSn());
            }
        }
        return 0;
    }

    /**
     * 新增车辆零件
     *
     * @param vehiclePartList 车辆零件列表
     * @return 影响行数
     */
    public int createVehiclePart(List<VmdVehiclePartDo> vehiclePartList) {
        List<VmdVehiclePartDo> newVehiclePartList = new ArrayList<>();
        for (VmdVehiclePartDo vehiclePart : vehiclePartList) {
            if (StrUtil.isNotBlank(vehiclePart.getSn()) && StrUtil.isBlank(vehiclePart.getVin())) {
                VmdVehiclePartDo vehiclePartTmp = vehiclePartDao.selectPoByPnAndSn(vehiclePart.getPn(), vehiclePart.getSn());
                if (vehiclePartTmp == null) {
                    vehiclePart.setPartState(VehiclePartState.UNBOUND.value);
                    newVehiclePartList.add(vehiclePart);
                } else {
                    log.warn("车辆零件[{}]已存在[pn={},sn={}]", vehiclePartTmp.getDeviceCode(), vehiclePart.getPn(), vehiclePart.getSn());
                }
            }
        }
        if (!newVehiclePartList.isEmpty()) {
            List<VmdVehiclePartHistoryDo> historyList = VehiclePartMapper.INSTANCE.toHistoryList(newVehiclePartList);
            vehiclePartHistoryDao.batchInsertPo(historyList);
            return vehiclePartDao.batchInsertPo(newVehiclePartList);
        }
        return 0;
    }

    /**
     * 修改车辆零件
     *
     * @param vehiclePart 车辆零件
     * @return 结果
     */
    public int modifyVehiclePart(VmdVehiclePartDo vehiclePart) {
        return vehiclePartDao.updatePo(vehiclePart);
    }

    /**
     * 绑定车辆零件
     *
     * @param vehiclePart 车辆零件
     */
    public void bindVehiclePart(VmdVehiclePartDo vehiclePart) {
        VmdVehiclePartDo vehiclePartOrigin = vehiclePartDao.selectPoByPnAndSn(vehiclePart.getPn(), vehiclePart.getSn());
        if (vehiclePartOrigin == null) {
            vehiclePart.setPartState(VehiclePartState.UNBOUND.value);
            vehiclePartHistoryDao.insertPo(VehiclePartMapper.INSTANCE.toHistory(vehiclePart));
            vehiclePartDao.insertPo(vehiclePart);
            vehiclePartOrigin = vehiclePart;
        }
        if (vehiclePartOrigin.getPartState() != VehiclePartState.UNBOUND.value) {
            throw new PartNotAllowBindException(vehiclePart.getPn(), vehiclePart.getSn(), vehiclePart.getPartState());
        }
        vehiclePartOrigin.setVin(vehiclePart.getVin());
        vehiclePartOrigin.setSupplierCode(vehiclePart.getSupplierCode());
        vehiclePartOrigin.setConfigWord(vehiclePart.getConfigWord());
        vehiclePartOrigin.setHardwareVer(vehiclePart.getHardwareVer());
        vehiclePartOrigin.setSoftwareVer(vehiclePart.getSoftwareVer());
        vehiclePartOrigin.setHardwarePn(vehiclePart.getHardwarePn());
        vehiclePartOrigin.setSoftwarePn(vehiclePart.getSoftwarePn());
        vehiclePartOrigin.setBindTime(new Date());
        vehiclePartOrigin.setBindBy(vehiclePart.getBindBy());
        vehiclePartOrigin.setBindOrg(vehiclePartOrigin.getBindOrg());
        vehiclePartOrigin.setPartState(VehiclePartState.IN_USE.value);
        vehiclePartDao.updatePo(vehiclePartOrigin);
        VmdVehiclePartHistoryDo history = VehiclePartMapper.INSTANCE.toHistory(vehiclePartOrigin);
        history.setId(null);
        vehiclePartHistoryDao.insertPo(history);
    }

    /**
     * 批量删除车辆零件
     *
     * @param ids 车辆零件ID数组
     * @return 结果
     */
    public int deleteVehiclePartByIds(Long[] ids) {
        return vehiclePartDao.batchPhysicalDeletePo(ids);
    }

}
