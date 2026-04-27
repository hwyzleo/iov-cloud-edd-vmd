package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.SupplierAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.SupplierDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.SupplierQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Supplier;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.SupplierRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

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
     * @param query 查询 DTO
     * @return 供应商列表
     */
    public List<SupplierDto> search(SupplierQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<Supplier> supplierList = supplierRepository.selectByMap(map);
        return PageUtil.convert(supplierList, SupplierAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查供应商代码是否唯一
     *
     * @param supplierId 供应商ID
     * @param code       供应商代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long supplierId, String code) {
        if (ObjUtil.isNull(supplierId)) {
            supplierId = -1L;
        }
        Supplier supplier = getSupplierByCode(code);
        return !ObjUtil.isNotNull(supplier) || supplier.getId().longValue() == supplierId.longValue();
    }

    /**
     * 根据主键ID获取供应商信息
     *
     * @param id 主键ID
     * @return 供应商 DTO
     */
    public SupplierDto getSupplierById(Long id) {
        return SupplierAssembler.INSTANCE.fromDomain(supplierRepository.selectById(id));
    }

    /**
     * 根据供应商代码获取供应商信息
     *
     * @param code 供应商代码
     * @return 供应商领域对象
     */
    public Supplier getSupplierByCode(String code) {
        return supplierRepository.selectByCode(code);
    }

    /**
     * 新增供应商
     *
     * @param supplierDto 供应商信息 DTO
     * @param userId     操作用户ID
     * @return 结果
     */
    public int createSupplier(SupplierDto supplierDto, String userId) {
        Supplier supplier = SupplierAssembler.INSTANCE.toDomain(supplierDto);
        return supplierRepository.insert(supplier);
    }

    /**
     * 修改供应商
     *
     * @param supplierDto 供应商信息 DTO
     * @param userId     操作用户ID
     * @return 结果
     */
    public int modifySupplier(SupplierDto supplierDto, String userId) {
        Supplier supplier = SupplierAssembler.INSTANCE.toDomain(supplierDto);
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
