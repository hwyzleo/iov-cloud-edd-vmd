package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BrandAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BrandCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.BrandQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BrandDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehCarLineRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 品牌应用服务类
 * 
 * <p>CR-012 语义重构：Brand 自 CR-012 起定位为 MDM Brand 主数据本地只读投影的消费方。</p>
 * 
 * <p>查询能力（search/getBrandById/getBrandByCode）：长期保留</p>
 * <p>写入能力（createBrand/modifyBrand/deleteBrandByIds）：兼容期遗留，仅可作用于 source=MANUAL 过渡数据</p>
 * 
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandAppService {

    private final VehBrandRepository vehBrandRepository;
    private final VehCarLineRepository vehCarLineRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询品牌信息
     *
     * @param query 查询 DTO
     * @return 品牌列表
     */
    public List<BrandDto> search(BrandQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<Brand> brandList = vehBrandRepository.selectByMap(map);
        return PageUtil.convert(brandList, BrandAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查品牌代码是否唯一
     *
     * @param brandId 品牌ID
     * @param code    品牌代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long brandId, String code) {
        if (ObjUtil.isNull(brandId)) {
            brandId = -1L;
        }
        Brand brand = getBrandByCode(code);
        return !ObjUtil.isNotNull(brand) || brand.getId().longValue() == brandId.longValue();
    }

    /**
     * 检查品牌下是否存在车系
     *
     * @param brandId 品牌ID
     * @return 结果
     */
    public Boolean checkBrandSeriesExist(Long brandId) {
        Brand brand = vehBrandRepository.selectById(brandId);
        Map<String, Object> map = new HashMap<>();
        map.put("brandCode", brand.getCode());
        return vehCarLineRepository.countByMap(map) > 0;
    }

    /**
     * 检查品牌下是否存在车辆
     *
     * @param brandId 品牌ID
     * @return 结果
     */
    public Boolean checkBrandVehicleExist(Long brandId) {
        Brand brand = vehBrandRepository.selectById(brandId);
        Map<String, Object> map = new HashMap<>();
        map.put("brandCode", brand.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取品牌信息
     *
     * @param id 主键ID
     * @return 品牌 DTO
     */
    public BrandDto getBrandById(Long id) {
        return BrandAssembler.INSTANCE.fromDomain(vehBrandRepository.selectById(id));
    }

    /**
     * 根据品牌代码获取品牌信息
     *
     * @param code 品牌代码
     * @return 品牌领域对象
     */
    public Brand getBrandByCode(String code) {
        return vehBrandRepository.selectByCode(code);
    }

    /**
     * 新增品牌（兼容期遗留能力）
     * 
     * <p>仅可作用于 source=MANUAL 过渡数据。对 source=MDM 记录，
     * 请通过 MDM 事件订阅或 Bootstrap 同步。</p>
     * 
     * @deprecated CR-012 后 Brand 定位为 MDM 只读投影，此方法仅保留兼容性。
     *             最终下线由后续兼容性清理 CR 完成。
     * @param brandCmd 品牌信息命令
     * @param userId  操作用户ID
     * @return 结果
     */
    @Deprecated
    public int createBrand(BrandCmd brandCmd, String userId) {
        Brand brand = BrandAssembler.INSTANCE.toDomain(brandCmd);
        // Source=MDM 只读限制（CR-012）
        if (brand.getSource() == SourceType.MDM) {
            throw new ProductDataReadOnlyException("品牌", brand.getCode());
        }
        return vehBrandRepository.insert(brand);
    }

    /**
     * 修改品牌（兼容期遗留能力）
     * 
     * <p>仅可作用于 source=MANUAL 过渡数据。对 source=MDM 记录，
     * 请通过 MDM 事件订阅或 Bootstrap 同步。</p>
     * 
     * @deprecated CR-012 后 Brand 定位为 MDM 只读投影，此方法仅保留兼容性。
     *             最终下线由后续兼容性清理 CR 完成。
     * @param brandCmd 品牌信息命令
     * @param userId  操作用户ID
     * @return 结果
     */
    @Deprecated
    public int modifyBrand(BrandCmd brandCmd, String userId) {
        Brand brand = vehBrandRepository.selectById(brandCmd.getId());
        // Source=MDM 只读限制（CR-012）
        if (brand.getSource() == SourceType.MDM) {
            throw new ProductDataReadOnlyException("品牌", brand.getCode());
        }
        Brand updateBrand = BrandAssembler.INSTANCE.toDomain(brandCmd);
        return vehBrandRepository.update(updateBrand);
    }

    /**
     * 批量删除品牌（兼容期遗留能力）
     * 
     * <p>仅可作用于 source=MANUAL 过渡数据。对 source=MDM 记录，
     * 请通过 MDM 事件订阅或 Bootstrap 同步。</p>
     * 
     * @deprecated CR-012 后 Brand 定位为 MDM 只读投影，此方法仅保留兼容性。
     *             最终下线由后续兼容性清理 CR 完成。
     * @param ids 品牌ID数组
     * @return 结果
     */
    @Deprecated
    public int deleteBrandByIds(Long[] ids) {
        // Source=MDM 只读限制（CR-012）
        for (Long id : ids) {
            Brand brand = vehBrandRepository.selectById(id);
            if (brand != null && brand.getSource() == SourceType.MDM) {
                throw new ProductDataReadOnlyException("品牌", brand.getCode());
            }
        }
        return vehBrandRepository.batchPhysicalDelete(ids);
    }

}
