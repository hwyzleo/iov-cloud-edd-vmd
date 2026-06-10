package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmConfigurationPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

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

}
