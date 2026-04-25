package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureFamily;

import java.util.List;
import java.util.Map;

/**
 * 特征数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehFeatureFamilyRepository {

    // ==================== 特征族 ====================

    /**
     * 根据条件查询特征族列表
     *
     * @param map 查询条件
     * @return 特征族列表
     */
    List<FeatureFamily> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询特征族
     *
     * @param id 主键ID
     * @return 特征族
     */
    FeatureFamily selectById(Long id);

    /**
     * 根据特征族代码查询特征族
     *
     * @param code 特征族代码
     * @return 特征族
     */
    FeatureFamily selectByCode(String code);

    /**
     * 新增特征族
     *
     * @param featureFamily 特征族
     * @return 影响行数
     */
    int insert(FeatureFamily featureFamily);

    /**
     * 修改特征族
     *
     * @param featureFamily 特征族
     * @return 影响行数
     */
    int update(FeatureFamily featureFamily);

    /**
     * 批量物理删除特征族
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    // ==================== 特征值 ====================

    /**
     * 根据特征族代码查询特征值列表
     *
     * @param familyCode 特征族代码
     * @return 特征值列表
     */
    List<FeatureCode> selectFeatureCodeByFamilyCode(String familyCode);

    /**
     * 根据主键ID查询特征值
     *
     * @param id 主键ID
     * @return 特征值
     */
    FeatureCode selectFeatureCodeById(Long id);

    /**
     * 根据特征值代码查询特征值
     *
     * @param code 特征值代码
     * @return 特征值
     */
    FeatureCode selectFeatureCodeByCode(String code);

    /**
     * 新增特征值
     *
     * @param featureCode 特征值
     * @return 影响行数
     */
    int insertFeatureCode(FeatureCode featureCode);

    /**
     * 修改特征值
     *
     * @param featureCode 特征值
     * @return 影响行数
     */
    int updateFeatureCode(FeatureCode featureCode);

    /**
     * 批量物理删除特征值
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDeleteFeatureCode(Long[] ids);

}
