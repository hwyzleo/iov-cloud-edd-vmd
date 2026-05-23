package net.hwyz.iov.cloud.edd.vmd.service.application.vid.impl;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleDetail;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 车辆信息持久化组件
 * <p>
 * 封装车辆基础信息的 insert/update 和详情的批量插入。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleInfoPersister {

    private final VehBasicInfoRepository vehBasicInfoRepository;

    /**
     * 持久化车辆基础信息和详情
     *
     * @param vehicleBasicInfo 车辆基础信息
     * @param details          车辆详情列表
     * @return true 如果是新车辆（执行了 insert），false 如果是更新
     */
    public boolean persist(VehicleBasicInfo vehicleBasicInfo, List<VehicleDetail> details) {
        boolean isNew = ObjUtil.isNull(vehicleBasicInfo.getId());
        if (isNew) {
            vehBasicInfoRepository.insert(vehicleBasicInfo);
        } else {
            vehBasicInfoRepository.update(vehicleBasicInfo);
        }
        List<VehicleDetail> needInsertDetailList = details.stream()
                .filter(detail -> detail.getId() == null)
                .toList();
        if (!needInsertDetailList.isEmpty()) {
            vehBasicInfoRepository.batchInsertDetail(needInsertDetailList);
        }
        return isNew;
    }
}
