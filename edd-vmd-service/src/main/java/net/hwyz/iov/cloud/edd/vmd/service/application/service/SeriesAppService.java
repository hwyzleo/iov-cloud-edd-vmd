package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.SeriesAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.SeriesDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.SeriesQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Series;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSeriesRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.SeriesCmd;

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
public class SeriesAppService {

    private final VehSeriesRepository vehSeriesRepository;
    private final VehModelRepository vehModelRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 查询车系信息
     *
     * @param query 查询 DTO
     * @return 车系列表
     */
    public List<SeriesDto> search(SeriesQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", query.getPlatformCode());
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<Series> seriesList = vehSeriesRepository.selectByMap(map);
        return PageUtil.convert(seriesList, SeriesAssembler.INSTANCE::fromDomain);
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
        Series series = vehSeriesRepository.selectByCode(code);
        return !ObjUtil.isNotNull(series) || series.getId().longValue() == seriesId.longValue();
    }

    /**
     * 检查车系下是否存在车型
     *
     * @param seriesId 车系ID
     * @return 结果
     */
    public Boolean checkSeriesModelExist(Long seriesId) {
        Series series = vehSeriesRepository.selectById(seriesId);
        Map<String, Object> map = new HashMap<>();
        map.put("seriesCode", series.getCode());
        return vehModelRepository.countByMap(map) > 0;
    }

    /**
     * 检查车系下是否存在车辆
     *
     * @param seriesId 车系ID
     * @return 结果
     */
    public Boolean checkSeriesVehicleExist(Long seriesId) {
        Series series = vehSeriesRepository.selectById(seriesId);
        Map<String, Object> map = new HashMap<>();
        map.put("seriesCode", series.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取车系信息
     *
     * @param id 主键ID
     * @return 车系 DTO
     */
    public SeriesDto getSeriesById(Long id) {
        return SeriesAssembler.INSTANCE.fromDomain(vehSeriesRepository.selectById(id));
    }

    /**
     * 根据车系代码获取车系信息
     *
     * @param code 车系代码
     * @return 车系领域对象
     */
    public Series getSeriesByCode(String code) {
        return vehSeriesRepository.selectByCode(code);
    }

    /**
     * 新增车系
     *
     * @param seriesDto 车系信息 DTO
     * @param userId    操作用户ID
     * @return 结果
     */
    public int createSeries(SeriesCmd seriesCmd, String userId) {
        Series series = SeriesAssembler.INSTANCE.toDomain(seriesCmd);
        return vehSeriesRepository.insert(series);
    }

    /**
     * 修改车系
     *
     * @param seriesDto 车系信息 DTO
     * @param userId    操作用户ID
     * @return 结果
     */
    public int modifySeries(SeriesCmd seriesCmd, String userId) {
        Series series = SeriesAssembler.INSTANCE.toDomain(seriesCmd);
        return vehSeriesRepository.update(series);
    }

    /**
     * 批量删除车系
     *
     * @param ids 车系ID数组
     * @return 结果
     */
    public int deleteSeriesByIds(Long[] ids) {
        return vehSeriesRepository.batchPhysicalDelete(ids);
    }

}
