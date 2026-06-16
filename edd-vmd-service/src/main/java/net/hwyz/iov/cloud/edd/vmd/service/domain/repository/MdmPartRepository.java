package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;

import java.util.List;
import java.util.Map;

/**
 * 零件数据仓库接口（MDM投影专用）
 *
 * @author hwyz_leo
 */
public interface MdmPartRepository {

    /**
     * 根据条件查询零件列表
     *
     * @param map 查询条件
     * @return 零件列表
     */
    List<Part> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计零件数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

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
     * @param code 零件号
     * @return 零件
     */
    Part selectByCode(String code);

    /**
     * 根据MDM外部引用ID查询零件
     *
     * @param externalRefId MDM外部引用ID
     * @return 零件
     */
    Part selectByExternalRefId(String externalRefId);

    /**
     * 根据数据来源统计零件数量
     *
     * @param source 数据来源
     * @return 数量
     */
    long countBySource(SourceType source);

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
     * 根据主键ID修改零件
     *
     * @param part 零件
     * @return 影响行数
     */
    int updateById(Part part);

    /**
     * 批量物理删除零件
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
