package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;

import java.util.List;

/**
 * 车辆导入数据仓储接口
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
public interface VehImportDataRepository {

    VehImportData selectById(Long id);

    VehImportData selectByBatchNum(String batchNum);

    int insert(VehImportData vehImportData);

    int update(VehImportData vehImportData);

    int deleteByIds(Long[] ids);

    List<VehImportData> selectList(VehImportData vehImportData);

    boolean checkBatchNumUnique(Long id, String batchNum);
}
