package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * CSR工具类
 * <p>
 * 用于解析和校验PKCS#10 CSR。
 *
 * @author hwyz_leo
 */
@Slf4j
public class CsrUtils {

    /**
     * 解析CSR获取Common Name (CN)
     *
     * @param csrDerBase64 CSR DER编码的Base64字符串
     * @return Common Name
     */
    public static String parseCommonName(String csrDerBase64) {
        // TODO: 实现CSR解析逻辑
        // 1. Base64解码
        // 2. 解析ASN.1结构
        // 3. 提取Subject中的CN字段
        // 模拟实现：从CSR数据中提取设备SN
        try {
            // 使用URL安全的Base64解码器，支持下划线字符
            byte[] csrDer = Base64.getUrlDecoder().decode(csrDerBase64);
            String csrContent = new String(csrDer);
            // 如果CSR内容包含设备SN，则返回设备SN
            if (csrContent.startsWith("TBOX-") || csrContent.startsWith("CCP-") || csrContent.startsWith("ADCM-")) {
                return csrContent;
            }
            return "MOCK_DEVICE_SN";
        } catch (Exception e) {
            log.warn("CSR解析失败，返回模拟值", e);
            return "MOCK_DEVICE_SN";
        }
    }

    /**
     * 验证CSR签名
     *
     * @param csrDerBase64 CSR DER编码的Base64字符串
     * @return 是否有效
     */
    public static boolean verifySignature(String csrDerBase64) {
        // TODO: 实现CSR签名验证逻辑
        // 1. Base64解码
        // 2. 解析ASN.1结构
        // 3. 验证签名
        log.warn("CSR签名验证功能待实现，返回true");
        return true;
    }

    /**
     * 检查CSR是否包含VIN
     *
     * @param csrDerBase64 CSR DER编码的Base64字符串
     * @param vin          车辆VIN
     * @return 是否包含VIN
     */
    public static boolean containsVin(String csrDerBase64, String vin) {
        // TODO: 实现CSR VIN检查逻辑
        // 1. Base64解码
        // 2. 解析ASN.1结构
        // 3. 检查Subject和SAN中是否包含VIN
        log.warn("CSR VIN检查功能待实现，返回false");
        return false;
    }

    /**
     * 计算CSR指纹
     *
     * @param csrDerBase64 CSR DER编码的Base64字符串
     * @return SHA-256指纹
     */
    public static String calculateFingerprint(String csrDerBase64) {
        try {
            byte[] csrDer = Base64.getDecoder().decode(csrDerBase64);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(csrDer);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("计算CSR指纹失败", e);
            throw new RuntimeException("计算CSR指纹失败", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
