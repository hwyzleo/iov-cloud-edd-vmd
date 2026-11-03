package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmCarLinePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 车辆车系表 DAO
 * </p>
 *
 * <p>VMD-DSN-CR-075：由 legacy BaseDao 迁移为原生 BaseMapper。
 * 基础查询（selectById 等）由 MP 注入；因时间来源（DB now()）与审计副作用裁决，
 * INSERT/UPDATE/逻辑删除/物理删除保留明确命名的具名 SQL；自定义查询保留 XML/注解。</p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface MdmCarLineMapper extends BaseMapper<MdmCarLinePo> {

    /**
     * 插入车系（专用 SQL：create_time/modify_time 由 DB now() 生成，row_version=1、row_valid=1）
     *
     * @param po 车系 PO
     * @return 影响行数
     */
    int insertPo(MdmCarLinePo po);

    /**
     * 更新车系（专用 SQL：保持旧字段语义 + 审计副作用，自 CR-075 起带 row_version CAS）
     *
     * @param po 车系 PO（必须携带可信原 rowVersion）
     * @return 影响行数（0 表示 CAS 冲突或记录不存在）
     */
    int updatePo(MdmCarLinePo po);

    /**
     * 逻辑删除车系（专用 SQL：保留 row_valid=0、版本递增、修改时间等审计副作用）
     *
     * @param id 主键
     * @return 影响行数
     */
    int logicalDeletePo(Long id);

    /**
     * 物理删除车系（受控硬删除，保留授权/引用校验边界）
     *
     * @param id 主键
     * @return 影响行数
     */
    int physicalDeletePo(Long id);

    /**
     * 批量物理删除车系（受控硬删除，固定 Long[] 参数绑定）
     *
     * @param ids 主键数组
     * @return 影响行数
     */
    int batchPhysicalDeletePo(Long[] ids);

    /**
     * 根据条件查询车系列表（保留 XML：模糊名称、时间范围、排序）
     *
     * @param map 查询条件
     * @return 车系 PO 列表
     */
    List<MdmCarLinePo> selectPoByMap(Map<String, Object> map);

    /**
     * 根据条件统计车系数量（保留 XML）
     *
     * @param map 查询条件
     * @return 数量
     */
    int countPoByMap(Map<String, Object> map);

    /**
     * 通过code查询车辆车系信息
     *
     * @param code 车辆车系编码
     * @return 车辆车系信息
     */
    MdmCarLinePo selectPoByCode(String code);

    /**
     * 通过MDM外部引用ID查询车辆车系信息
     *
     * @param externalRefId MDM外部引用ID
     * @return 车辆车系信息
     */
    @Select("SELECT * FROM tb_mdm_car_line WHERE external_ref_id = #{externalRefId} AND row_valid = 1")
    MdmCarLinePo selectPoByExternalRefId(@Param("externalRefId") String externalRefId);

    /**
     * 根据数据来源统计车系数量
     *
     * @param source 数据来源
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM tb_mdm_car_line WHERE source = #{source} AND row_valid = 1")
    long countPoBySource(@Param("source") String source);

}
