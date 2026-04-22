package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.cache.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeState;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.Qrcode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.cache.CacheService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
// @Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis Key前缀：二维码
     */
    private static final String REDIS_KEY_PREFIX_QRCODE = "vmd:qrcode:";
    /**
     * Redis Key前缀：车辆
     */
    private static final String REDIS_KEY_PREFIX_VEHICLE = "vmd:vehicle:";

    @Override
    public Optional<Vehicle> getVehicle(String vin) {
        String vehicleDoJson = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX_VEHICLE + vin);
        if (StrUtil.isBlank(vehicleDoJson)) {
            return Optional.empty();
        }
        JSONObject jsonObject = JSONUtil.parseObj(vehicleDoJson);
        return Optional.ofNullable(Vehicle.builder()
                .vin(jsonObject.getStr("vin"))
                .eolTime(jsonObject.getDate("eolTime"))
                .orderNum(jsonObject.getStr("orderNum"))
                .build());
    }

    @Override
    public void setVehicle(Vehicle vehicle) {
        log.debug("设置车辆[{}]领域对象缓存", vehicle.getVin());
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX_VEHICLE + vehicle.getVin(), JSONUtil.parse(vehicle).toJSONString(0));
    }

    @Override
    public Optional<Qrcode> getQrcode(String qrcode) {
        String qrcodeDoJson = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX_QRCODE + qrcode);
        if (StrUtil.isBlank(qrcodeDoJson)) {
            return Optional.empty();
        }
        JSONObject jsonObject = JSONUtil.parseObj(qrcodeDoJson);
        return Optional.ofNullable(Qrcode.builder()
                .vin(jsonObject.getStr("vin"))
                .sn(jsonObject.getStr("sn"))
                .qrcode(jsonObject.getStr("qrcode"))
                .type(QrcodeType.valOf(jsonObject.getStr("type")))
                .qrcodeState(QrcodeState.valOf(jsonObject.getStr("qrcodeState")))
                .createTime(new Date(jsonObject.getLong("createTime")))
                .build());
    }

    @Override
    public void setQrcode(Qrcode qrcode) {
        log.debug("设置车辆[{}]二维码[{}]领域对象缓存", qrcode.getVin(), qrcode.getType());
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX_QRCODE + qrcode.getQrcode(),
                JSONUtil.parse(qrcode).toJSONString(0), 1, TimeUnit.HOURS);
    }
}
