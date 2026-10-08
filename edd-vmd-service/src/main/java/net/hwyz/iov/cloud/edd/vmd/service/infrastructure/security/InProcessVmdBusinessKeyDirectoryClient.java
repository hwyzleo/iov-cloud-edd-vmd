package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyDirectoryAppService;
import net.hwyz.iov.cloud.framework.security.crypto.VmdBusinessKeyDirectoryClient;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyDescriptor;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.DeviceKeyContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.KeyOperation;
import org.springframework.stereotype.Component;

/**
 * framework BusinessKeyDirectoryResolver Adapter 进程内实现（CR-055 §7.2）
 * <p>
 * framework-security 与本服务同 JVM：直接委托 {@link BusinessKeyDirectoryAppService}，
 * 避免自调用 HTTP 环。framework 的 {@code DefaultVmdBusinessKeyDirectoryResolver}
 * 通过 ObjectProvider 注入本 bean（crypto.business-key.directory.enabled=true）。
 * <p>
 * 注意：调用方身份由接入层认证上下文提供，不接受请求体自报 caller。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InProcessVmdBusinessKeyDirectoryClient implements VmdBusinessKeyDirectoryClient {

    private final BusinessKeyDirectoryAppService businessKeyDirectoryAppService;

    @Override
    public BusinessKeyDescriptor resolveActive(DeviceKeyContext context) {
        return businessKeyDirectoryAppService.resolveActive(context);
    }

    @Override
    public BusinessKeyDescriptor resolveByKeyId(String keyId, KeyOperation operation) {
        return businessKeyDirectoryAppService.resolveByKeyId(keyId, operation);
    }
}
