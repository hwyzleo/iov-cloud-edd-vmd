package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.QrcodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.QrcodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.QrcodePublish;
import net.hwyz.iov.cloud.edd.vmd.service.domain.factory.QrcodeFactory;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Qrcode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.QrcodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleRepository;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.QrcodeNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleHasActivatedException;
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
     * @return 二维码 DTO
     */
    public QrcodeDto generateActiveQrcode(String vin, String sn) {
        Vehicle vehicle = vehicleRepository.getByVin(vin);
        if (vehicle.isActive()) {
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
     * @return 二维码 DTO
     */
    public QrcodeDto getActiveQrcodeState(String qrcode, String vin, String sn) {
        return getQrcodeState(qrcode, vin, sn);
    }

    /**
     * 生成二维码
     *
     * @param type 二维码类型
     * @param vin  车架号
     * @param sn   车机序列号
     * @return 二维码 DTO
     */
    private QrcodeDto generateQrcode(QrcodeType type, String vin, String sn) {
        Qrcode qrcode = qrcodeFactory.buildQrcode(type, vin, sn);
        qrcodeRepository.save(qrcode);
        return QrcodeAssembler.INSTANCE.fromDomain(qrcode);
    }

    /**
     * 获取二维码状态
     *
     * @param qrcode 二维码
     * @param vin    车架号
     * @param sn     车机序列号
     * @return 二维码 DTO
     */
    private QrcodeDto getQrcodeState(String qrcode, String vin, String sn) {
        Qrcode qrcodeDomain = qrcodeRepository.getByQrcode(qrcode).orElseThrow(() -> new QrcodeNotExistException(qrcode));
        if (!qrcodeDomain.getSn().equals(sn)) {
            log.warn("车辆[{}]获取二维码状态时，传入的SN[{}]与原SN[{}]不一致", vin, sn, qrcodeDomain.getSn());
        }
        qrcodeDomain.polling();
        qrcodeRepository.save(qrcodeDomain);
        return QrcodeAssembler.INSTANCE.fromDomain(qrcodeDomain);
    }

    /**
     * 扫描验证二维码
     *
     * @param qrcode    二维码
     * @param accountId 账号ID
     * @return 二维码 DTO
     */
    public QrcodeDto validateQrcode(String qrcode, String accountId) {
        Qrcode qrcodeDomain = qrcodeRepository.getByQrcode(qrcode).orElseThrow(() -> new QrcodeNotExistException(qrcode));
        qrcodeDomain.polling();
        qrcodeDomain.validate(qrcode);
        qrcodeRepository.save(qrcodeDomain);
        qrcodePublish.validate(qrcodeDomain.getType(), qrcodeDomain.getVin(), accountId);
        return QrcodeAssembler.INSTANCE.fromDomain(qrcodeDomain);
    }

    /**
     * 确认二维码
     *
     * @param qrcode    二维码
     * @param accountId 账号ID
     * @return 二维码 DTO
     */
    public QrcodeDto confirmQrcode(String qrcode, String accountId) {
        Qrcode qrcodeDomain = qrcodeRepository.getByQrcode(qrcode).orElseThrow(() -> new QrcodeNotExistException(qrcode));
        qrcodeDomain.polling();
        qrcodeDomain.confirm(qrcode);
        qrcodeRepository.save(qrcodeDomain);
        qrcodePublish.confirm(qrcodeDomain.getType(), qrcodeDomain.getVin(), accountId);
        return QrcodeAssembler.INSTANCE.fromDomain(qrcodeDomain);
    }
}
