package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ManufacturerVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ManufacturerAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Manufacturer;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehManufacturerRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产厂商应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManufacturerAppService {

    private final VehManufacturerRepository vehManufacturerRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询生产厂商信息
     *
     * @param code      工厂代码
     * @param name      工厂名称
     * @param beginTime 开始时间
     * @param endTime    结束时间
     * @return 生产厂商列表
     */
    public List<ManufacturerVo> search(String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<Manufacturer> manufacturerList = vehManufacturerRepository.selectByMap(map);
        return PageUtil.convert(manufacturerList, ManufacturerAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查工厂代码是否唯一
     *
     * @param manufacturerId 工厂ID
     * @param code           工厂代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long manufacturerId, String code) {
        if (ObjUtil.isNull(manufacturerId)) {
            manufacturerId = -1L;
        }
        Manufacturer manufacturer = getManufacturerByCode(code);
        return !ObjUtil.isNotNull(manufacturer) || manufacturer.getId().longValue() == manufacturerId.longValue();
    }

    /**
     * 检查工厂下是否存在车辆
     *
     * @param manufacturerId 工厂ID
     * @return 结果
     */
    public Boolean checkManufacturerVehicleExist(Long manufacturerId) {
        Manufacturer manufacturer = vehManufacturerRepository.selectById(manufacturerId);
        Map<String, Object> map = new HashMap<>();
        map.put("manufacturerCode", manufacturer.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取生产厂商信息
     *
     * @param id 主键ID
     * @return 生产厂商信息
     */
    public ManufacturerVo getManufacturerById(Long id) {
        return ManufacturerAssembler.INSTANCE.fromDomain(vehManufacturerRepository.selectById(id));
    }

    /**
     * 根据工厂代码获取生产厂商信息
     *
     * @param code 工厂代码
     * @return 生产厂商领域对象
     */
    public Manufacturer getManufacturerByCode(String code) {
        return vehManufacturerRepository.selectByCode(code);
    }

    /**
     * 新增生产厂商
     *
     * @param manufacturerVo 生产厂商信息
     * @param userId         操作用户ID
     * @return 结果
     */
    public int createManufacturer(ManufacturerVo manufacturerVo, String userId) {
        Manufacturer manufacturer = ManufacturerAssembler.INSTANCE.toDomain(manufacturerVo);
        return vehManufacturerRepository.insert(manufacturer);
    }

    /**
     * 修改生产厂商
     *
     * @param manufacturerVo 生产厂商信息
     * @param userId         操作用户ID
     * @return 结果
     */
    public int modifyManufacturer(ManufacturerVo manufacturerVo, String userId) {
        Manufacturer manufacturer = ManufacturerAssembler.INSTANCE.toDomain(manufacturerVo);
        return vehManufacturerRepository.update(manufacturer);
    }

    /**
     * 批量删除生产厂商
     *
     * @param ids 生产厂商ID数组
     * @return 结果
     */
    public int deleteManufacturerByIds(Long[] ids) {
        return vehManufacturerRepository.batchPhysicalDelete(ids);
    }

}
