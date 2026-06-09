package net.hwyz.iov.cloud.edd.vmd.api.service;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdConfigurationResponse;
import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.edd.vmd.api.fallback.VmdVehicleModelConfigServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 车辆车系车型配置相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "exVehicleModelConfigService", value = ServiceNameConstants.EDD_VMD, path = "/api/service/vehicleModelConfig/v1", fallbackFactory = VmdVehicleModelConfigServiceFallbackFactory.class)
public interface VmdVehicleModelConfigService {

    /**
     * 根据特征族特征值组合得到匹配的配置代码
     *
     * @param featureCodeMap  特征族代码-特征值代码映射
     * @return 配置代码
     */
    @GetMapping("/configurationCode")
    String getConfigurationCodeByFeatureCodes(@RequestParam Map<String, String> featureCodeMap);

    /**
     * 按选项族-选项值组合反查配置代码
     * CR-018: 原 getConfigurationCodeByFeatureCodes, 入参键改名
     */
    @GetMapping("/configurationCode/byOptionCodeMap")
    String getConfigurationCodeByOptionCodeMap(@RequestParam Map<String, String> optionCodes);

    /**
     * 根据版本代码获取配置列表
     *
     * @param variantCode 版本代码
     * @return 配置列表
     */
    @GetMapping("/configuration/list/{variantCode}")
    List<VmdConfigurationResponse> getConfigurationListByVariantCode(@PathVariable String variantCode);

    /**
     * 根据基础车型代码获取配置列表
     *
     * @param baseModelCode 基础车型代码
     * @return 配置列表
     */
    @GetMapping("/configuration/listByBaseModelCode/{baseModelCode}")
    List<VmdConfigurationResponse> getConfigurationListByBaseModelCode(@PathVariable String baseModelCode);

    /**
     * 根据配置代码获取配置详细信息（包含特征值）
     *
     * @param configurationCode 配置代码
     * @return 配置详细信息
     */
    @GetMapping("/configuration/{configurationCode}")
    VmdConfigurationResponse getConfigurationByCode(@PathVariable String configurationCode);

    /**
     * 根据销售车型编码和OptionCode列表获取匹配的配置代码
     *
     * @param saleModelCode 销售车型编码
     * @param optionCodes   OptionCode列表
     * @return 配置代码
     */
    @GetMapping("/configurationCode/byOptionCodes")
    String getConfigurationCodeByOptionCodes(@RequestParam String saleModelCode, @RequestParam List<String> optionCodes);

    /**
     * 根据特征族特征值组合得到匹配的生产配置代码
     *
     * @param featureCodeMap  特征族代码-特征值代码映射
     * @return 生产配置代码
     * @deprecated 使用 {@link #getConfigurationCodeByFeatureCodes(Map)} 替代
     */
    @Deprecated
    @GetMapping("/buildConfigCode")
    String getVehicleBuildConfigCode(@RequestParam Map<String, String> featureCodeMap);

    /**
     * 根据版本代码获取生产配置列表
     *
     * @param variantCode 版本代码
     * @return 生产配置列表
     * @deprecated 使用 {@link #getConfigurationListByVariantCode(String)} 替代
     */
    @Deprecated
    @GetMapping("/buildConfig/list/{variantCode}")
    List<VmdBuildConfigResponse> getBuildConfigListByVariantCode(@PathVariable String variantCode);

    /**
     * 根据基础车型代码获取生产配置列表
     *
     * @param baseModelCode 基础车型代码
     * @return 生产配置列表
     * @deprecated 使用 {@link #getConfigurationListByBaseModelCode(String)} 替代
     */
    @Deprecated
    @GetMapping("/buildConfig/listByBaseModelCode/{baseModelCode}")
    List<VmdBuildConfigResponse> getBuildConfigListByBaseModelCode(@PathVariable String baseModelCode);

    /**
     * 根据生产配置代码获取生产配置详细信息（包含特征值）
     *
     * @param buildConfigCode 生产配置代码
     * @return 生产配置详细信息
     * @deprecated 使用 {@link #getConfigurationByCode(String)} 替代
     */
    @Deprecated
    @GetMapping("/buildConfig/{buildConfigCode}")
    VmdBuildConfigResponse getBuildConfigByCode(@PathVariable String buildConfigCode);

    /**
     * 根据销售车型编码和OptionCode列表获取匹配的生产配置代码
     *
     * @param saleModelCode 销售车型编码
     * @param optionCodes   OptionCode列表
     * @return 生产配置代码
     * @deprecated 使用 {@link #getConfigurationCodeByOptionCodes(String, List)} 替代
     */
    @Deprecated
    @GetMapping("/buildConfigCode/byOptionCodes")
    String getBuildConfigCodeByOptionCodes(@RequestParam String saleModelCode, @RequestParam List<String> optionCodes);

}
