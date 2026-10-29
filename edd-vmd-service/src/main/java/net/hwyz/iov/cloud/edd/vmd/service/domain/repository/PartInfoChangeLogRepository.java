package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfoChangeLog;

import java.util.List;

/**
 * 零件实例属性变更历史数据仓库接口（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
public interface PartInfoChangeLogRepository {

    /**
     * 新增变更日志
     *
     * @param changeLog 变更日志
     * @return 影响行数
     */
    int insert(PartInfoChangeLog changeLog);

    /**
     * 按零件实例与业务版本查询变更日志
     *
     * @param partId 零件实例ID
     * @param rowVersion 业务本体提交版本
     * @return 变更日志（可能为 null）
     */
    PartInfoChangeLog selectByPartIdAndRowVersion(Long partId, Integer rowVersion);

    /**
     * 查询零件实例变更日志（按时间倒序）
     *
     * @param partId 零件实例ID
     * @return 变更日志列表
     */
    List<PartInfoChangeLog> selectByPartId(Long partId);

}
