package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.vid.DownstreamProcessor;
import org.springframework.stereotype.Component;

/**
 * IDK下游处理器
 * 处理IDK相关的下游联动
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdkDownstreamProcessor implements DownstreamProcessor {

    @Override
    public void process(String batchNum, String partCode, String vehicleNodeCode, JSONObject data) {
        log.info("IDK处理器处理零件导入下游联动, batchNum={}, partCode={}, vehicleNodeCode={}", 
                batchNum, partCode, vehicleNodeCode);
        
        // IDK相关处理逻辑
        // 根据实际IDK服务接口实现
        
        log.info("IDK处理器处理完成, batchNum={}, partCode={}", batchNum, partCode);
    }

    @Override
    public String getSupportedVehicleNodeCode() {
        return "IDK";
    }
}
