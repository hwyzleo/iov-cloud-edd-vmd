package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleImportDataVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.VehicleImportDataAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.ImportDataParser;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleImportDataException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehImportDataMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportDataPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆导入数据应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleImportDataAppService {

    private final ApplicationContext ctx;
    private final VehImportDataMapper vehImportDataMapper;

    /**
     * 查询车辆导入数据
     *
     * @param batchNum  批次号
     * @param type      数据类型
     * @param version   数据版本
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 车辆导入数据列表
     */
    public List<VehicleImportDataVo> search(String batchNum, String type, String version, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("batchNum", batchNum);
        map.put("type", type);
        map.put("version", version);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VehImportDataPo> vehImportDataPoList = vehImportDataMapper.selectPoByMap(map);
        return PageUtil.convert(vehImportDataPoList, VehicleImportDataAssembler.INSTANCE::fromPo);
    }

    /**
     * 检查批次号是否唯一
     *
     * @param vehicleImportDataId 车辆导入数据ID
     * @param batchNum            批次号
     * @return 结果
     */
    public Boolean checkBatchNumUnique(Long vehicleImportDataId, String batchNum) {
        if (ObjUtil.isNull(vehicleImportDataId)) {
            vehicleImportDataId = -1L;
        }
        VehImportDataPo vehImportDataPo = getVehicleImportDataByBatchNum(batchNum);
        return !ObjUtil.isNotNull(vehImportDataPo) || vehImportDataPo.getId().longValue() == vehicleImportDataId.longValue();
    }

    /**
     * 根据主键ID获取车辆导入数据
     *
     * @param id 主键ID
     * @return 车辆导入数据
     */
    public VehImportDataPo getVehicleImportDataById(Long id) {
        return vehImportDataMapper.selectPoById(id);
    }

    /**
     * 根据批次号获取车辆导入数据
     *
     * @param batchNum 批次号
     * @return 车辆导入数据
     */
    public VehImportDataPo getVehicleImportDataByBatchNum(String batchNum) {
        return vehImportDataMapper.selectPoByBatchNum(batchNum);
    }

    /**
     * 新增车辆导入数据
     *
     * @param vehicleImportData 车辆导入数据
     * @return 结果
     */
    public int createVehicleImportData(VehImportDataPo vehicleImportData) {
        vehicleImportData.setHandle(false);
        return vehImportDataMapper.insertPo(vehicleImportData);
    }

    /**
     * 修改车辆导入数据
     *
     * @param vehicleImportData 车辆导入数据
     * @return 结果
     */
    public int modifyVehicleImportData(VehImportDataPo vehicleImportData) {
        return vehImportDataMapper.updatePo(vehicleImportData);
    }

    /**
     * 批量删除车辆导入数据
     *
     * @param ids 车辆导入数据ID数组
     * @return 结果
     */
    public int deleteVehicleImportDataByIds(Long[] ids) {
        return vehImportDataMapper.batchPhysicalDeletePo(ids);
    }

    /**
     * 解析车辆导入数据
     *
     * @param batchNum 批次号
     */
    public void parseVehicleImportData(String batchNum) {
        VehImportDataPo vehImportDataPo = vehImportDataMapper.selectPoByBatchNum(batchNum);
        if (ObjUtil.isNull(vehImportDataPo)) {
            throw new VehicleImportDataException(batchNum, "车辆导入数据批次号不存在");
        }
        if (vehImportDataPo.getHandle()) {
            throw new VehicleImportDataException(batchNum, "车辆导入数据批次号已处理");
        }
        String dataStr = vehImportDataPo.getData();
        if (StrUtil.isBlank(dataStr)) {
            throw new VehicleImportDataException(batchNum, "车辆导入数据数据为空");
        }
        String type = vehImportDataPo.getType();
        if (StrUtil.isBlank(type)) {
            throw new VehicleImportDataException(batchNum, "车辆导入数据数据类型为空");
        }
        String version = vehImportDataPo.getVersion();
        if (StrUtil.isBlank(version)) {
            throw new VehicleImportDataException(batchNum, "车辆导入数据版本为空");
        }
        JSONObject dataJson = JSONUtil.parseObj(dataStr);
        vehImportDataPo.setHandle(true);
        vehImportDataPo.setDescription("");
        try {
            ImportDataParser parser = getParser(type, version);
            if (ObjUtil.isNull(parser)) {
                throw new VehicleImportDataException(batchNum, "没找到数据类型[" + type + "]版本[" + version + "]对应的解析器");
            }
            parser.parse(batchNum, dataJson);
        } catch (Exception e) {
            vehImportDataPo.setHandle(false);
            vehImportDataPo.setDescription("车辆导入数据批次号[" + batchNum + "]处理失败:" + e.getMessage());
            vehImportDataMapper.updatePo(vehImportDataPo);
            log.warn("车辆导入数据批次号[" + batchNum + "]处理失败:" + e.getMessage(), e);
            throw new VehicleImportDataException(batchNum, e.getMessage());
        }
        vehImportDataMapper.updatePo(vehImportDataPo);
    }

    /**
     * 获取解析器
     *
     * @param dataType    数据类型
     * @param dataVersion 数据版本
     * @return 数据解析器
     */
    private ImportDataParser getParser(String dataType, String dataVersion) {
        return ctx.getBean(dataType.toLowerCase() + "DataParserV" + dataVersion, ImportDataParser.class);
    }

}
