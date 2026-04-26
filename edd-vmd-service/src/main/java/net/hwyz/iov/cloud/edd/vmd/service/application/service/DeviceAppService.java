package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.DeviceAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Device;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAppService {

    private final DeviceRepository deviceRepository;

    /**
     * 查询设备信息
     *
     * @param code       设备编码
     * @param name       设备名称
     * @param funcDomain 功能域
     * @param beginTime  开始时间
     * @param endTime    结束时间
     * @return 设备列表
     */
    public List<DeviceVo> search(String code, String name, String funcDomain, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("funcDomain", funcDomain);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<Device> deviceList = deviceRepository.selectByMap(map);
        return PageUtil.convert(deviceList, DeviceAssembler.INSTANCE::fromDomain);
    }

    /**
     * 获取所有设备
     *
     * @return 设备列表
     */
    public List<DeviceVo> listAll() {
        List<Device> deviceList = deviceRepository.selectByMap(new HashMap<>());
        return DeviceAssembler.INSTANCE.fromDomainList(deviceList);
    }

    /**
     * 检查设备编码是否唯一
     *
     * @param deviceId 设备ID
     * @param code     设备编码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long deviceId, String code) {
        if (ObjUtil.isNull(deviceId)) {
            deviceId = -1L;
        }
        Device device = deviceRepository.selectByCode(code);
        return !ObjUtil.isNotNull(device) || device.getId().longValue() == deviceId.longValue();
    }

    /**
     * 根据主键ID获取设备信息
     *
     * @param id 主键ID
     * @return 设备信息
     */
    public DeviceVo getDeviceById(Long id) {
        return DeviceAssembler.INSTANCE.fromDomain(deviceRepository.selectById(id));
    }

    /**
     * 根据设备编码获取设备信息
     *
     * @param code 设备编码
     * @return 设备领域对象
     */
    public Device getDeviceByCode(String code) {
        return deviceRepository.selectByCode(code);
    }

    /**
     * 新增设备
     *
     * @param deviceVo 设备信息
     * @param userId   操作用户ID
     * @return 结果
     */
    public int createDevice(DeviceVo deviceVo, String userId) {
        Device device = DeviceAssembler.INSTANCE.toDomain(deviceVo);
        return deviceRepository.insert(device);
    }

    /**
     * 修改设备
     *
     * @param deviceVo 设备信息
     * @param userId   操作用户ID
     * @return 结果
     */
    public int modifyDevice(DeviceVo deviceVo, String userId) {
        Device device = DeviceAssembler.INSTANCE.toDomain(deviceVo);
        return deviceRepository.update(device);
    }

    /**
     * 批量删除设备
     *
     * @param ids 设备ID数组
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids) {
        return deviceRepository.batchPhysicalDelete(ids);
    }

}
