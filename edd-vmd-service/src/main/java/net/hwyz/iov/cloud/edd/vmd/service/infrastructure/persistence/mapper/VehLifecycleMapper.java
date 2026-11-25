package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehLifecyclePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;

/**
 * <p>
 * 车辆生命周期表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface VehLifecycleMapper extends BaseDao<VehLifecyclePo, Long> {

    /**
     * 根据vin批量物理删除车辆生命周期
     *
     * @param vin 车架号
     * @return 影响行数
     */
    int batchPhysicalDeletePoByVin(String vin);

    /**
     * 根据车架号与生命周期节点查询单条记录（VMD-DSN-CR-079 / D079-02）
     *
     * @param vin  车架号
     * @param node 生命周期节点
     * @return 生命周期记录（可能为 null）
     */
    @Select("SELECT * FROM tb_veh_lifecycle WHERE vin = #{vin} AND node = #{node} AND row_valid = 1 LIMIT 1")
    VehLifecyclePo selectPoByVinAndNode(@Param("vin") String vin, @Param("node") String node);

    /**
     * 仅向前纠正触达时间（VMD-DSN-CR-079 / D079-03）
     * <p>
     * 条件更新在数据库层保证「只前不后」：既有 {@code reach_time} 为空（无更早证据）或严格晚于目标时间时才更新；
     * 更早/相等时不更新，由调用方按已收敛（{@code DUPLICATE}）处理。
     *
     * @param vin       车架号
     * @param node      生命周期节点
     * @param reachTime 目标触达时间
     * @return 影响行数
     */
    @Update("UPDATE tb_veh_lifecycle SET reach_time = #{reachTime}, modify_time = CURRENT_TIMESTAMP, "
            + "row_version = row_version + 1 "
            + "WHERE vin = #{vin} AND node = #{node} AND row_valid = 1 "
            + "AND (reach_time IS NULL OR reach_time > #{reachTime})")
    int correctReachTimeIfLaterPo(@Param("vin") String vin, @Param("node") String node,
                                  @Param("reachTime") Instant reachTime);

}
