package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆基础车型表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-01-19
 */
@Mapper
public interface VehBaseModelMapper extends BaseDao<VehBaseModelPo, Long> {

    /**
     * 通过code查询基础车型信息
     *
     * @param code 基础车型编码
     * @return 基础车型信息
     */
    VehBaseModelPo selectPoByCode(String code);

}
