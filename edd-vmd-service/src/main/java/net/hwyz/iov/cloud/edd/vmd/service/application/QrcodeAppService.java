package net.hwyz.iov.cloud.edd.vmd.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.QrcodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.QrcodePublish;
import net.hwyz.iov.cloud.edd.vmd.service.domain.factory.QrcodeFactory;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.Qrcode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.QrcodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception.QrcodeNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception.VehicleHasActivatedException;
import org.springframework.stereotype.Service;

/**
 * 二维码相关应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QrcodeAppService {

    private final QrcodePublish qrcodePublish;
    private final QrcodeFactory qrcodeFactory;
    private final QrcodeRepository qrcodeRepository;
    private final VehicleRepository vehicleRepository;

    /**
     * 生成车辆激活二维码
     *
     * @param vin 车架号
     * @param sn  车机序列号
     * @return 二维码状态
     */
    public QrcodeResponse generateActiveQrcode(String vin, String sn) {
        Vehicle vehicleDo = vehicleRepository.getByVin(vin);
        if (vehicleDo.isActive()) {
            throw new VehicleHasActivatedException(vin);
        }
        return generateQrcode(QrcodeType.VEHICLE_ACTIVE, vin, sn);
    }

    /**
     * 获取车辆激活二维码状态
     *
     * @param qrcode 二维码
     * @param vin    车架号
     * @param sn     车机序列号
     * @return 二维码状态
     */
    public QrcodeResponse getActiveQrcodeState(String qrcode, String vin, String sn) {
        return getQrcodeState(qrcode, vin, sn);
    }

    /**
     * 生成二维码
     *
     * @param type 二维码类型
     * @param vin  车架号
     * @param sn   车机序列号
     * @return 二维码状态
     */
    private QrcodeResponse generateQrcode(QrcodeType type, String vin, String sn) {
        Qrcode qrcodeDo = qrcodeFactory.buildQrcode(type, vin, sn);
        qrcodeRepository.save(qrcodeDo);
        return QrcodeResponse.builder()
                .qrcode(qrcodeDo.getQrcode())
                .type(qrcodeDo.getType())
                .state(qrcodeDo.getQrcodeState())
                .build();
    }

    /**
     * 获取二维码状态
     *
     * @param qrcode 二维码
     * @param vin    车架号
     * @param sn     车机序列号
     * @return 二维码状态
     */
    private QrcodeResponse getQrcodeState(String qrcode, String vin, String sn) {
        Qrcode qrcodeDo = qrcodeRepository.getByQrcode(qrcode).orElseThrow(() -> new QrcodeNotExistException(qrcode));
        if (!qrcodeDo.getSn().equals(sn)) {
            log.warn("车辆[{}]获取二维码状态时，传入的SN[{}]与原SN[{}]不一致", vin, sn, qrcodeDo.getSn());
        }
        qrcodeDo.polling();
        qrcodeRepository.save(qrcodeDo);
        return QrcodeResponse.builder()
                .qrcode(qrcodeDo.getQrcode())
                .type(qrcodeDo.getType())
                .state(qrcodeDo.getQrcodeState())
                .build();
    }

    /**
     * 扫描验证二维码
     *
     * @param qrcode    二维码
     * @param accountId 账号ID
     * @return 二维码状态
     */
    public QrcodeResponse validateQrcode(String qrcode, String accountId) {
        Qrcode qrcodeDo = qrcodeRepository.getByQrcode(qrcode).orElseThrow(() -> new QrcodeNotExistException(qrcode));
        qrcodeDo.polling();
        qrcodeDo.validate(qrcode);
        qrcodeRepository.save(qrcodeDo);
        qrcodePublish.validate(qrcodeDo.getType(), qrcodeDo.getVin(), accountId);
        return QrcodeResponse.builder()
                .qrcode(qrcodeDo.getQrcode())
                .type(qrcodeDo.getType())
                .state(qrcodeDo.getQrcodeState())
                .build();
    }

    /**
     * 确认二维码
     *
     * @param qrcode    二维码
     * @param accountId 账号ID
     * @return 二维码状态
     */
    public QrcodeResponse confirmQrcode(String qrcode, String accountId) {
        Qrcode qrcodeDo = qrcodeRepository.getByQrcode(qrcode).orElseThrow(() -> new QrcodeNotExistException(qrcode));
        qrcodeDo.polling();
        qrcodeDo.confirm(qrcode);
        qrcodeRepository.save(qrcodeDo);
        qrcodePublish.confirm(qrcodeDo.getType(), qrcodeDo.getVin(), accountId);
        return QrcodeResponse.builder()
                .qrcode(qrcodeDo.getQrcode())
                .type(qrcodeDo.getType())
                .state(qrcodeDo.getQrcodeState())
                .build();
    }
}