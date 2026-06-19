package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleOption;

import java.util.List;

/**
 * 单车选项值快照仓库接口
 *
 * @author VMD-DSN-CR-030 / US-043
 */
public interface VehicleOptionRepository {

    /**
     * 批量插入或更新选项值快照
     * <p>
     * 按 UK(vin, option_family_code) 幂等覆盖
     * </p>
     *
     * @param options 选项值快照列表
     */
    void batchUpsert(List<VehicleOption> options);

    /**
     * 根据 VIN 查询选项值快照
     *
     * @param vin 车辆识别码
     * @return 选项值快照列表
     */
    List<VehicleOption> findByVin(String vin);

    /**
     * 根据 VIN 和选项族编码查询选项值快照
     *
     * @param vin              车辆识别码
     * @param optionFamilyCode 选项族编码
     * @return 选项值快照
     */
    VehicleOption findByVinAndOptionFamilyCode(String vin, String optionFamilyCode);
}
