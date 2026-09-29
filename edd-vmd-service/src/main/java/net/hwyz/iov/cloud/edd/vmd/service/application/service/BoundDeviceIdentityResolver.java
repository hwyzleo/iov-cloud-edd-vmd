package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateDeviceUidUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import org.springframework.stereotype.Service;

/**
 * active TBOX 绑定设备身份解析器（CR-054）
 * <p>
 * 按 VIN + deviceSn 定位唯一 active 物理绑定，并解析权威 HSM UID：
 * <ol>
 *   <li>优先取 active 绑定关联 part_info.extra.HSM（字段名由 HsmUidFieldResolver 统一）；</li>
 *   <li>part_security_constant.chip_uid 用作一致性核对与存量受控兜底；</li>
 *   <li>两处均有且规范化后不一致：数据冲突，fail-closed、告警并停止签发；</li>
 *   <li>两处均无：身份数据不完整，fail-closed，不得退回 device_sn。</li>
 * </ol>
 * 解析失败一律抛出 {@link CertificateDeviceUidUnavailableException}，禁止绕过。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoundDeviceIdentityResolver {

    /** part_info.extra 中 HSM UID 字段（小写，CR-049 统一字段名 HSM） */
    private static final String EXTRA_HSM_FIELD = "hsm";

    private final PartInfoRepository partInfoRepository;
    private final VehiclePartRepository vehiclePartRepository;
    private final PartSecurityConstantRepository partSecurityConstantRepository;

    /**
     * 解析 active 绑定及权威 HSM UID
     *
     * @param vin            车辆VIN
     * @param deviceSn       TBOX 物理实例序列号
     * @param deviceCategory 设备类别
     * @return 绑定设备身份
     */
    public BoundDeviceIdentity resolve(String vin, String deviceSn, String deviceCategory) {
        PartInfo partInfo = partInfoRepository.selectBySn(deviceSn);
        if (partInfo == null) {
            throw new IllegalArgumentException("设备不存在: " + deviceSn);
        }

        VehiclePart activeBinding = vehiclePartRepository.selectActiveByVinAndPartId(vin, partInfo.getId());
        if (activeBinding == null) {
            throw new IllegalStateException("设备与车辆未建立active绑定: vin=" + vin + ", deviceSn=" + deviceSn);
        }

        String extraHsm = resolveFromPartInfoExtra(partInfo);
        PartSecurityConstant secConstant = partSecurityConstantRepository.selectByPartCodeAndSn(partInfo.getPartCode(), deviceSn);
        String chipUid = secConstant != null ? secConstant.getChipUid() : null;

        String normExtra = CsrUtils.normalizeUid(extraHsm);
        String normChip = CsrUtils.normalizeUid(chipUid);

        String hsmUid;
        String uidSource;
        if (StrUtil.isNotBlank(normExtra) && StrUtil.isNotBlank(normChip)) {
            if (!normExtra.equals(normChip)) {
                throw new CertificateDeviceUidUnavailableException(deviceSn,
                        "HSM UID来源冲突: part_info.extra=" + normExtra + ", chip_uid=" + normChip);
            }
            hsmUid = normExtra;
            uidSource = "BOTH";
        } else if (StrUtil.isNotBlank(normExtra)) {
            hsmUid = normExtra;
            uidSource = "PART_INFO_EXTRA";
        } else if (StrUtil.isNotBlank(normChip)) {
            hsmUid = normChip;
            uidSource = "SECURITY_CONSTANT";
        } else {
            throw new CertificateDeviceUidUnavailableException(deviceSn, "HSM UID缺失（part_info.extra 与 chip_uid 均无）");
        }

        return new BoundDeviceIdentity(
                vin, activeBinding.getId(), partInfo.getId(), deviceSn, deviceCategory, hsmUid, uidSource);
    }

    /**
     * 从 part_info.extra JSON 解析 HSM UID（缺字段/解析失败返回 null）
     */
    private String resolveFromPartInfoExtra(PartInfo partInfo) {
        if (partInfo == null || partInfo.getExtra() == null || partInfo.getExtra().isBlank()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(partInfo.getExtra()).getStr(EXTRA_HSM_FIELD);
        } catch (Exception e) {
            log.warn("解析 part_info.extra 失败，sn=[{}]", partInfo.getSn(), e);
            return null;
        }
    }

}
