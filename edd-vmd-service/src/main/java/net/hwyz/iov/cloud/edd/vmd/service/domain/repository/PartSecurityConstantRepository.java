package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;

/**
 * 零件安全常量仓储接口
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
public interface PartSecurityConstantRepository {

    /**
     * 根据零件编码和序列号查询
     *
     * @param partCode 零件编码
     * @param sn 零件序列号
     * @return 零件安全常量
     */
    PartSecurityConstant selectByPartCodeAndSn(String partCode, String sn);

    /**
     * 插入记录
     *
     * @param entity 零件安全常量
     * @return 影响行数
     */
    int insert(PartSecurityConstant entity);

    /**
     * 更新记录
     *
     * @param entity 零件安全常量
     * @return 影响行数
     */
    int update(PartSecurityConstant entity);

    /**
     * 根据零件编码和序列号删除
     *
     * @param partCode 零件编码
     * @param sn 零件序列号
     * @return 影响行数
     */
    int deleteByPartCodeAndSn(String partCode, String sn);
}
