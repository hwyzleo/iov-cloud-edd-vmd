package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 零件导入数据仓储接口
 *
 * @author hwyz_leo
 */
public interface PartImportDataRepository {

    PartImportData selectById(Long id);

    PartImportData selectByBatchNum(String batchNum);

    int insert(PartImportData partImportData);

    int update(PartImportData partImportData);

    int deleteByIds(Long[] ids);

    List<PartImportData> selectList(PartImportData partImportData, LocalDateTime beginTime, LocalDateTime endTime);

    boolean checkBatchNumUnique(Long id, String batchNum);
}
