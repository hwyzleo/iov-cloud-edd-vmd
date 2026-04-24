package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehiclePartVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehiclePartAssembler;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehiclePartState;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.PartNotAllowBindException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehiclePartMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehiclePartHistoryMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartHistoryPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
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

    private final VehiclePartMapper vehiclePartMapper;
    private final VehiclePartHistoryMapper vehiclePartHistoryMapper;

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
        List<VehiclePartPo> vehiclePartPoList = vehiclePartMapper.selectPoByMap(map);
        return PageUtil.convert(vehiclePartPoList, VehiclePartAssembler.INSTANCE::fromPo);
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
        VehiclePartPo vehiclePartPo = getVehiclePartByPnAndSn(pn, sn);
        return !ObjUtil.isNotNull(vehiclePartPo) || vehiclePartPo.getId().longValue() == vehiclePartId.longValue();
    }

    /**
     * 根据主键ID获取车辆零件
     *
     * @param id 主键ID
     * @return 车辆零件
     */
    public VehiclePartPo getVehiclePartById(Long id) {
        return vehiclePartMapper.selectPoById(id);
    }

    /**
     * 根据零件号与序列号获取车辆零件
     *
     * @param pn 零件号
     * @param sn 序列号
     * @return 车辆零件
     */
    public VehiclePartPo getVehiclePartByPnAndSn(String pn, String sn) {
        return vehiclePartMapper.selectPoByPnAndSn(pn, sn);
    }

    /**
     * 新增车辆零件
     *
     * @param vehiclePart 车辆零件
     * @return 影响行数
     */
    public int createVehiclePart(VehiclePartPo vehiclePart) {
        if (StrUtil.isNotBlank(vehiclePart.getSn()) && StrUtil.isBlank(vehiclePart.getVin())) {
            VehiclePartPo vehiclePartTmp = vehiclePartMapper.selectPoByPnAndSn(vehiclePart.getPn(), vehiclePart.getSn());
            if (vehiclePartTmp == null) {
                vehiclePart.setPartState(VehiclePartState.UNBOUND.value);
                vehiclePartHistoryMapper.insertPo(VehiclePartAssembler.INSTANCE.toHistory(vehiclePart));
                return vehiclePartMapper.insertPo(vehiclePart);
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
    public int createVehiclePart(List<VehiclePartPo> vehiclePartList) {
        List<VehiclePartPo> newVehiclePartList = new ArrayList<>();
        for (VehiclePartPo vehiclePart : vehiclePartList) {
            if (StrUtil.isNotBlank(vehiclePart.getSn()) && StrUtil.isBlank(vehiclePart.getVin())) {
                VehiclePartPo vehiclePartTmp = vehiclePartMapper.selectPoByPnAndSn(vehiclePart.getPn(), vehiclePart.getSn());
                if (vehiclePartTmp == null) {
                    vehiclePart.setPartState(VehiclePartState.UNBOUND.value);
                    newVehiclePartList.add(vehiclePart);
                } else {
                    log.warn("车辆零件[{}]已存在[pn={},sn={}]", vehiclePartTmp.getDeviceCode(), vehiclePart.getPn(), vehiclePart.getSn());
                }
            }
        }
        if (!newVehiclePartList.isEmpty()) {
            List<VehiclePartHistoryPo> historyList = VehiclePartAssembler.INSTANCE.toHistoryList(newVehiclePartList);
            vehiclePartHistoryMapper.batchInsertPo(historyList);
            return vehiclePartMapper.batchInsertPo(newVehiclePartList);
        }
        return 0;
    }

    /**
     * 修改车辆零件
     *
     * @param vehiclePart 车辆零件
     * @return 结果
     */
    public int modifyVehiclePart(VehiclePartPo vehiclePart) {
        return vehiclePartMapper.updatePo(vehiclePart);
    }

    /**
     * 绑定车辆零件
     *
     * @param vehiclePart 车辆零件
     */
    public void bindVehiclePart(VehiclePartPo vehiclePart) {
        VehiclePartPo vehiclePartOrigin = vehiclePartMapper.selectPoByPnAndSn(vehiclePart.getPn(), vehiclePart.getSn());
        if (vehiclePartOrigin == null) {
            vehiclePart.setPartState(VehiclePartState.UNBOUND.value);
            vehiclePartHistoryMapper.insertPo(VehiclePartAssembler.INSTANCE.toHistory(vehiclePart));
            vehiclePartMapper.insertPo(vehiclePart);
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
        vehiclePartMapper.updatePo(vehiclePartOrigin);
        VehiclePartHistoryPo history = VehiclePartAssembler.INSTANCE.toHistory(vehiclePartOrigin);
        history.setId(null);
        vehiclePartHistoryMapper.insertPo(history);
    }

    /**
     * 批量删除车辆零件
     *
     * @param ids 车辆零件ID数组
     * @return 结果
     */
    public int deleteVehiclePartByIds(Long[] ids) {
        return vehiclePartMapper.batchPhysicalDeletePo(ids);
    }

}
