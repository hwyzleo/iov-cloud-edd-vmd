package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ManufacturerVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ManufacturerAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBasicInfoMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehManufacturerMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehManufacturerPo;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆工厂应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManufacturerAppService {

    private final VehBasicInfoMapper vehBasicInfoMapper;
    private final VehManufacturerMapper vehManufacturerMapper;

    /**
     * 查询车辆工厂信息
     *
     * @param code      车辆工厂代码
     * @param name      车辆工厂名称
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆平台列表
     */
    public List<ManufacturerVo> search(String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VehManufacturerPo> vehManufacturerPoList = vehManufacturerMapper.selectPoByMap(map);
        return PageUtil.convert(vehManufacturerPoList, ManufacturerAssembler.INSTANCE::fromPo);
    }

    /**
     * 检查车辆工厂代码是否唯一
     *
     * @param manufacturerId 车辆工厂ID
     * @param code           车辆工厂代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long manufacturerId, String code) {
        if (ObjUtil.isNull(manufacturerId)) {
            manufacturerId = -1L;
        }
        VehManufacturerPo manufacturerPo = getManufacturerByCode(code);
        return !ObjUtil.isNotNull(manufacturerPo) || manufacturerPo.getId().longValue() == manufacturerId.longValue();
    }

    /**
     * 检查车辆工厂下是否存在车辆
     *
     * @param manufacturerId 车辆工厂ID
     * @return 结果
     */
    public Boolean checkManufacturerVehicleExist(Long manufacturerId) {
        VehManufacturerPo manufacturerPo = getManufacturerById(manufacturerId);
        Map<String, Object> map = new HashMap<>();
        map.put("manufacturerCode", manufacturerPo.getCode());
        return vehBasicInfoMapper.countPoByMap(map) > 0;
    }

    /**
     * 根据主键ID获取车辆工厂信息
     *
     * @param id 主键ID
     * @return 车辆工厂信息
     */
    public VehManufacturerPo getManufacturerById(Long id) {
        return vehManufacturerMapper.selectPoById(id);
    }

    /**
     * 根据车辆平台代码获取车辆平台信息
     *
     * @param code 车辆平台代码
     * @return 车辆平台信息
     */
    public VehManufacturerPo getManufacturerByCode(String code) {
        return vehManufacturerMapper.selectPoByCode(code);
    }

    /**
     * 新增车辆工厂
     *
     * @param manufacturer 车辆工厂信息
     * @return 结果
     */
    public int createManufacturer(VehManufacturerPo manufacturer) {
        return vehManufacturerMapper.insertPo(manufacturer);
    }

    /**
     * 修改车辆工厂
     *
     * @param manufacturer 车辆工厂信息
     * @return 结果
     */
    public int modifyManufacturer(VehManufacturerPo manufacturer) {
        return vehManufacturerMapper.updatePo(manufacturer);
    }

    /**
     * 批量删除车辆平台
     *
     * @param ids 车辆平台ID数组
     * @return 结果
     */
    public int deletePlatformByIds(Long[] ids) {
        return vehManufacturerMapper.batchPhysicalDeletePo(ids);
    }

}
