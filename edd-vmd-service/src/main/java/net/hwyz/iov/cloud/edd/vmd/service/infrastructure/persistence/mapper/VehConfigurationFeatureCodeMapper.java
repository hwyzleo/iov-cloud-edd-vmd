package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehConfigurationFeatureCodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 车辆生产配置特征码表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-10-11
 */
@Mapper
public interface VehConfigurationFeatureCodeMapper extends BaseDao<VehConfigurationFeatureCodePo, Long> {

    /**
     * 通过配置编码和family编码查询特征码
     *
     * @param configurationCode 配置编码
     * @param familyCode family编码
     * @return 特征码信息
     */
    VehConfigurationFeatureCodePo selectPoByConfigurationCodeAndFamilyCode(String configurationCode, String familyCode);

    /**
     * 通过特征码map查询配置编码
     *
     * @param params 参数
     * @return 配置编码列表
     */
    List<String> selectConfigurationCodeByFeatureCodeMap(Map<String, Object> params);

}
