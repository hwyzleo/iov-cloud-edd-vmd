package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleOptionPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 单车选项值快照表 DAO
 * </p>
 *
 * @author VMD-DSN-CR-030 / US-043
 * @since 2026-06-19
 */
@Mapper
public interface VehicleOptionMapper extends BaseDao<VehicleOptionPo, Long> {

    /**
     * 插入或更新单车选项值快照记录
     * 使用 ON DUPLICATE KEY UPDATE 实现原子操作，避免并发插入时的唯一键冲突
     *
     * @param po 持久化对象
     * @return 影响行数
     */
    @Insert("INSERT INTO tb_vehicle_option (vin, option_family_code, option_code, source, batch_num, snapshot_time) " +
            "VALUES (#{vin}, #{optionFamilyCode}, #{optionCode}, #{source}, #{batchNum}, #{snapshotTime}) " +
            "ON DUPLICATE KEY UPDATE option_code = VALUES(option_code), source = VALUES(source), batch_num = VALUES(batch_num), snapshot_time = VALUES(snapshot_time)")
    int insertOrUpdate(VehicleOptionPo po);

}
