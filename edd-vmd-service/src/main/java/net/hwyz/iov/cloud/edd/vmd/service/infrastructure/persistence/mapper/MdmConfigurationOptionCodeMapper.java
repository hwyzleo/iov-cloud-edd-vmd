package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmConfigurationOptionCodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 车辆配置选项值关系表 DAO（原VehBuildConfigFeatureCodeMapper→MdmConfigurationOptionCodeMapper，CR-018重命名）
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-10-11
 */
@Mapper
public interface MdmConfigurationOptionCodeMapper extends BaseDao<MdmConfigurationOptionCodePo, Long> {

    /**
     * 通过配置编码和选项族编码查询
     *
     * @param configurationCode 配置编码
     * @param optionFamilyCode  选项族编码
     * @return 配置选项值关系
     */
    MdmConfigurationOptionCodePo selectPoByConfigurationCodeAndOptionFamilyCode(String configurationCode, String optionFamilyCode);

    /**
     * 通过选项值map查询配置编码
     *
     * @param params 参数
     * @return 配置编码列表
     */
    List<String> selectConfigurationCodeByOptionCodeMap(Map<String, Object> params);

}
