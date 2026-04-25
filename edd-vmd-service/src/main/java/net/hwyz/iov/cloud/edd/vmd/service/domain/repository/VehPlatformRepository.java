package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;

import java.util.List;
import java.util.Map;

/**
 * 平台数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehPlatformRepository {

    /**
     * 根据条件查询平台列表
     *
     * @param map 查询条件
     * @return 平台列表
     */
    List<Platform> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计平台数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询平台
     *
     * @param id 主键ID
     * @return 平台
     */
    Platform selectById(Long id);

    /**
     * 根据平台代码查询平台
     *
     * @param code 平台代码
     * @return 平台
     */
    Platform selectByCode(String code);

    /**
     * 新增平台
     *
     * @param platform 平台
     * @return 影响行数
     */
    int insert(Platform platform);

    /**
     * 修改平台
     *
     * @param platform 平台
     * @return 影响行数
     */
    int update(Platform platform);

    /**
     * 批量物理删除平台
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
