package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;

import java.util.List;
import java.util.Map;

/**
 * 物理零件实例数据仓库接口
 *
 * @author hwyz_leo
 */
public interface PartInfoRepository {

    /**
     * 根据条件查询零件实例列表
     *
     * @param map 查询条件
     * @return 零件实例列表
     */
    List<PartInfo> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询零件实例
     *
     * @param id 主键ID
     * @return 零件实例
     */
    PartInfo selectById(Long id);

    /**
     * 根据零件编码和序列号查询零件实例
     *
     * @param partCode 零件编码
     * @param sn 序列号
     * @return 零件实例
     */
    PartInfo selectByPartCodeAndSn(String partCode, String sn);

    /**
     * 新增零件实例
     *
     * @param partInfo 零件实例
     * @return 影响行数
     */
    int insert(PartInfo partInfo);

    /**
     * 批量新增零件实例
     *
     * @param partInfoList 零件实例列表
     * @return 影响行数
     */
    int batchInsert(List<PartInfo> partInfoList);

    /**
     * 修改零件实例
     *
     * @param partInfo 零件实例
     * @return 影响行数
     */
    int update(PartInfo partInfo);

    /**
     * 批量物理删除零件实例
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
