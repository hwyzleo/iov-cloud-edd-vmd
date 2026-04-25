package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehiclePartAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * 解析器基础类
 *
 * @author hwyz_leo
 */
@Slf4j
public class BaseParser {

    @Autowired
    private VehiclePartAppService vehiclePartAppService;

    /**
     * 获取头部分
     *
     * @param dataJson 整体数据JSON对象
     * @return 头部分JSON对象
     */
    protected JSONObject getHeader(JSONObject dataJson) {
        JSONObject request = dataJson.getJSONObject("REQUEST");
        return request.getJSONObject("HEAD");
    }

    /**
     * 获取供应商
     *
     * @param dataJson 整体数据JSON对象
     * @return 供应商代码
     */
    protected String getSupplier(JSONObject dataJson) {
        JSONObject head = getHeader(dataJson);
        String supplier = null;
        if (ObjUtil.isNotNull(head)) {
            supplier = head.getStr("ACCOUNT");
        }
        return supplier;
    }

    /**
     * 获取数据部分
     *
     * @param dataJson 整体数据JSON对象
     * @return 数据部分JSON对象
     */
    protected JSONObject getData(JSONObject dataJson) {
        JSONObject request = dataJson.getJSONObject("REQUEST");
        return request.getJSONObject("DATA");
    }

    /**
     * 处理车辆信息数据
     *
     * @param itemJson      车辆JSON数据
     * @param vehicleInfoDo 车辆信息对象
     * @param jsonKey       解析JSON KEY
     * @param propertyName  对象属性名
     * @param keyDesc       KEY描述
     * @param batchNum      批次号
     * @param vin           车架号
     */
    protected void handleVehicleInfo(JSONObject itemJson, Object vehicleInfoDo, String jsonKey, String propertyName,
                                     String keyDesc, String batchNum, String vin) {
        String keyValue = itemJson.getStr(jsonKey);
        if (StrUtil.isNotBlank(keyValue)) {
            Object fieldValue = BeanUtil.getFieldValue(vehicleInfoDo, propertyName);
            if (ObjUtil.isNull(fieldValue) || StrUtil.isBlank(fieldValue.toString())) {
                BeanUtil.setFieldValue(vehicleInfoDo, propertyName, keyValue.trim().toUpperCase());
            } else if (!keyValue.trim().equalsIgnoreCase(fieldValue.toString())) {
                log.warn("车辆导入数据批次号[{}]车辆[{}]{}[{}]与原数据[{}]不一致", batchNum, vin, keyDesc, keyValue.trim(),
                        fieldValue);
            }
        } else {
            log.warn("车辆导入数据批次号[{}]车辆[{}]{}为空", batchNum, vin, keyDesc);
        }
    }

    /**
     * 处理车辆信息数据
     *
     * @param itemJson         车辆JSON数据
     * @param vehicleDetailMap 车辆详情Map
     * @param jsonKey          解析JSON KEY
     * @param keyDesc          KEY描述
     * @param batchNum         批次号
     * @param vin              车架号
     */
    protected void handleVehicleDetail(JSONObject itemJson, Map<String, VehicleDetail> vehicleDetailMap, String jsonKey,
                                       String keyDesc, String batchNum, String vin) {
        String keyValue = itemJson.getStr(jsonKey);
        if (StrUtil.isNotBlank(keyValue)) {
            VehicleDetail vehicleDetail = vehicleDetailMap.get(jsonKey);
            if (vehicleDetail == null) {
                vehicleDetailMap.put(jsonKey, VehicleDetail.builder()
                        .vin(vin)
                        .type(jsonKey)
                        .val(keyValue)
                        .build());
            } else if (!keyValue.trim().equalsIgnoreCase(vehicleDetail.getVal())) {
                log.warn("车辆导入数据批次号[{}]车辆[{}]{}[{}]与原数据[{}]不一致", batchNum, vin, keyDesc, keyValue.trim(),
                        vehicleDetail.getVal());
            }
        } else {
            log.warn("车辆导入数据批次号[{}]车辆[{}]{}为空", batchNum, vin, keyDesc);
        }
    }

    /**
     * 创建车辆零件
     *
     * @param vehiclePartList 车辆零件列表
     * @return 创建结果
     */
    protected int createVehiclePart(List<VehiclePart> vehiclePartList) {
        return vehiclePartAppService.createVehiclePart(vehiclePartList);
    }

}
