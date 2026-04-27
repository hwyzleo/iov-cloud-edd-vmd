package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Device;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.DeviceRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.DeviceConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.DeviceMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.DevicePo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 设备数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceRepositoryImpl implements DeviceRepository {

    private final DeviceMapper deviceMapper;

    @Override
    public List<Device> selectByMap(Map<String, Object> map) {
        List<DevicePo> poList = deviceMapper.selectPoByMap(map);
        return PageUtil.convert(poList, DeviceConverter.INSTANCE::toDomain);
    }

    @Override
    public Device selectById(Long id) {
        return DeviceConverter.INSTANCE.toDomain(deviceMapper.selectPoById(id));
    }

    @Override
    public Device selectByCode(String code) {
        return DeviceConverter.INSTANCE.toDomain(deviceMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Device device) {
        return deviceMapper.insertPo(DeviceConverter.INSTANCE.fromDomain(device));
    }

    @Override
    public int update(Device device) {
        return deviceMapper.updatePo(DeviceConverter.INSTANCE.fromDomain(device));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return deviceMapper.batchPhysicalDeletePo(ids);
    }

}
