package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleOption;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleOptionRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleOptionConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehicleOptionMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleOptionPo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单车选项值快照仓库实现
 *
 * @author VMD-DSN-CR-030 / US-043
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleOptionRepositoryImpl implements VehicleOptionRepository {

    private final VehicleOptionMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpsert(List<VehicleOption> options) {
        for (VehicleOption option : options) {
            VehicleOptionPo po = VehicleOptionConverter.INSTANCE.fromDomain(option);
            mapper.insertOrUpdate(po);
            log.debug("Upsert vehicle option: vin={}, family={}, code={}",
                option.getVin(), option.getOptionFamilyCode(), option.getOptionCode());
        }
    }

    @Override
    public List<VehicleOption> findByVin(String vin) {
        Map<String, Object> map = new HashMap<>();
        map.put("vin", vin);
        List<VehicleOptionPo> poList = mapper.selectPoByMap(map);
        return VehicleOptionConverter.INSTANCE.toDomainList(poList);
    }

    @Override
    public VehicleOption findByVinAndOptionFamilyCode(String vin, String optionFamilyCode) {
        VehicleOptionPo example = VehicleOptionPo.builder()
            .vin(vin)
            .optionFamilyCode(optionFamilyCode)
            .build();
        List<VehicleOptionPo> poList = mapper.selectPoByExample(example);
        if (poList != null && !poList.isEmpty()) {
            return VehicleOptionConverter.INSTANCE.toDomain(poList.get(0));
        }
        return null;
    }

    @Override
    public int physicalDeleteByVin(String vin) {
        return mapper.physicalDeleteByVin(vin);
    }
}
