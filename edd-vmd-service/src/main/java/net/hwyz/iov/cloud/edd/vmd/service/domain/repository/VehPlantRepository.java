package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;

import java.util.List;
import java.util.Map;

/**
 * 生产工厂数据仓库接口（原VehManufacturerRepository）
 *
 * @author hwyz_leo
 */
public interface VehPlantRepository {

    /**
     * 根据条件查询生产工厂列表
     *
     * @param map 查询条件
     * @return 生产工厂列表
     */
    List<Plant> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计生产工厂数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询生产工厂
     *
     * @param id 主键ID
     * @return 生产工厂
     */
    Plant selectById(Long id);

    /**
     * 根据工厂代码查询生产工厂
     *
     * @param code 工厂代码
     * @return 生产工厂
     */
    Plant selectByCode(String code);

    /**
     * 新增生产工厂
     *
     * @param plant 生产工厂
     * @return 影响行数
     */
    int insert(Plant plant);

    /**
     * 修改生产工厂
     *
     * @param plant 生产工厂
     * @return 影响行数
     */
    int update(Plant plant);

    /**
     * 批量物理删除生产工厂
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    /**
     * 根据外部引用ID查询生产工厂
     *
     * @param externalRefId 外部引用ID
     * @return 生产工厂
     */
    Plant selectByExternalRefId(String externalRefId);

    /**
     * 根据数据来源统计生产工厂数量
     *
     * @param source 数据来源
     * @return 数量
     */
    int countBySource(String source);

}