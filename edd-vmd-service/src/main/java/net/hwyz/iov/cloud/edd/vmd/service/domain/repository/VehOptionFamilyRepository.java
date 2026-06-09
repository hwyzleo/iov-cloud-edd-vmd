package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.OptionFamily;

import java.util.List;
import java.util.Map;

/**
 * 选装数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehOptionFamilyRepository {

    // ==================== 选装族 ====================

    /**
     * 根据条件查询选装族列表
     *
     * @param map 查询条件
     * @return 选装族列表
     */
    List<OptionFamily> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询选装族
     *
     * @param id 主键ID
     * @return 选装族
     */
    OptionFamily selectById(Long id);

    /**
     * 根据选装族代码查询选装族
     *
     * @param code 选装族代码
     * @return 选装族
     */
    OptionFamily selectByCode(String code);

    /**
     * 根据外部引用ID查询选装族
     *
     * @param externalRefId 外部引用ID
     * @return 选装族
     */
    OptionFamily selectByExternalRefId(String externalRefId);

    /**
     * 根据来源统计选装族数量
     *
     * @param source 来源
     * @return 数量
     */
    long countBySource(String source);

    /**
     * 新增选装族
     *
     * @param optionFamily 选装族
     * @return 影响行数
     */
    int insert(OptionFamily optionFamily);

    /**
     * 修改选装族
     *
     * @param optionFamily 选装族
     * @return 影响行数
     */
    int update(OptionFamily optionFamily);

    /**
     * 根据ID修改选装族
     *
     * @param optionFamily 选装族
     * @return 影响行数
     */
    int updateById(OptionFamily optionFamily);

    /**
     * 批量物理删除选装族
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    // ==================== 选装值 ====================

    /**
     * 根据选装族代码查询选装值列表
     *
     * @param optionFamilyCode 选装族代码
     * @return 选装值列表
     */
    List<OptionCode> selectOptionCodeByOptionFamilyCode(String optionFamilyCode);

    /**
     * 根据主键ID查询选装值
     *
     * @param id 主键ID
     * @return 选装值
     */
    OptionCode selectOptionCodeById(Long id);

    /**
     * 根据选装值代码查询选装值
     *
     * @param code 选装值代码
     * @return 选装值
     */
    OptionCode selectOptionCodeByCode(String code);

    /**
     * 根据外部引用ID查询选装值
     *
     * @param externalRefId 外部引用ID
     * @return 选装值
     */
    OptionCode selectOptionCodeByExternalRefId(String externalRefId);

    /**
     * 根据来源统计选装值数量
     *
     * @param source 来源
     * @return 数量
     */
    long countOptionCodeBySource(String source);

    /**
     * 新增选装值
     *
     * @param optionCode 选装值
     * @return 影响行数
     */
    int insertOptionCode(OptionCode optionCode);

    /**
     * 修改选装值
     *
     * @param optionCode 选装值
     * @return 影响行数
     */
    int updateOptionCode(OptionCode optionCode);

    /**
     * 根据ID修改选装值
     *
     * @param optionCode 选装值
     * @return 影响行数
     */
    int updateOptionCodeById(OptionCode optionCode);

    /**
     * 批量物理删除选装值
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDeleteOptionCode(Long[] ids);

}
