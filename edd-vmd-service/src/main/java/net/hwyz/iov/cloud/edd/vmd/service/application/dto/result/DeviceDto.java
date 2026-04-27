package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDto {

    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String type;
    private String deviceItem;
    private String funcDomain;
    private String[] nodeType;
    private String otaSupport;
    private String partitionType;
    private Integer lockUnlockSecurityComponent;
    private String linkConfigSource;
    private String linkFlashTarget;
    private String[] commProtocol;
    private String[] flashProtocol;
    private String canTxId;
    private String canRxId;
    private String ethernetIp;
    private String doipGatewayId;
    private String doipEntityId;
    private Boolean core;
    private Integer sort;

}
