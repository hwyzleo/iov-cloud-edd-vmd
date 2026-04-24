package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.framework.common.domain.DoState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Qrcode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.QrcodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.cache.CacheService;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 二维码领域仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class QrcodeRepositoryImpl extends AbstractRepository<String, Qrcode> implements QrcodeRepository {

    private final CacheService cacheService;

    @Override
    public Optional<Qrcode> getById(String s) {
        return Optional.empty();
    }

    @Override
    public boolean save(Qrcode qrcodePo) {
        if (qrcodePo.getState() != DoState.UNCHANGED) {
            cacheService.setQrcode(qrcodePo);
        }
        return true;
    }

    @Override
    public Optional<Qrcode> getByQrcode(String qrcode) {
        return cacheService.getQrcode(qrcode);
    }
}
