package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehVariantFeatureCodePo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆版本特征值关系表 DAO（原VehBaseModelFeatureCodeMapper，CR-016重命名）
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-08
 */
@Mapper
public interface VehVariantFeatureCodeMapper extends BaseDao<VehVariantFeatureCodePo, Long> {

    /**
     * 根据版本代码和特征族代码查询
     *
     * @param variantCode 版本代码
     * @param familyCode  特征族代码
     * @return 版本特征值关系
     */
    VehVariantFeatureCodePo selectPoByVariantCodeAndFamilyCode(String variantCode, String familyCode);

}