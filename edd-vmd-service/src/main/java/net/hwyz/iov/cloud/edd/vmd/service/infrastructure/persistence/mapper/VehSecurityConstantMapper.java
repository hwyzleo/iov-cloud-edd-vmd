package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSecurityConstantPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 车辆安全常量表 DAO
 * 
 * @author hwyz_leo
 * @since 2026-06-17
 */
@Mapper
public interface VehSecurityConstantMapper extends BaseDao<VehSecurityConstantPo, Long> {

    /**
     * 根据车架号查询
     * 
     * @param vin 车架号
     * @return 车辆安全常量
     */
    @Select("SELECT * FROM tb_veh_security_constant WHERE vin = #{vin} AND row_valid = 1")
    VehSecurityConstantPo selectPoByVin(@Param("vin") String vin);

    /**
     * 根据车架号统计数量
     * 
     * @param vin 车架号
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_veh_security_constant WHERE vin = #{vin} AND row_valid = 1")
    long countPoByVin(@Param("vin") String vin);
}
