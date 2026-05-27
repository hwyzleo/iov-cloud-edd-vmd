package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.CarLineAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CarLineDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.CarLineQuery;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ProductDataReadOnlyException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehCarLineRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CarLineCmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车系应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarLineAppService {

    private final VehCarLineRepository vehCarLineRepository;
    private final VehModelRepository vehModelRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询车系信息
     *
     * @param query 查询 DTO
     * @return 车系列表
     */
    public List<CarLineDto> search(CarLineQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("brandCode", query.getBrandCode());
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<CarLine> carLineList = vehCarLineRepository.selectByMap(map);
        return PageUtil.convert(carLineList, CarLineAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查车系代码是否唯一
     *
     * @param seriesId 车系ID
     * @param code     车系代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long seriesId, String code) {
        if (ObjUtil.isNull(seriesId)) {
            seriesId = -1L;
        }
        CarLine carLine = vehCarLineRepository.selectByCode(code);
        return !ObjUtil.isNotNull(carLine) || carLine.getId().longValue() == seriesId.longValue();
    }

    /**
     * 检查车系下是否存在车型
     *
     * @param seriesId 车系ID
     * @return 结果
     */
    public Boolean checkSeriesModelExist(Long seriesId) {
        CarLine carLine = vehCarLineRepository.selectById(seriesId);
        Map<String, Object> map = new HashMap<>();
        map.put("carLineCode", carLine.getCode());
        return vehModelRepository.countByMap(map) > 0;
    }

    /**
     * 检查车系下是否存在车辆
     *
     * @param seriesId 车系ID
     * @return 结果
     */
    public Boolean checkSeriesVehicleExist(Long seriesId) {
        CarLine carLine = vehCarLineRepository.selectById(seriesId);
        Map<String, Object> map = new HashMap<>();
        map.put("carLineCode", carLine.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取车系信息
     *
     * @param id 主键ID
     * @return 车系 DTO
     */
    public CarLineDto getSeriesById(Long id) {
        return CarLineAssembler.INSTANCE.fromDomain(vehCarLineRepository.selectById(id));
    }

    /**
     * 根据车系代码获取车系信息
     *
     * @param code 车系代码
     * @return 车系领域对象
     */
    public CarLine getSeriesByCode(String code) {
        return vehCarLineRepository.selectByCode(code);
    }

    /**
     * 新增车系
     *
     * @param seriesDto 车系信息 DTO
     * @param userId    操作用户ID
     * @return 结果
     */
    public int createSeries(CarLineCmd seriesCmd, String userId) {
        CarLine carLine = CarLineAssembler.INSTANCE.toDomain(seriesCmd);
        // 检查是否为 MDM 来源数据
        if (carLine.getSource() == SourceType.MDM) {
            throw new ProductDataReadOnlyException("车系", carLine.getCode());
        }
        return vehCarLineRepository.insert(carLine);
    }

    /**
     * 修改车系
     *
     * @param seriesDto 车系信息 DTO
     * @param userId    操作用户ID
     * @return 结果
     */
    public int modifySeries(CarLineCmd seriesCmd, String userId) {
        CarLine carLine = vehCarLineRepository.selectById(seriesCmd.getId());
        // 检查是否为 MDM 来源数据
        if (carLine.getSource() == SourceType.MDM) {
            throw new ProductDataReadOnlyException("车系", carLine.getCode());
        }
        CarLine updateCarLine = CarLineAssembler.INSTANCE.toDomain(seriesCmd);
        return vehCarLineRepository.update(updateCarLine);
    }

    /**
     * 批量删除车系
     *
     * @param ids 车系ID数组
     * @return 结果
     */
    public int deleteSeriesByIds(Long[] ids) {
        // 检查是否为 MDM 来源数据
        for (Long id : ids) {
            CarLine carLine = vehCarLineRepository.selectById(id);
            if (carLine != null && carLine.getSource() == SourceType.MDM) {
                throw new ProductDataReadOnlyException("车系", carLine.getCode());
            }
        }
        return vehCarLineRepository.batchPhysicalDelete(ids);
    }

}
