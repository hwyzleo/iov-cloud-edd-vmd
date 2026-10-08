package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BusinessKeyMetadataResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyDirectoryAppService;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.BusinessKeyDescriptor;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.CallerIdentity;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.DeviceKeyContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.businesskey.KeyOperation;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 业务密钥目录内部 Service API（CR-055 §7.2）
 * <p>
 * 为 framework 的 BusinessKeyDirectoryResolver Adapter 与其它受信服务提供 Feign 契约：
 * <ul>
 *   <li>resolveActive(DeviceKeyContext) → BusinessKeyDescriptor（仅返回 ACTIVE）</li>
 *   <li>resolveByKeyId(keyId, operation) → BusinessKeyDescriptor（受控放行 DEPRECATED 解密窗口）</li>
 *   <li>getBusinessKeyMetadata(keyId) → 非敏感元数据</li>
 * </ul>
 * 调用方身份从服务认证上下文获取，不接受请求体自报 caller；返回可含内部 kmsKeyRef，
 * 但仅限受信服务间契约，不对外部客户端暴露。所有调用经 AppService，禁止绕过直连 KMS。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/businessKeyDirectory/v1")
public class BusinessKeyDirectoryController {

    private final BusinessKeyDirectoryAppService businessKeyDirectoryAppService;

    /**
     * 正向目录解析（framework 运行时/受信服务调用）
     *
     * @param context DeviceKeyContext（caller 字段忽略，以认证上下文为准）
     * @return 业务密钥描述符
     */
    @PostMapping("/resolveActive")
    public BusinessKeyDescriptor resolveActive(@RequestBody DeviceKeyContext context) {
        log.info("内部服务请求正向解析业务密钥: deviceSn={}, bizType={}, purpose={}",
                context != null ? context.deviceSn() : null,
                context != null && context.bizType() != null ? context.bizType().name() : null,
                context != null ? context.purpose() : null);
        DeviceKeyContext trusted = new DeviceKeyContext(
                context.deviceSn(),
                context.bizType(),
                context.purpose(),
                currentCaller());
        return businessKeyDirectoryAppService.resolveActive(trusted);
    }

    /**
     * 反向目录解析（按 keyId + 操作）
     *
     * @param keyId     framework/KMS 不透明标识
     * @param operation ENCRYPT/DECRYPT
     * @return 业务密钥描述符
     */
    @GetMapping("/keyId/{keyId}/operation/{operation}")
    public BusinessKeyDescriptor resolveByKeyId(@PathVariable String keyId, @PathVariable KeyOperation operation) {
        log.info("内部服务请求反向解析业务密钥: keyId={}, operation={}", keyId, operation);
        return businessKeyDirectoryAppService.resolveByKeyId(keyId, operation);
    }

    /**
     * 查询非敏感元数据
     *
     * @param keyId framework/KMS 不透明标识
     * @return 非敏感元数据
     */
    @GetMapping("/keyId/{keyId}/metadata")
    public BusinessKeyMetadataResult getMetadata(@PathVariable String keyId) {
        return businessKeyDirectoryAppService.getMetadata(keyId);
    }

    /**
     * 从认证上下文获取调用方身份（不接受请求体自报）
     */
    private CallerIdentity currentCaller() {
        try {
            Long userId = SecurityUtils.getUserId();
            if (userId != null) {
                return new CallerIdentity(String.valueOf(userId), "AUTH_CONTEXT");
            }
        } catch (Exception e) {
            log.debug("未获取到认证上下文调用方，使用匿名: {}", e.getMessage());
        }
        return CallerIdentity.anonymous();
    }
}
