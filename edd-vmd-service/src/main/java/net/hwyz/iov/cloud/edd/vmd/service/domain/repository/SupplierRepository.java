package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Supplier;

import java.util.List;
import java.util.Map;

/**
 * 供应商数据仓库接口
 *
 * @author hwyz_leo
 */
public interface SupplierRepository {

    /**
     * 根据条件查询供应商列表
     *
     * @param map 查询条件
     * @return 供应商列表
     */
    List<Supplier> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询供应商
     *
     * @param id 主键ID
     * @return 供应商
     */
    Supplier selectById(Long id);

    /**
     * 根据供应商代码查询供应商
     *
     * @param code 供应商代码
     * @return 供应商
     */
    Supplier selectByCode(String code);

    /**
     * 新增供应商
     *
     * @param supplier 供应商
     * @return 影响行数
     */
    int insert(Supplier supplier);

    /**
     * 修改供应商
     *
     * @param supplier 供应商
     * @return 影响行数
     */
    int update(Supplier supplier);

    /**
     * 批量物理删除供应商
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
