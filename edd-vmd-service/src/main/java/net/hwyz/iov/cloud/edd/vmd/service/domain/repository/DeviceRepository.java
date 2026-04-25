package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Device;

import java.util.List;
import java.util.Map;

/**
 * 设备数据仓库接口
 *
 * @author hwyz_leo
 */
public interface DeviceRepository {

    /**
     * 根据条件查询设备列表
     *
     * @param map 查询条件
     * @return 设备列表
     */
    List<Device> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询设备
     *
     * @param id 主键ID
     * @return 设备
     */
    Device selectById(Long id);

    /**
     * 根据设备编码查询设备
     *
     * @param code 设备编码
     * @return 设备
     */
    Device selectByCode(String code);

    /**
     * 新增设备
     *
     * @param device 设备
     * @return 影响行数
     */
    int insert(Device device);

    /**
     * 修改设备
     *
     * @param device 设备
     * @return 影响行数
     */
    int update(Device device);

    /**
     * 批量物理删除设备
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
