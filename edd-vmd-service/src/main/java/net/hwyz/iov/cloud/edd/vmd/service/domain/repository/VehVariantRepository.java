package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VariantOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;

import java.util.List;
import java.util.Map;

/**
 * 版本数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehVariantRepository {

    /**
     * 根据条件查询版本列表
     *
     * @param map 查询条件
     * @return 版本列表
     */
    List<Variant> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计版本数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询版本
     *
     * @param id 主键ID
     * @return 版本
     */
    Variant selectById(Long id);

    /**
     * 根据版本代码查询版本
     *
     * @param code 版本代码
     * @return 版本
     */
    Variant selectByCode(String code);

    /**
     * 根据MDM外部引用ID查询版本
     *
     * @param externalRefId MDM外部引用ID
     * @return 版本
     */
    Variant selectByExternalRefId(String externalRefId);

    /**
     * 根据数据来源统计版本数量
     *
     * @param source 数据来源
     * @return 数量
     */
    long countBySource(SourceType source);

    /**
     * 新增版本
     *
     * @param variant 版本
     * @return 影响行数
     */
    int insert(Variant variant);

    /**
     * 修改版本
     *
     * @param variant 版本
     * @return 影响行数
     */
    int update(Variant variant);

    /**
     * 根据主键ID修改版本
     *
     * @param variant 版本
     * @return 影响行数
     */
    int updateById(Variant variant);

    /**
     * 批量物理删除版本
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    /**
     * 根据条件查询版本选项值关系列表
     *
     * @param example 查询条件
     * @return 版本选项值关系列表
     */
    List<VariantOptionCode> selectOptionCodeByExample(VariantOptionCode example);

    /**
     * 批量新增版本选项值关系
     *
     * @param optionCodeList 版本选项值关系列表
     * @return 影响行数
     */
    int batchInsertOptionCode(List<VariantOptionCode> optionCodeList);

    /**
     * 修改版本选项值关系
     *
     * @param optionCode 版本选项值关系
     * @return 影响行数
     */
    int updateOptionCode(VariantOptionCode optionCode);

    /**
     * 批量物理删除版本选项值关系
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDeleteOptionCode(Long[] ids);

}
