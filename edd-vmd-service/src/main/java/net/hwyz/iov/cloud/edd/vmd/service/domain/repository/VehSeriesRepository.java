package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Series;

import java.util.List;
import java.util.Map;

/**
 * 车系数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehSeriesRepository {

    /**
     * 根据条件查询车系列表
     *
     * @param map 查询条件
     * @return 车系列表
     */
    List<Series> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计车系数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询车系
     *
     * @param id 主键ID
     * @return 车系
     */
    Series selectById(Long id);

    /**
     * 根据车系代码查询车系
     *
     * @param code 车系代码
     * @return 车系
     */
    Series selectByCode(String code);

    /**
     * 新增车系
     *
     * @param series 车系
     * @return 影响行数
     */
    int insert(Series series);

    /**
     * 修改车系
     *
     * @param series 车系
     * @return 影响行数
     */
    int update(Series series);

    /**
     * 批量物理删除车系
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
