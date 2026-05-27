package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BuildConfigRequest extends BaseRequest {

    private Long id;

    private String platformCode;

    private String carLineCode;

    private String modelCode;

    private String baseModelCode;

    private String code;

    private String name;

    private String nameEn;

    private String vehicleStageCode;

    private Boolean enable;

    private Integer sort;

    private Date createTime;

}