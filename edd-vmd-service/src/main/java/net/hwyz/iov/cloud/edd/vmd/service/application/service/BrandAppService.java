package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BrandVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BrandAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSeriesRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 品牌应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrandAppService {

    private final VehBrandRepository vehBrandRepository;
    private final VehSeriesRepository vehSeriesRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询品牌信息
     *
     * @param code      品牌代码
     * @param name      品牌名称
     * @param beginTime 开始时间
     * @param endTime    结束时间
     * @return 品牌列表
     */
    public List<BrandVo> search(String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
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
        return vehSeriesRepository.countByMap(map) > 0;
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
     * @return 品牌信息
     */
    public BrandVo getBrandById(Long id) {
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
     * 新增品牌
     *
     * @param brandVo 品牌信息
     * @param userId  操作用户ID
     * @return 结果
     */
    public int createBrand(BrandVo brandVo, String userId) {
        Brand brand = BrandAssembler.INSTANCE.toDomain(brandVo);
        return vehBrandRepository.insert(brand);
    }

    /**
     * 修改品牌
     *
     * @param brandVo 品牌信息
     * @param userId  操作用户ID
     * @return 结果
     */
    public int modifyBrand(BrandVo brandVo, String userId) {
        Brand brand = BrandAssembler.INSTANCE.toDomain(brandVo);
        return vehBrandRepository.update(brand);
    }

    /**
     * 批量删除品牌
     *
     * @param ids 品牌ID数组
     * @return 结果
     */
    public int deleteBrandByIds(Long[] ids) {
        return vehBrandRepository.batchPhysicalDelete(ids);
    }

}
