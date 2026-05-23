package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.cache.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.cache.CacheService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * 缓存服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis Key前缀：车辆
     */
    private static final String REDIS_KEY_PREFIX_VEHICLE = "vmd:vehicle:";

    @Override
    public Optional<Vehicle> getVehicle(String vin) {
        String vehiclePoJson = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX_VEHICLE + vin);
        if (StrUtil.isBlank(vehiclePoJson)) {
            return Optional.empty();
        }
        JSONObject jsonObject = JSONUtil.parseObj(vehiclePoJson);
        return Optional.ofNullable(Vehicle.builder()
                .vin(jsonObject.getStr("vin"))
                .eolTime(jsonObject.get("eolTime", Instant.class))
                .orderNum(jsonObject.getStr("orderNum"))
                .build());
    }

    @Override
    public void setVehicle(Vehicle vehicle) {
        log.debug("设置车辆[{}]领域对象缓存", vehicle.getVin());
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX_VEHICLE + vehicle.getVin(), JSONUtil.parse(vehicle).toJSONString(0));
    }
}
