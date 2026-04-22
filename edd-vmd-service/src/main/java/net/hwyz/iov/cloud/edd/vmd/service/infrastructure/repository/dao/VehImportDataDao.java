package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehImportDataDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆导入数据表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-04-25
 */
@Mapper
public interface VehImportDataDao extends BaseDao<VmdVehImportDataDo, Long> {

    /**
     * 根据批次号查询车辆导入数据
     *
     * @param batchNum 批次号
     * @return 车辆导入数据
     */
    VmdVehImportDataDo selectPoByBatchNum(String batchNum);

}
