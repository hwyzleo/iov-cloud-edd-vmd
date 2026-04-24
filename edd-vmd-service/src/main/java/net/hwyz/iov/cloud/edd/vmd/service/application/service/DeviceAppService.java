package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.DeviceAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.DeviceMapper;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.DevicePo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备信息应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAppService {

    private final DeviceMapper deviceMapper;

    /**
     * 查询设备信息
     *
     * @param code       车载域代码
     * @param name       车载域名称
     * @param funcDomain 功能域
     * @param beginTime  开始时间
     * @param endTime    结束时间
     * @return 查询车载列表
     */
    public List<DeviceVo> search(String code, String name, String funcDomain, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("funcDomain", funcDomain);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<DevicePo> devicePoList = deviceMapper.selectPoByMap(map);
        return PageUtil.convert(devicePoList, DeviceAssembler.INSTANCE::fromPo);
    }

    /**
     * 获取所有设备信息
     *
     * @return 设备信息
     */
    public List<DevicePo> listAll() {
        return deviceMapper.selectAllPo();
    }

    /**
     * 获取所有FOTA升级设备
     *
     * @return 设备信息
     */
    public List<DevicePo> listAllFota() {
        return deviceMapper.selectAllFotaPo();
    }

    /**
     * 检查设备信息代码是否唯一
     *
     * @param deviceId 设备信息ID
     * @param code     设备信息代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long deviceId, String code) {
        if (ObjUtil.isNull(deviceId)) {
            deviceId = -1L;
        }
        DevicePo devicePo = getDeviceByCode(code);
        return !ObjUtil.isNotNull(devicePo) || devicePo.getId().longValue() == deviceId.longValue();
    }

    /**
     * 根据主键ID获取设备信息
     *
     * @param id 主键ID
     * @return 设备信息
     */
    public DevicePo getDeviceById(Long id) {
        return deviceMapper.selectPoById(id);
    }

    /**
     * 根据设备信息代码获取设备信息
     *
     * @param code 设备信息代码
     * @return 设备信息
     */
    public DevicePo getDeviceByCode(String code) {
        return deviceMapper.selectPoByCode(code);
    }

    /**
     * 新增设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    public int createDevice(DevicePo device) {
        return deviceMapper.insertPo(device);
    }

    /**
     * 修改设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    public int modifyDevice(DevicePo device) {
        return deviceMapper.updatePo(device);
    }

    /**
     * 批量删除设备信息
     *
     * @param ids 设备信息ID数组
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids) {
        return deviceMapper.batchPhysicalDeletePo(ids);
    }

}
