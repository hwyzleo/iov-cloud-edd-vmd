package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ImportOperationPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一导入操作主表 Mapper 接口（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Mapper
public interface ImportOperationMapper extends BaseMapper<ImportOperationPo> {

    /**
     * 查询同 domain+importData 的 active 操作（生成列互斥）
     */
    ImportOperationPo selectActiveByDomainAndImportDataId(@Param("domain") String domain, @Param("importDataId") Long importDataId);

    /**
     * 认领操作
     */
    int claim(@Param("operationId") String operationId, @Param("owner") String owner,
              @Param("leaseUntil") LocalDateTime leaseUntil, @Param("now") LocalDateTime now);

    /**
     * 续租
     */
    int renewLease(@Param("operationId") String operationId, @Param("owner") String owner,
                   @Param("leaseUntil") LocalDateTime leaseUntil);

    /**
     * 完成操作
     */
    int finish(@Param("operationId") String operationId, @Param("owner") String owner,
               @Param("status") String status, @Param("countersJson") String countersJson,
               @Param("finishedAt") LocalDateTime finishedAt);

    /**
     * 核验 fence
     */
    int countByFence(@Param("operationId") String operationId, @Param("owner") String owner,
                     @Param("fenceToken") Long fenceToken, @Param("now") LocalDateTime now);

    /**
     * 查询失联操作（RUNNING 且租约过期）
     */
    List<ImportOperationPo> selectExpired(@Param("now") LocalDateTime now);

}
