package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildConfigFeatureCodeResponse {

    private Long id;

    private String buildConfigCode;

    private String familyCode;

    private String familyName;

    private String[] featureCode;

    private String[] featureName;

    private String featureType;

    private Date createTime;

}