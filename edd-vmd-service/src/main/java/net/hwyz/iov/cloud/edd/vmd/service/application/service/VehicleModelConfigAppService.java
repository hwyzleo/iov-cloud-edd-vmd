package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBuildConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车系车型配置相关应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleModelConfigAppService {

    private final VehBuildConfigRepository vehBuildConfigRepository;

    /**
     * 根据车型配置类型获取生产配置编码
     *
     * @param baseModelCode 基础车型编码
     * @param exteriorCode  外饰编码
     * @param interiorCode  内饰编码
     * @param wheelCode     轮毂编码
     * @param tireCode      轮胎编码
     * @param spareTireCode 备胎编码
     * @param adasCode      智驾编码
     * @param seatCode      座椅编码
     * @return 生产配置编码
     */
    public String getBuildConfigCodeByType(String baseModelCode, String exteriorCode, String interiorCode, String wheelCode,
                                           String tireCode, String spareTireCode, String adasCode, String seatCode) {
        List<BuildConfig> buildConfigList = vehBuildConfigRepository.selectByExample(BuildConfig.builder()
                .baseModelCode(baseModelCode)
                .exteriorCode(exteriorCode)
                .interiorCode(interiorCode)
                .wheelCode(wheelCode)
                .tireCode(tireCode)
                .spareTireCode(spareTireCode)
                .adasCode(adasCode)
                .seatCode(seatCode)
                .build());
        if (buildConfigList.isEmpty()) {
            return null;
        }
        if (buildConfigList.size() > 1) {
            log.warn("车型[{}]外饰[{}]内饰[{}]轮毂[{}]轮胎[{}]备胎[{}]智驾[{}]查询车型配置编码结果数量大于1", baseModelCode,
                    exteriorCode, interiorCode, wheelCode, tireCode, spareTireCode, adasCode);
        }
        return buildConfigList.get(0).getCode();
    }

    /**
     * 根据车型配置类型得到匹配的生产配置代码
     *
     * @param baseModelCode 基础车型代码
     * @param exteriorCode  外饰代码
     * @param interiorCode  内饰代码
     * @param wheelCode     轮毂代码
     * @param tireCode      轮胎代码
     * @param spareTireCode 备胎代码
     * @param adasCode      智驾代码
     * @param seatCode      座椅代码
     * @return 生产配置代码
     */
    public String getVehicleBuildConfigCode(String baseModelCode, String exteriorCode, String interiorCode, String wheelCode,
                                            String tireCode, String spareTireCode, String adasCode, String seatCode) {
        return getBuildConfigCodeByType(baseModelCode, exteriorCode, interiorCode, wheelCode, tireCode, spareTireCode, adasCode, seatCode);
    }

}
