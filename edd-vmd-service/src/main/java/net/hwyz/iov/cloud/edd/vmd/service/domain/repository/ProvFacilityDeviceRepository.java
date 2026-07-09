package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ProvFacilityDevice;

/**
 * 安全灌注机注册仓储接口
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
public interface ProvFacilityDeviceRepository {

    /**
     * 根据灌注机唯一标识查询
     *
     * @param facilityUid 灌注机唯一标识
     * @return 安全灌注机注册
     */
    ProvFacilityDevice selectByFacilityUid(String facilityUid);

    /**
     * 插入记录
     *
     * @param provFacilityDevice 安全灌注机注册
     * @return 影响行数
     */
    int insert(ProvFacilityDevice provFacilityDevice);

    /**
     * 更新记录
     *
     * @param provFacilityDevice 安全灌注机注册
     * @return 影响行数
     */
    int update(ProvFacilityDevice provFacilityDevice);
}
