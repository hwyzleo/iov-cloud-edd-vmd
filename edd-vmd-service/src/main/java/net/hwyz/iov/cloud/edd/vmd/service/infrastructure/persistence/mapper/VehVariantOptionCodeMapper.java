package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehVariantOptionCodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆版本选项值关系表 DAO（原VehBaseModelFeatureCodeMapper→VehVariantFeatureCodeMapper，CR-018重命名）
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-08
 */
@Mapper
public interface VehVariantOptionCodeMapper extends BaseDao<VehVariantOptionCodePo, Long> {

    /**
     * 根据版本代码和选项族代码查询
     *
     * @param variantCode      版本代码
     * @param optionFamilyCode 选项族代码
     * @return 版本选项值关系
     */
    VehVariantOptionCodePo selectPoByVariantCodeAndOptionFamilyCode(String variantCode, String optionFamilyCode);

}
