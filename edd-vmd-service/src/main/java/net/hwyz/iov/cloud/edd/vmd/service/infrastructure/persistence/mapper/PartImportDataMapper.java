package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartImportDataPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 零件导入数据表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-06-15
 */
@Mapper
public interface PartImportDataMapper extends BaseDao<PartImportDataPo, Long> {

}
