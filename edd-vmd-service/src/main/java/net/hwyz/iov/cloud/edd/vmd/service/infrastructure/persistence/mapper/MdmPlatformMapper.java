package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmPlatformPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 车辆平台表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface MdmPlatformMapper extends BaseDao<MdmPlatformPo, Long> {

    /**
     * 通过code查询车辆平台信息
     *
     * @param code 车辆平台编码
     * @return 车辆平台信息
     */
    MdmPlatformPo selectPoByCode(String code);

    /**
     * 通过MDM外部引用ID查询车辆平台信息
     *
     * @param externalRefId MDM外部引用ID
     * @return 车辆平台信息
     */
    @Select("SELECT * FROM tb_mdm_platform WHERE external_ref_id = #{externalRefId} AND row_valid = 1")
    MdmPlatformPo selectPoByExternalRefId(@Param("externalRefId") String externalRefId);

    /**
     * 根据数据来源统计平台数量
     *
     * @param source 数据来源
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_mdm_platform WHERE source = #{source} AND row_valid = 1")
    long countPoBySource(@Param("source") String source);

}
