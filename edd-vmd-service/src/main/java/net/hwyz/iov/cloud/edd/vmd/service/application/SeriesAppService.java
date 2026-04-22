package net.hwyz.iov.cloud.edd.vmd.service.application;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.SeriesVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.SeriesMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.*;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehSeriesDo;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    private final VehModelDao vehModelDao;
    private final VehSeriesDao vehSeriesDao;
    private final VehBasicInfoDao vehBasicInfoDao;
    private final VehBaseModelDao vehBasicModelDao;
    private final VehBuildConfigDao vehBuildConfigDao;

    /**
     * 查询车系信息
     *
     * @param platformCode 车辆平台代码
     * @param code         车系代码
     * @param name         车系名称
     * @param beginTime    开始时间
     * @param endTime      结束时间
     * @return 车系列表
     */
    public List<SeriesVo> search(String platformCode, String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdVehSeriesDo> vmdVehSeriesDoList = vehSeriesDao.selectPoByMap(map);
        return PageUtil.convert(vmdVehSeriesDoList, SeriesMapper.INSTANCE::fromDo);
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
        VmdVehSeriesDo seriesPo = getSeriesByCode(code);
        return !ObjUtil.isNotNull(seriesPo) || seriesPo.getId().longValue() == seriesId.longValue();
    }

    /**
     * 检查车系下是否存在车型
     *
     * @param seriesId 车系ID
     * @return 结果
     */
    public Boolean checkSeriesModelExist(Long seriesId) {
        VmdVehSeriesDo seriesPo = getSeriesById(seriesId);
        Map<String, Object> map = new HashMap<>();
        map.put("seriesCode", seriesPo.getCode());
        return vehModelDao.countPoByMap(map) > 0;
    }

    /**
     * 检查车系下是否存在基础车型
     *
     * @param seriesId 车系ID
     * @return 结果
     */
    public Boolean checkSeriesBasicModelExist(Long seriesId) {
        VmdVehSeriesDo seriesPo = getSeriesById(seriesId);
        Map<String, Object> map = new HashMap<>();
        map.put("seriesCode", seriesPo.getCode());
        return vehBasicModelDao.countPoByMap(map) > 0;
    }

    /**
     * 检查车系下是否存在车型配置
     *
     * @param seriesId 车系ID
     * @return 结果
     */
    public Boolean checkSeriesModelConfigExist(Long seriesId) {
        VmdVehSeriesDo seriesPo = getSeriesById(seriesId);
        Map<String, Object> map = new HashMap<>();
        map.put("seriesCode", seriesPo.getCode());
        return vehBuildConfigDao.countPoByMap(map) > 0;
    }

    /**
     * 检查车系下是否存在车辆
     *
     * @param seriesId 车系ID
     * @return 结果
     */
    public Boolean checkSeriesVehicleExist(Long seriesId) {
        VmdVehSeriesDo seriesPo = getSeriesById(seriesId);
        Map<String, Object> map = new HashMap<>();
        map.put("seriesCode", seriesPo.getCode());
        return vehBasicInfoDao.countPoByMap(map) > 0;
    }

    /**
     * 根据主键ID获取车系信息
     *
     * @param id 主键ID
     * @return 车系信息
     */
    public VmdVehSeriesDo getSeriesById(Long id) {
        return vehSeriesDao.selectPoById(id);
    }

    /**
     * 根据车系代码获取车系信息
     *
     * @param code 车系代码
     * @return 车系信息
     */
    public VmdVehSeriesDo getSeriesByCode(String code) {
        return vehSeriesDao.selectPoByCode(code);
    }

    /**
     * 新增车系
     *
     * @param series 车系信息
     * @return 结果
     */
    public int createSeries(VmdVehSeriesDo series) {
        return vehSeriesDao.insertPo(series);
    }

    /**
     * 修改车系
     *
     * @param series 车系信息
     * @return 结果
     */
    public int modifySeries(VmdVehSeriesDo series) {
        return vehSeriesDao.updatePo(series);
    }

    /**
     * 批量删除车系
     *
     * @param ids 车系ID数组
     * @return 结果
     */
    public int deleteSeriesByIds(Long[] ids) {
        return vehSeriesDao.batchPhysicalDeletePo(ids);
    }

}
