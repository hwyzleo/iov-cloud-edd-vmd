package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KmsHsmResult {

    private String kmsKeyRef;
    private String keySpec;
    private String provider;
    private String algorithm;
}