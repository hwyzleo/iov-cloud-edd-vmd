package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;

import java.util.List;
import java.util.Map;

/**
 * 品牌数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehBrandRepository {

    /**
     * 根据条件查询品牌列表
     *
     * @param map 查询条件
     * @return 品牌列表
     */
    List<Brand> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计品牌数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询品牌
     *
     * @param id 主键ID
     * @return 品牌
     */
    Brand selectById(Long id);

    /**
     * 根据品牌代码查询品牌
     *
     * @param code 品牌代码
     * @return 品牌
     */
    Brand selectByCode(String code);

    /**
     * 新增品牌
     *
     * @param brand 品牌
     * @return 影响行数
     */
    int insert(Brand brand);

    /**
     * 修改品牌
     *
     * @param brand 品牌
     * @return 影响行数
     */
    int update(Brand brand);

    /**
     * 批量物理删除品牌
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
