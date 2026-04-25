package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModel;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModelFeatureCode;

import java.util.List;
import java.util.Map;

/**
 * 基础车型数据仓库接口
 *
 * @author hwyz_leo
 */
public interface VehBaseModelRepository {

    /**
     * 根据条件查询基础车型列表
     *
     * @param map 查询条件
     * @return 基础车型列表
     */
    List<BaseModel> selectByMap(Map<String, Object> map);

    /**
     * 根据条件统计基础车型数量
     *
     * @param map 查询条件
     * @return 数量
     */
    int countByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询基础车型
     *
     * @param id 主键ID
     * @return 基础车型
     */
    BaseModel selectById(Long id);

    /**
     * 根据基础车型代码查询基础车型
     *
     * @param code 基础车型代码
     * @return 基础车型
     */
    BaseModel selectByCode(String code);

    /**
     * 新增基础车型
     *
     * @param baseModel 基础车型
     * @return 影响行数
     */
    int insert(BaseModel baseModel);

    /**
     * 修改基础车型
     *
     * @param baseModel 基础车型
     * @return 影响行数
     */
    int update(BaseModel baseModel);

    /**
     * 批量物理删除基础车型
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    /**
     * 根据示例查询基础车型特征关系列表
     *
     * @param example 示例
     * @return 基础车型特征关系列表
     */
    List<BaseModelFeatureCode> selectFeatureCodeByExample(BaseModelFeatureCode example);

    /**
     * 批量新增基础车型特征关系
     *
     * @param featureCodeList 基础车型特征关系列表
     * @return 影响行数
     */
    int batchInsertFeatureCode(List<BaseModelFeatureCode> featureCodeList);

    /**
     * 修改基础车型特征关系
     *
     * @param featureCode 基础车型特征关系
     * @return 影响行数
     */
    int updateFeatureCode(BaseModelFeatureCode featureCode);

    /**
     * 批量物理删除基础车型特征关系
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDeleteFeatureCode(Long[] ids);

}
