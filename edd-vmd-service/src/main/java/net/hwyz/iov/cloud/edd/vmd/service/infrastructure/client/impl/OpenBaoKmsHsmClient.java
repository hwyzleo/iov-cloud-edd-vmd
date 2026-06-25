package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.KmsHsmUnavailableException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.KmsHsmClient;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.client.dto.KmsHsmResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * KMS/HSM 客户端 OpenBao 实现
 * <p>
 * 对接 OpenBao（Vault 兼容）Transit 引擎，生成安全常量。
 * 通过配置 kms.hsm.type=openbao 启用。
 * <p>
 * 核心 API：
 * - 派生 per-VIN 安全常量：POST /v1/transit/hmac/oem-master/sha2-256
 * - 封装为运输态密文：POST /v1/transit/encrypt/{key_name}
 * - 生成 KCV 校验值：POST /v1/transit/hmac/oem-master/sha2-256
 *
 * @author hwyz_leo
 * @since 2026-06-25
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kms.hsm.type", havingValue = "openbao")
public class OpenBaoKmsHsmClient implements KmsHsmClient {

    /**
     * OpenBao 服务地址
     */
    @Value("${kms.hsm.address:https://kms.internal:8200}")
    private String kmsAddress;

    /**
     * OpenBao 认证 Token（AppRole 签发的短期 token）
     */
    @Value("${kms.hsm.token:}")
    private String kmsToken;

    /**
     * OEM 主密钥名称（用于 HMAC 派生）
     */
    @Value("${kms.hsm.master-key:oem-master}")
    private String masterKeyName;

    /**
     * HTTP 读取超时（毫秒）
     */
    @Value("${kms.hsm.read-timeout-ms:10000}")
    private int readTimeoutMs;

    @Override
    public KmsHsmResult generatePerVinConstant(String vin) throws Exception {
        log.info("[OpenBao] 请求生成 per-VIN 安全常量, vin={}", vin);

        try {
            // 规范化 VIN：大写、去空格
            String normalizedVin = normalizeIdentifier(vin);

            // 1. 派生 per-VIN 安全常量 = HMAC(oem-master, "per-vin-secret|VIN")
            String input = "per-vin-secret|" + normalizedVin;
            String hmacResult = callTransitHmac(masterKeyName, input);
            String constant = extractHmacValue(hmacResult);

            // 2. 生成 KCV 校验值 = HMAC(oem-master, "kcv|VIN")，截断 3 字节
            String kcvInput = "kcv|" + normalizedVin;
            String kcvHmac = callTransitHmac(masterKeyName, kcvInput);
            String kcv = extractKcv(kcvHmac);

            log.info("[OpenBao] per-VIN 安全常量派生成功, vin={}, kcv={}", vin, kcv);

            return KmsHsmResult.builder()
                    .kmsKeyRef(constant)
                    .keySpec("aes256-gcm96")
                    .provider("OpenBao")
                    .algorithm("HMAC-SHA256")
                    .build();
        } catch (Exception e) {
            log.error("[OpenBao] per-VIN 安全常量派生失败, vin={}", vin, e);
            throw new KmsHsmUnavailableException("KMS/HSM 服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public KmsHsmResult generatePerDeviceConstant(String partCode, String sn, String constantType, String chipUid) throws Exception {
        log.info("[OpenBao] 请求生成器件级安全常量, partCode={}, sn={}, constantType={}, chipUid={}", partCode, sn, constantType, chipUid);

        try {
            // 规范化标识符
            String normalizedPartCode = normalizeIdentifier(partCode);
            String normalizedSn = normalizeIdentifier(sn);
            String normalizedChipUid = normalizeIdentifier(chipUid);

            // 1. 派生器件级安全常量 = HMAC(oem-master, "per-device-secret|partCode|sn|chipUid")
            // chipUid 作为 KDF 派生绑定锚，确保常量与特定安全芯片绑定
            String input = "per-device-secret|" + normalizedPartCode + "|" + normalizedSn + "|" + normalizedChipUid;
            String hmacResult = callTransitHmac(masterKeyName, input);
            String constant = extractHmacValue(hmacResult);

            // 2. 封装到设备密钥（方式B）- 使用 chipUid 作为设备密钥名称
            String deviceKeyName = "dev-" + normalizedChipUid;
            ensureTransitKeyExists(deviceKeyName);
            String wrappedResult = callTransitEncrypt(deviceKeyName, constant);

            // 3. 生成 KCV 校验值（用于灌注回执对账）
            String kcvInput = "kcv|" + normalizedPartCode + "|" + normalizedSn + "|" + normalizedChipUid;
            String kcvHmac = callTransitHmac(masterKeyName, kcvInput);
            String kcv = extractKcv(kcvHmac);

            log.info("[OpenBao] 器件级安全常量派生成功, partCode={}, sn={}, chipUid={}, kcv={}", partCode, sn, chipUid, kcv);

            return KmsHsmResult.builder()
                    .kmsKeyRef(wrappedResult)
                    .keySpec("aes256-gcm96")
                    .provider("OpenBao")
                    .algorithm("HMAC-SHA256")
                    .build();
        } catch (Exception e) {
            log.error("[OpenBao] 器件级安全常量派生失败, partCode={}, sn={}, chipUid={}", partCode, sn, chipUid, e);
            throw new KmsHsmUnavailableException("KMS/HSM 服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 调用 Transit 引擎 HMAC API
     *
     * @param keyName 密钥名称
     * @param input   输入数据
     * @return HMAC 结果（格式：vault:v1:<base64>）
     */
    private String callTransitHmac(String keyName, String input) {
        String url = kmsAddress + "/v1/transit/hmac/" + keyName + "/sha2-256";
        String inputBase64 = Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));

        JSONObject requestBody = JSONUtil.createObj()
                .set("input", inputBase64);

        log.debug("[OpenBao] 调用 Transit HMAC API, url={}, keyName={}", url, keyName);

        HttpResponse response = HttpRequest.post(url)
                .header(Header.CONTENT_TYPE, "application/json")
                .header("X-Vault-Token", kmsToken)
                .timeout(readTimeoutMs)
                .body(requestBody.toString())
                .execute();

        if (!response.isOk()) {
            throw new RuntimeException("Transit HMAC API 调用失败, HTTP状态码: " + response.getStatus());
        }

        JSONObject responseJson = JSONUtil.parseObj(response.body());
        return responseJson.getJSONObject("data").getStr("hmac");
    }

    /**
     * 调用 Transit 引擎 Encrypt API
     *
     * @param keyName   密钥名称
     * @param plaintext 明文
     * @return 加密后的密文引用
     */
    private String callTransitEncrypt(String keyName, String plaintext) {
        String url = kmsAddress + "/v1/transit/encrypt/" + keyName;

        // 将明文转换为 base64
        String plaintextBase64 = Base64.getEncoder().encodeToString(
                plaintext.getBytes(StandardCharsets.UTF_8));

        JSONObject requestBody = JSONUtil.createObj()
                .set("plaintext", plaintextBase64);

        log.debug("[OpenBao] 调用 Transit Encrypt API, url={}, keyName={}", url, keyName);

        HttpResponse response = HttpRequest.post(url)
                .header(Header.CONTENT_TYPE, "application/json")
                .header("X-Vault-Token", kmsToken)
                .timeout(readTimeoutMs)
                .body(requestBody.toString())
                .execute();

        if (!response.isOk()) {
            throw new RuntimeException("Transit Encrypt API 调用失败, HTTP状态码: " + response.getStatus());
        }

        JSONObject responseJson = JSONUtil.parseObj(response.body());
        return responseJson.getJSONObject("data").getStr("ciphertext");
    }

    /**
     * 确保 Transit 引擎中的密钥存在，不存在则创建
     *
     * @param keyName 密钥名称
     */
    private void ensureTransitKeyExists(String keyName) {
        String url = kmsAddress + "/v1/transit/keys/" + keyName;

        log.debug("[OpenBao] 检查/创建 Transit 密钥, keyName={}", keyName);

        // 先尝试读取密钥（GET），判断是否存在
        HttpResponse readResponse = HttpRequest.get(url)
                .header("X-Vault-Token", kmsToken)
                .timeout(readTimeoutMs)
                .execute();

        if (readResponse.isOk()) {
            log.debug("[OpenBao] Transit 密钥已存在, keyName={}", keyName);
            return;
        }

        if (readResponse.getStatus() != 404) {
            log.warn("[OpenBao] 查询 Transit 密钥异常, keyName={}, status={}", keyName, readResponse.getStatus());
        }

        // 密钥不存在，创建新的
        log.info("[OpenBao] Transit 密钥不存在，创建新密钥, keyName={}", keyName);
        HttpResponse createResponse = HttpRequest.post(url)
                .header(Header.CONTENT_TYPE, "application/json")
                .header("X-Vault-Token", kmsToken)
                .timeout(readTimeoutMs)
                .execute();

        if (!createResponse.isOk()) {
            throw new RuntimeException("创建 Transit 密钥失败, keyName=" + keyName + ", HTTP状态码: " + createResponse.getStatus());
        }

        log.info("[OpenBao] Transit 密钥创建成功, keyName={}", keyName);
    }

    /**
     * 规范化标识符：大写、去空格
     *
     * @param identifier 原始标识符
     * @return 规范化后的标识符
     */
    private String normalizeIdentifier(String identifier) {
        if (StrUtil.isBlank(identifier)) {
            return "";
        }
        return identifier.toUpperCase().trim().replaceAll("\\s+", "");
    }

    /**
     * 从 HMAC 结果中提取值
     * HMAC 返回格式：vault:v1:<base64>
     *
     * @param hmacResult HMAC 结果
     * @return 提取的值
     */
    private String extractHmacValue(String hmacResult) {
        if (hmacResult == null) {
            throw new RuntimeException("HMAC 结果为空");
        }
        return hmacResult;
    }

    /**
     * 从 HMAC 结果中提取 KCV（Key Check Value）
     * KCV = 截断 3 字节 = 6 hex 字符
     *
     * @param hmacResult HMAC 结果
     * @return KCV（6 字符 hex 字符串）
     */
    private String extractKcv(String hmacResult) {
        if (hmacResult == null) {
            throw new RuntimeException("KCV HMAC 结果为空");
        }
        // 提取 base64 部分（vault:v1:<base64>）
        String[] parts = hmacResult.split(":");
        if (parts.length < 3) {
            throw new RuntimeException("HMAC 结果格式错误: " + hmacResult);
        }
        String base64Value = parts[2];
        // 解码并截取前 3 字节（6 hex 字符）
        byte[] decoded = Base64.getDecoder().decode(base64Value);
        StringBuilder kcv = new StringBuilder();
        for (int i = 0; i < Math.min(3, decoded.length); i++) {
            kcv.append(String.format("%02x", decoded[i] & 0xFF));
        }
        return kcv.toString();
    }
}
