package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Manufacturer;

import java.util.List;
import java.util.Map;

/**
 * 生产厂商数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehManufacturerRepository {

    /**
     * 根据条件查询生产厂商列表
     *
     * @param map 查询条件
     * @return 生产厂商列表
     */
    List<Manufacturer> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计生产厂商数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询生产厂商
     *
     * @param id 主键ID
     * @return 生产厂商
     */
    Manufacturer selectById(Long id);

    /**
     * 根据工厂代码查询生产厂商
     *
     * @param code 工厂代码
     * @return 生产厂商
     */
    Manufacturer selectByCode(String code);

    /**
     * 新增生产厂商
     *
     * @param manufacturer 生产厂商
     * @return 影响行数
     */
    int insert(Manufacturer manufacturer);

    /**
     * 修改生产厂商
     *
     * @param manufacturer 生产厂商
     * @return 影响行数
     */
    int update(Manufacturer manufacturer);

    /**
     * 批量物理删除生产厂商
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
