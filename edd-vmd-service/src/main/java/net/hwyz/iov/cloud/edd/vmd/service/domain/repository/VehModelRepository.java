package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;

import java.util.List;
import java.util.Map;

/**
 * 车型数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehModelRepository {

    /**
     * 根据条件查询车型列表
     *
     * @param map 查询条件
     * @return 车型列表
     */
    List<Model> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计车型数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询车型
     *
     * @param id 主键ID
     * @return 车型
     */
    Model selectById(Long id);

    /**
     * 根据车型代码查询车型
     *
     * @param code 车型代码
     * @return 车型
     */
    Model selectByCode(String code);

    /**
     * 新增车型
     *
     * @param model 车型
     * @return 影响行数
     */
    int insert(Model model);

    /**
     * 修改车型
     *
     * @param model 车型
     * @return 影响行数
     */
    int update(Model model);

    /**
     * 批量物理删除车型
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
