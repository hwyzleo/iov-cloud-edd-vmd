package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleModelConfigService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdConfigurationResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 车辆车系车型配置相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdVehicleModelConfigServiceFallbackFactory implements FallbackFactory<VmdVehicleModelConfigService> {

    @Override
    public VmdVehicleModelConfigService create(Throwable throwable) {
        return new VmdVehicleModelConfigService() {
            @Override
            public String getConfigurationCodeByFeatureCodes(Map<String, String> featureCodeMap) {
                log.error("车辆车系车型配置服务根据特征族特征值[{}]得到匹配的配置代码调用失败", featureCodeMap, throwable);
                return null;
            }

            @Override
            public List<VmdConfigurationResponse> getConfigurationListByVariantCode(String variantCode) {
                log.error("车辆车系车型配置服务根据版本代码[{}]获取配置列表调用失败", variantCode, throwable);
                return Collections.emptyList();
            }

            @Override
            public List<VmdConfigurationResponse> getConfigurationListByBaseModelCode(String baseModelCode) {
                log.error("车辆车系车型配置服务根据基础车型代码[{}]获取配置列表调用失败", baseModelCode, throwable);
                return Collections.emptyList();
            }

            @Override
            public VmdConfigurationResponse getConfigurationByCode(String configurationCode) {
                log.error("车辆车系车型配置服务根据配置代码[{}]获取配置详细信息调用失败", configurationCode, throwable);
                return null;
            }

            @Override
            public String getConfigurationCodeByOptionCodes(String saleModelCode, List<String> optionCodes) {
                log.error("车辆车系车型配置服务根据销售车型编码[{}]和OptionCode列表[{}]获取配置代码调用失败", saleModelCode, optionCodes, throwable);
                return null;
            }

            @Override
            public String getConfigurationCodeByOptionCodeMap(Map<String, String> optionCodes) {
                log.error("车辆车系车型配置服务根据选项族选项值[{}]得到匹配的配置代码调用失败", optionCodes, throwable);
                return null;
            }

            @Deprecated
            @Override
            public String getVehicleBuildConfigCode(Map<String, String> featureCodeMap) {
                log.error("车辆车系车型配置服务根据特征族特征值[{}]得到匹配的生产配置代码调用失败", featureCodeMap, throwable);
                return null;
            }

            @Deprecated
            @Override
            public List<VmdBuildConfigResponse> getBuildConfigListByVariantCode(String variantCode) {
                log.error("车辆车系车型配置服务根据版本代码[{}]获取生产配置列表调用失败", variantCode, throwable);
                return Collections.emptyList();
            }

            @Deprecated
            @Override
            public List<VmdBuildConfigResponse> getBuildConfigListByBaseModelCode(String baseModelCode) {
                log.error("车辆车系车型配置服务根据基础车型代码[{}]获取生产配置列表调用失败", baseModelCode, throwable);
                return Collections.emptyList();
            }

            @Deprecated
            @Override
            public VmdBuildConfigResponse getBuildConfigByCode(String buildConfigCode) {
                log.error("车辆车系车型配置服务根据生产配置代码[{}]获取生产配置详细信息调用失败", buildConfigCode, throwable);
                return null;
            }

            @Deprecated
            @Override
            public String getBuildConfigCodeByOptionCodes(String saleModelCode, List<String> optionCodes) {
                log.error("车辆车系车型配置服务根据销售车型编码[{}]和OptionCode列表[{}]获取生产配置代码调用失败", saleModelCode, optionCodes, throwable);
                return null;
            }
        };
    }
}
