package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmPlantPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆生产工厂表 DAO（原VehManufacturerMapper）
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-23
 */
@Mapper
public interface MdmPlantMapper extends BaseDao<MdmPlantPo, Long> {

    /**
     * 通过code查询车辆工厂信息
     *
     * @param code 车辆工厂编码
     * @return 车辆工厂信息
     */
    MdmPlantPo selectPoByCode(String code);

    /**
     * 通过外部引用ID查询车辆工厂信息
     *
     * @param externalRefId 外部引用ID
     * @return 车辆工厂信息
     */
    MdmPlantPo selectPoByExternalRefId(String externalRefId);

    /**
     * 通过数据来源统计车辆工厂数量
     *
     * @param source 数据来源
     * @return 数量
     */
    int countPoBySource(String source);

}
