package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;

import java.util.List;
import java.util.Map;

/**
 * 车系数据仓库接口
 *
 * @author hwyz_leo
 */
public interface MdmCarLineRepository {

    /**
     * 根据条件查询车系列表
     *
     * @param map 查询条件
     * @return 车系列表
     */
    List<CarLine> selectByMap(Map<String, Object> map);

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
    CarLine selectById(Long id);

    /**
     * 根据车系代码查询车系
     *
     * @param code 车系代码
     * @return 车系
     */
    CarLine selectByCode(String code);

    /**
     * 根据MDM外部引用ID查询车系
     *
     * @param externalRefId MDM外部引用ID
     * @return 车系
     */
    CarLine selectByExternalRefId(String externalRefId);

    /**
     * 根据数据来源统计车系数量
     *
     * @param source 数据来源
     * @return 数量
     */
    long countBySource(SourceType source);

    /**
     * 新增车系
     *
     * @param carLine 车系
     * @return 影响行数
     */
    int insert(CarLine carLine);

    /**
     * 修改车系
     *
     * @param carLine 车系
     * @return 影响行数
     */
    int update(CarLine carLine);

    /**
     * 根据主键ID修改车系
     *
     * @param carLine 车系
     * @return 影响行数
     */
    int updateById(CarLine carLine);

    /**
     * 批量物理删除车系
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

}
