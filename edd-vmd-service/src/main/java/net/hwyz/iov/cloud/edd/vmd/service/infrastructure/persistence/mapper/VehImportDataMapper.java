package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportDataPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 车辆导入数据表 DAO
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@Mapper
public interface VehImportDataMapper extends BaseDao<VehImportDataPo, Long> {

    /**
     * 根据批次号查询
     *
     * @param batchNum 批次号
     * @return 车辆导入数据
     */
    @Select("SELECT * FROM tb_veh_import_data WHERE batch_num = #{batchNum} AND row_valid = 1")
    VehImportDataPo selectPoByBatchNum(@Param("batchNum") String batchNum);

    /**
     * 根据批次号统计数量
     *
     * @param batchNum 批次号
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_veh_import_data WHERE batch_num = #{batchNum} AND row_valid = 1")
    long countPoByBatchNum(@Param("batchNum") String batchNum);

    /**
     * 根据批次号统计数量（排除指定ID）
     *
     * @param batchNum 批次号
     * @param id 排除的ID
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_veh_import_data WHERE batch_num = #{batchNum} AND id != #{id} AND row_valid = 1")
    long countPoByBatchNumAndIdNot(@Param("batchNum") String batchNum, @Param("id") Long id);

}
