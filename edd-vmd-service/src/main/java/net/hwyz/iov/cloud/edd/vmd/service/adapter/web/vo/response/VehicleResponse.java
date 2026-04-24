package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 车辆信息响应对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    @JsonProperty("vin")
    private String vin;

    @JsonProperty("manufacturer_code")
    private String manufacturerCode;

    @JsonProperty("brand_code")
    private String brandCode;

    @JsonProperty("platform_code")
    private String platformCode;

    @JsonProperty("series_code")
    private String seriesCode;

    @JsonProperty("model_code")
    private String modelCode;

    @JsonProperty("base_model_code")
    private String baseModelCode;

    @JsonProperty("build_config_code")
    private String buildConfigCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("eol_time")
    private Date eolTime;

    @JsonProperty("order_num")
    private String orderNum;

}
