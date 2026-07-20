package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartInfoPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 物理零件实例本体表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-06-10
 */
@Mapper
public interface PartInfoMapper extends BaseDao<PartInfoPo, Long> {

    /**
     * 根据零件编码和序列号查询
     *
     * @param partCode 零件编码
     * @param sn 序列号
     * @return 零件实例信息
     */
    PartInfoPo selectPoByPartCodeAndSn(String partCode, String sn);

    /**
     * 根据序列号查询
     *
     * @param sn 序列号
     * @return 零件实例信息
     */
    PartInfoPo selectPoBySn(String sn);

}
