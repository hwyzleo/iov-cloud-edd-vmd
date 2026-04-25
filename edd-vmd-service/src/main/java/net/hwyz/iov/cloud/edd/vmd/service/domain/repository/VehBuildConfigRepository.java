package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;

import java.util.List;
import java.util.Map;

/**
 * 生产配置数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehBuildConfigRepository {

    /**
     * 根据条件查询生产配置列表
     *
     * @param map 查询条件
     * @return 生产配置列表
     */
    List<BuildConfig> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计生产配置数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询生产配置
     *
     * @param id 主键ID
     * @return 生产配置
     */
    BuildConfig selectById(Long id);

    /**
     * 根据生产配置代码查询生产配置
     *
     * @param code 生产配置代码
     * @return 生产配置
     */
    BuildConfig selectByCode(String code);

    /**
     * 新增生产配置
     *
     * @param buildConfig 生产配置
     * @return 影响行数
     */
    int insert(BuildConfig buildConfig);

    /**
     * 修改生产配置
     *
     * @param buildConfig 生产配置
     * @return 影响行数
     */
    int update(BuildConfig buildConfig);

    /**
     * 批量物理删除生产配置
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    /**
     * 根据示例查询生产配置列表
     *
     * @param example 示例
     * @return 生产配置列表
     */
    List<BuildConfig> selectByExample(BuildConfig example);

}
