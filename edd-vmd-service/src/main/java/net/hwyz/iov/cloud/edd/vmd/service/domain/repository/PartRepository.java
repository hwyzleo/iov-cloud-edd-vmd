package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;

import java.util.List;
import java.util.Map;

/**
 * 零件数据仓库接口
 *
 * @author hwyz_leo
 */
public interface PartRepository {

    /**
     * 根据条件查询零件列表
     *
     * @param map 查询条件
     * @return 零件列表
     */
    List<Part> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询零件
     *
     * @param id 主键ID
     * @return 零件
     */
    Part selectById(Long id);

    /**
     * 根据零件号查询零件
     *
     * @param pn 零件号
     * @return 零件
     */
    Part selectByPn(String pn);

    /**
     * 新增零件
     *
     * @param part 零件
     * @return 影响行数
     */
    int insert(Part part);

    /**
     * 修改零件
     *
     * @param part 零件
     * @return 影响行数
     */
    int update(Part part);

    /**
     * 批量物理删除零件
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
