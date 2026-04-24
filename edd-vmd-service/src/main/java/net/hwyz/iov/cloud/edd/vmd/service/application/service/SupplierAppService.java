package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.SupplierVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.SupplierAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.SupplierMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.SupplierPo;
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

    private final SupplierMapper supplierMapper;

    /**
     * 查询供应商信息
     *
     * @param code      供应商代码
     * @param name      供应商名称
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆平台列表
     */
    public List<SupplierVo> search(String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<SupplierPo> supplierPoList = supplierMapper.selectPoByMap(map);
        return PageUtil.convert(supplierPoList, SupplierAssembler.INSTANCE::fromPo);
    }

    /**
     * 检查车辆工厂代码是否唯一
     *
     * @param manufacturerId 车辆工厂ID
     * @param code           车辆工厂代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long manufacturerId, String code) {
        if (ObjUtil.isNull(manufacturerId)) {
            manufacturerId = -1L;
        }
        SupplierPo supplierPo = getManufacturerByCode(code);
        return !ObjUtil.isNotNull(supplierPo) || supplierPo.getId().longValue() == manufacturerId.longValue();
    }

    /**
     * 根据主键ID获取供应商信息
     *
     * @param id 主键ID
     * @return 供应商信息
     */
    public SupplierPo getSupplierById(Long id) {
        return supplierMapper.selectPoById(id);
    }

    /**
     * 根据供应商代码获取供应商信息
     *
     * @param code 供应商代码
     * @return 供应商信息
     */
    public SupplierPo getManufacturerByCode(String code) {
        return supplierMapper.selectPoByCode(code);
    }

    /**
     * 新增供应商
     *
     * @param supplier 供应商信息
     * @return 结果
     */
    public int createSupplier(SupplierPo supplier) {
        return supplierMapper.insertPo(supplier);
    }

    /**
     * 修改供应商
     *
     * @param supplier 供应商信息
     * @return 结果
     */
    public int modifySupplier(SupplierPo supplier) {
        return supplierMapper.updatePo(supplier);
    }

    /**
     * 批量删除供应商
     *
     * @param ids 供应商ID数组
     * @return 结果
     */
    public int deleteSupplierByIds(Long[] ids) {
        return supplierMapper.batchPhysicalDeletePo(ids);
    }

}
