package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request;

import lombok.*;
import net.hwyz.iov.cloud.framework.common.bean.BaseRequest;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BuildConfigFeatureCodeRequest extends BaseRequest {

    private Long id;

    private String buildConfigCode;

    private String familyCode;

    private String familyName;

    private String[] featureCode;

    private String[] featureName;

    private String featureType;

}