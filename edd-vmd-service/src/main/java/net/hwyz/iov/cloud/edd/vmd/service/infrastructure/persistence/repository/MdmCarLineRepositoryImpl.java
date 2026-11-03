package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.CarLineConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmCarLineMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmCarLinePo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车系数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MdmCarLineRepositoryImpl implements MdmCarLineRepository {

    private final MdmCarLineMapper mdmCarLineMapper;

    @Override
    public List<CarLine> selectByMap(Map<String, Object> map) {
        List<MdmCarLinePo> mdmCarLinePoList = mdmCarLineMapper.selectPoByMap(map);
        return PageUtil.convert(mdmCarLinePoList, CarLineConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return mdmCarLineMapper.countPoByMap(map);
    }

    @Override
    public CarLine selectById(Long id) {
        // 原生 BaseMapper.selectById（@TableLogic 自动过滤 row_valid=1，与旧 selectPoById 等价）
        return CarLineConverter.INSTANCE.toDomain(mdmCarLineMapper.selectById(id));
    }

    @Override
    public CarLine selectByCode(String code) {
        return CarLineConverter.INSTANCE.toDomain(mdmCarLineMapper.selectPoByCode(code));
    }

    @Override
    public CarLine selectByExternalRefId(String externalRefId) {
        return CarLineConverter.INSTANCE.toDomain(mdmCarLineMapper.selectPoByExternalRefId(externalRefId));
    }

    @Override
    public long countBySource(SourceType source) {
        return mdmCarLineMapper.countPoBySource(source.getValue());
    }

    @Override
    public int insert(CarLine carLine) {
        MdmCarLinePo po = CarLineConverter.INSTANCE.fromDomain(carLine);
        int result = mdmCarLineMapper.insertPo(po);
        if (result > 0) {
            carLine.setId(po.getId());
            // insertPo 专用 SQL 固定写入 row_version=1（事实性回填，非自动补版本）
            carLine.setRowVersion(1);
        }
        return result;
    }

    @Override
    public int update(CarLine carLine) {
        return updateWithCas(carLine);
    }

    @Override
    public int updateById(CarLine carLine) {
        return updateWithCas(carLine);
    }

    /**
     * CAS 更新：必须携带可信原版本；成功回传新版本（DB 固定 +1），失败（0 行）不修改内存版本。
     */
    private int updateWithCas(CarLine carLine) {
        MdmCarLinePo po = CarLineConverter.INSTANCE.fromDomain(carLine);
        int result = mdmCarLineMapper.updatePo(po);
        if (result > 0 && po.getRowVersion() != null) {
            carLine.setRowVersion(po.getRowVersion() + 1);
        }
        return result;
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        // 空集合明确 no-op，不生成 IN () 非法 SQL，也不触发全表删除（CR-075 受控硬删除边界）
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return mdmCarLineMapper.batchPhysicalDeletePo(ids);
    }

}
