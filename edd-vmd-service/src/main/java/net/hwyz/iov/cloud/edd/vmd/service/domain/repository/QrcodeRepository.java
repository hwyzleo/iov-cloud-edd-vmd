package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Qrcode;
import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;

import java.util.Optional;

/**
 * 二维码领域仓库接口
 *
 * @author hwyz_leo
 */
public interface QrcodeRepository extends BaseRepository<String, Qrcode> {

    /**
     * 根据二维码获取二维码领域对象
     *
     * @param qrcode 二维码
     * @return 二维码领域对象
     */
    Optional<Qrcode> getByQrcode(String qrcode);

}
