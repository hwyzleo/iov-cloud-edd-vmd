package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ModelConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmModelMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmModelPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车型数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MdmModelRepositoryImpl implements MdmModelRepository {

    private final MdmModelMapper mdmModelMapper;

    @Override
    public List<Model> selectByMap(Map<String, Object> map) {
        List<MdmModelPo> poList = mdmModelMapper.selectPoByMap(map);
        return PageUtil.convert(poList, ModelConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return mdmModelMapper.countPoByMap(map);
    }

    @Override
    public Model selectById(Long id) {
        // 原生 BaseMapper.selectById（@TableLogic 自动过滤 row_valid=1，与旧 selectPoById 等价）
        return ModelConverter.INSTANCE.toDomain(mdmModelMapper.selectById(id));
    }

    @Override
    public Model selectByCode(String code) {
        return ModelConverter.INSTANCE.toDomain(mdmModelMapper.selectPoByCode(code));
    }

    @Override
    public Model selectByExternalRefId(String externalRefId) {
        return ModelConverter.INSTANCE.toDomain(mdmModelMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return mdmModelMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(Model model) {
        MdmModelPo po = ModelConverter.INSTANCE.fromDomain(model);
        int result = mdmModelMapper.insertPo(po);
        if (result > 0) {
            model.setId(po.getId());
            // insertPo 专用 SQL 固定写入 row_version=1（事实性回填，非自动补版本）
            model.setRowVersion(1);
        }
        return result;
    }

    @Override
    public int update(Model model) {
        return updateWithCas(model);
    }

    @Override
    public int updateById(Model model) {
        return updateWithCas(model);
    }

    /**
     * CAS 更新：必须携带可信原版本；成功回传新版本（DB 固定 +1），失败（0 行）不修改内存版本。
     */
    private int updateWithCas(Model model) {
        MdmModelPo po = ModelConverter.INSTANCE.fromDomain(model);
        int result = mdmModelMapper.updatePo(po);
        if (result > 0 && po.getRowVersion() != null) {
            model.setRowVersion(po.getRowVersion() + 1);
        }
        return result;
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        // 空集合明确 no-op，不生成 IN () 非法 SQL，也不触发全表删除（CR-075 受控硬删除边界）
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return mdmModelMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public int logicalDeleteById(Long id) {
        return mdmModelMapper.logicalDeletePo(id);
    }

}
