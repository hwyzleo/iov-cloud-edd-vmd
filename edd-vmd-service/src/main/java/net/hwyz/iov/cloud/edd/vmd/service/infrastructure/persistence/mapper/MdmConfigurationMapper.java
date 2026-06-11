package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmConfigurationPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 车辆生产配置表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-10-11
 */
@Mapper
public interface MdmConfigurationMapper extends BaseDao<MdmConfigurationPo, Long> {

    /**
     * 通过code查询生产配置信息
     *
     * @param code 生产配置编码
     * @return 生产配置信息
     */
    MdmConfigurationPo selectPoByCode(String code);

    /**
     * 通过外部引用ID查询生产配置信息
     *
     * @param externalRefId 外部引用ID
     * @return 生产配置信息
     */
    MdmConfigurationPo selectPoByExternalRefId(@Param("externalRefId") String externalRefId);

    /**
     * 统计指定来源的生产配置数量
     *
     * @param source 数据来源
     * @return 数量
     */
    long countPoBySource(@Param("source") String source);

}
