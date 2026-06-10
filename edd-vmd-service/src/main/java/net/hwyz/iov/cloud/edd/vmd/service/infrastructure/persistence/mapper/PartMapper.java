package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 零件信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-26
 */
@Mapper
public interface PartMapper extends BaseDao<PartPo, Long> {

    /**
     * 根据零件号查询零件信息
     *
     * @param pn 零件号
     * @return 零件信息
     */
    PartPo selectPoByPn(String pn);

    /**
     * 获取所有FOTA升级零件
     *
     * @param software 是否为软件零件
     * @return 零件信息列表
     */
    List<PartPo> selectAllFotaPo(Boolean software);

    /**
     * 根据MDM外部引用ID查询零件
     *
     * @param externalRefId MDM外部引用ID
     * @return 零件持久化对象
     */
    PartPo selectPoByExternalRefId(@Param("externalRefId") String externalRefId);

    /**
     * 根据数据来源统计零件数量
     *
     * @param source 数据来源
     * @return 数量
     */
    long countPoBySource(@Param("source") String source);

    /**
     * 根据主键ID修改零件
     *
     * @param partPo 零件持久化对象
     * @return 影响行数
     */
    int updatePoById(@Param("part") PartPo partPo);

}
