package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ProvFacilityDevicePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 安全灌注机注册表 DAO
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Mapper
public interface ProvFacilityDeviceMapper extends BaseDao<ProvFacilityDevicePo, Long> {

    /**
     * 根据灌注机唯一标识查询
     *
     * @param facilityUid 灌注机唯一标识
     * @return 安全灌注机注册
     */
    @Select("SELECT * FROM tb_prov_facility_device WHERE facility_uid = #{facilityUid} AND row_valid = 1")
    ProvFacilityDevicePo selectPoByFacilityUid(@Param("facilityUid") String facilityUid);
}
