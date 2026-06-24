package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.dto.KmsHsmResult;

public interface KmsHsmClient {

    KmsHsmResult generatePerVinConstant(String vin) throws Exception;

    KmsHsmResult generatePerDeviceConstant(String partCode, String sn, String constantType) throws Exception;
}