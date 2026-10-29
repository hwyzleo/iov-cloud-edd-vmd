package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperationItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ImportOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ImportOperationConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ImportOperationItemConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.ImportOperationItemMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.ImportOperationMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ImportOperationItemPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ImportOperationPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一导入操作数据仓库实现（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ImportOperationRepositoryImpl implements ImportOperationRepository {

    private final ImportOperationMapper importOperationMapper;
    private final ImportOperationItemMapper importOperationItemMapper;

    @Override
    public int insert(ImportOperation operation) {
        ImportOperationPo po = ImportOperationConverter.INSTANCE.fromDomain(operation);
        fillAudit(po);
        int result = importOperationMapper.insert(po);
        if (result > 0 && po.getId() != null) {
            operation.setId(po.getId());
        }
        return result;
    }

    @Override
    public ImportOperation selectByOperationId(String operationId) {
        return ImportOperationConverter.INSTANCE.toDomain(
                importOperationMapper.selectOne(new LambdaQueryWrapper<ImportOperationPo>()
                        .eq(ImportOperationPo::getOperationId, operationId)
                        .last("LIMIT 1")));
    }

    @Override
    public ImportOperation selectActiveByDomainAndImportDataId(String domain, Long importDataId) {
        return ImportOperationConverter.INSTANCE.toDomain(
                importOperationMapper.selectActiveByDomainAndImportDataId(domain, importDataId));
    }

    @Override
    public int claim(String operationId, String owner, LocalDateTime leaseUntil) {
        return importOperationMapper.claim(operationId, owner, leaseUntil, LocalDateTime.now());
    }

    @Override
    public int renewLease(String operationId, String owner, LocalDateTime leaseUntil) {
        return importOperationMapper.renewLease(operationId, owner, leaseUntil);
    }

    @Override
    public int finish(String operationId, String owner, String status, String countersJson) {
        return importOperationMapper.finish(operationId, owner, status, countersJson, LocalDateTime.now());
    }

    @Override
    public boolean verifyFence(String operationId, String owner, Long fenceToken) {
        return importOperationMapper.countByFence(operationId, owner, fenceToken, LocalDateTime.now()) > 0;
    }

    @Override
    public List<ImportOperation> selectExpired(LocalDateTime now) {
        return ImportOperationConverter.INSTANCE.toDomainList(importOperationMapper.selectExpired(now));
    }

    @Override
    public List<ImportOperation> selectRecent(String domain, String operatorId, String status, int limit) {
        LambdaQueryWrapper<ImportOperationPo> wrapper = new LambdaQueryWrapper<ImportOperationPo>()
                .eq(domain != null, ImportOperationPo::getDomain, domain)
                .eq(operatorId != null, ImportOperationPo::getOperatorId, operatorId)
                .eq(status != null, ImportOperationPo::getStatus, status)
                .orderByDesc(ImportOperationPo::getCreatedAt)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 200));
        return ImportOperationConverter.INSTANCE.toDomainList(importOperationMapper.selectList(wrapper));
    }

    @Override
    public int update(ImportOperation operation) {
        return importOperationMapper.updateById(ImportOperationConverter.INSTANCE.fromDomain(operation));
    }

    // ==================== 明细 ====================

    @Override
    public int insertItem(ImportOperationItem item) {
        ImportOperationItemPo po = ImportOperationItemConverter.INSTANCE.fromDomain(item);
        fillAudit(po);
        int result = importOperationItemMapper.insert(po);
        if (result > 0 && po.getId() != null) {
            item.setId(po.getId());
        }
        return result;
    }

    @Override
    public int batchInsertItems(List<ImportOperationItem> items) {
        List<ImportOperationItemPo> poList = ImportOperationItemConverter.INSTANCE.fromDomainList(items);
        if (poList.isEmpty()) {
            return 0;
        }
        poList.forEach(this::fillAudit);
        return poList.stream().mapToInt(importOperationItemMapper::insert).sum();
    }

    /**
     * 审计字段兜底填充（不依赖框架自动填充，保证 NOT NULL 列可写）
     */
    private void fillAudit(net.hwyz.iov.cloud.framework.mysql.po.BasePo po) {
        if (po.getCreateTime() == null) {
            po.setCreateTime(new java.util.Date());
        }
        if (po.getModifyTime() == null) {
            po.setModifyTime(new java.util.Date());
        }
        if (po.getRowVersion() == null) {
            po.setRowVersion(1);
        }
        if (po.getRowValid() == null) {
            po.setRowValid(true);
        }
    }

    @Override
    public int updateItem(ImportOperationItem item) {
        return importOperationItemMapper.updateById(ImportOperationItemConverter.INSTANCE.fromDomain(item));
    }

    @Override
    public ImportOperationItem selectItemByKey(String operationId, byte[] itemKey) {
        return ImportOperationItemConverter.INSTANCE.toDomain(
                importOperationItemMapper.selectOne(new LambdaQueryWrapper<ImportOperationItemPo>()
                        .eq(ImportOperationItemPo::getOperationId, operationId)
                        .eq(ImportOperationItemPo::getItemKey, itemKey)
                        .last("LIMIT 1")));
    }

    @Override
    public List<ImportOperationItem> selectItems(String operationId, String status, Long cursorId, int limit) {
        LambdaQueryWrapper<ImportOperationItemPo> wrapper = new LambdaQueryWrapper<ImportOperationItemPo>()
                .eq(ImportOperationItemPo::getOperationId, operationId)
                .eq(status != null, ImportOperationItemPo::getStatus, status)
                .gt(cursorId != null, ImportOperationItemPo::getId, cursorId)
                .orderByAsc(ImportOperationItemPo::getId)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 200));
        return ImportOperationItemConverter.INSTANCE.toDomainList(importOperationItemMapper.selectList(wrapper));
    }

}
