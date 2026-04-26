package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.SupplierVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.SupplierAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Supplier;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.SupplierRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierAppService {

    private final SupplierRepository supplierRepository;

    /**
     * 查询供应商信息
     *
     * @param code      供应商代码
     * @param name      供应商名称
     * @param beginTime 开始时间
     * @param endTime    结束时间
     * @return 供应商列表
     */
    public List<SupplierVo> search(String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<Supplier> supplierList = supplierRepository.selectByMap(map);
        return PageUtil.convert(supplierList, SupplierAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查供应商代码是否唯一
     *
     * @param manufacturerId 供应商ID
     * @param code           供应商代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long manufacturerId, String code) {
        if (ObjUtil.isNull(manufacturerId)) {
            manufacturerId = -1L;
        }
        Supplier supplier = getManufacturerByCode(code);
        return !ObjUtil.isNotNull(supplier) || supplier.getId().longValue() == manufacturerId.longValue();
    }

    /**
     * 根据主键ID获取供应商信息
     *
     * @param id 主键ID
     * @return 供应商信息
     */
    public SupplierVo getSupplierById(Long id) {
        return SupplierAssembler.INSTANCE.fromDomain(supplierRepository.selectById(id));
    }

    /**
     * 根据供应商代码获取供应商信息
     *
     * @param code 供应商代码
     * @return 供应商领域对象
     */
    public Supplier getManufacturerByCode(String code) {
        return supplierRepository.selectByCode(code);
    }

    /**
     * 新增供应商
     *
     * @param supplierVo 供应商信息
     * @param userId     操作用户ID
     * @return 结果
     */
    public int createSupplier(SupplierVo supplierVo, String userId) {
        Supplier supplier = SupplierAssembler.INSTANCE.toDomain(supplierVo);
        return supplierRepository.insert(supplier);
    }

    /**
     * 修改供应商
     *
     * @param supplierVo 供应商信息
     * @param userId     操作用户ID
     * @return 结果
     */
    public int modifySupplier(SupplierVo supplierVo, String userId) {
        Supplier supplier = SupplierAssembler.INSTANCE.toDomain(supplierVo);
        return supplierRepository.update(supplier);
    }

    /**
     * 批量删除供应商
     *
     * @param ids 供应商ID数组
     * @return 结果
     */
    public int deleteSupplierByIds(Long[] ids) {
        return supplierRepository.batchPhysicalDelete(ids);
    }

}
