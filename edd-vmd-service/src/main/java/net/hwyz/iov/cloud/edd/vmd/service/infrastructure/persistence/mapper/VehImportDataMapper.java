package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportDataPo;
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
public interface VehImportDataMapper extends BaseDao<VehImportDataPo, Long> {

    /**
     * 根据批次号查询车辆导入数据
     *
     * @param batchNum 批次号
     * @return 车辆导入数据
     */
    VehImportDataPo selectPoByBatchNum(String batchNum);

}
