package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.framework.security.crypto.model.DerivedWrappedKey;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * 逻辑密钥代际判定与身份摘要（VMD-DSN-CR-066 §5/§6）。
 * <p>
 * 三层身份分离（§5.1）：
 * <ul>
 *   <li>密码学逻辑材料：{@code bizType + anchor + purpose + rootKeyVersion + derivationVersion}；</li>
 *   <li>VMD 目录材料：canonical directory row + keyId + businessKeyVersion（同逻辑材料不变）；</li>
 *   <li>开通/封装 attempt：requestId + purpose + operationId + certificate digest + envelope（可变）。</li>
 * </ul>
 * 业务版本提升必须对应可证明的材料代际变化（§5.2）：
 * {@code newGeneration = rootKeyVersion changed OR derivationVersion changed OR normalizedAnchor changed
 * OR purpose/bizType/materialModel changed}；证书变化、换件、requestId 变化均为 REIMPORT/REWRAP。
 * <p>
 * {@link #logicalIdentityDigest} 为 BINARY(32) 非敏感规范化身份摘要（§7.2），输入：
 * {@code versionedLengthPrefix(bizType, sha256(normalizedAnchor), purpose, rootKeyVersion, derivationVersion)}；
 * 仅作辅助索引/预检/审计，不取代 Provider keyId，根版本在 Provider 返回前无法确定时不得猜测最终摘要。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Component
public class LogicalKeyGenerationPolicy {

    /** 摘要编码版本（v1） */
    static final byte DIGEST_VERSION_V1 = 1;

    /**
     * 判断两个目录行的密码学逻辑代际是否相同（VMD-DSN-CR-066 §5.2）。
     * <p>目录行不含 anchor 原文；同一设备上下文中 keyId/kmsKeyRef 确定性等价于 anchor 一致，
     * 此处比较 materialModel/rootKeyVersion/derivationVersion/上下文（deviceSn/domain/purpose）。
     *
     * @param left  目录行 A（可为 null）
     * @param right 目录行 B（可为 null）
     * @return 代际是否相同
     */
    public boolean sameLogicalGeneration(DeviceBusinessKey left, DeviceBusinessKey right) {
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getMaterialModel(), right.getMaterialModel())
                && Objects.equals(left.getRootKeyVersion(), right.getRootKeyVersion())
                && Objects.equals(left.getDerivationVersion(), right.getDerivationVersion())
                && Objects.equals(left.getBusinessDomain(), right.getBusinessDomain())
                && Objects.equals(left.getPurpose(), right.getPurpose())
                && Objects.equals(left.getDeviceSn(), right.getDeviceSn());
    }

    /**
     * 目录行与 Provider 派生结果是否同一逻辑材料（claim-or-reuse 身份一致性核心）。
     * <p>keyId 为确定性标识（按 keyId 命中既有行时天然相等）；锚一致性由 keyId/kmsKeyRef
     * 确定性等价保证；此处比较 rootKeyVersion/derivationVersion 与材料模型。
     *
     * @param row    目录行
     * @param result Provider 派生封装结果
     * @return 是否同一逻辑材料
     */
    public boolean matchesResultGeneration(DeviceBusinessKey row, DerivedWrappedKey result) {
        if (row == null || result == null) {
            return false;
        }
        boolean rootMatches = row.getRootKeyVersion() != null
                && row.getRootKeyVersion() == result.rootKeyVersion();
        boolean derivationMatches = Objects.equals(row.getDerivationVersion(), result.derivationVersion());
        boolean materialMatches = row.getMaterialModel() != null
                && "DERIVED_ROOT".equals(row.getMaterialModel());
        boolean keyIdMatches = row.getKeyId() == null || row.getKeyId().equals(result.logicalKeyId());
        return rootMatches && derivationMatches && materialMatches && keyIdMatches;
    }

    /**
     * 计算非敏感规范化身份摘要（BINARY(32)，VMD-DSN-CR-066 §7.2）。
     * <p>输入：{@code versionedLengthPrefix(bizType, sha256(normalizedAnchor), purpose, rootKeyVersion, derivationVersion)}。
     * 返回 SHA-256 后的 32 字节；字段缺省（rootKeyVersion 未知）时返回 {@code null}，禁止猜测最终摘要。
     *
     * @param bizType           业务类型名称（如 V2C_COMM_ROOT）
     * @param normalizedAnchor  规范化锚原文（VIN/UID；内部先做 SHA-256 再入编码）
     * @param purpose           派生用途
     * @param rootKeyVersion    Transit/HSM 根版本（Provider 返回前为 null）
     * @param derivationVersion 派生格式版本
     * @return 32 字节身份摘要；rootKeyVersion 为 null 时返回 null
     */
    public byte[] logicalIdentityDigest(String bizType, String normalizedAnchor, String purpose,
                                        Long rootKeyVersion, String derivationVersion) {
        if (bizType == null || normalizedAnchor == null || purpose == null
                || rootKeyVersion == null || derivationVersion == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 * 5 + (2 + 32 + 2 + 8 + 2 + 1) * 4);
        buffer.put(DIGEST_VERSION_V1);
        putLengthPrefixed(buffer, bizType.getBytes(StandardCharsets.UTF_8));
        putLengthPrefixed(buffer, sha256(normalizedAnchor));
        putLengthPrefixed(buffer, purpose.getBytes(StandardCharsets.UTF_8));
        putLengthPrefixed(buffer, ByteBuffer.allocate(8).putLong(rootKeyVersion).array());
        putLengthPrefixed(buffer, derivationVersion.getBytes(StandardCharsets.UTF_8));
        return sha256(buffer.array());
    }

    /**
     * length-prefixed 编码（4 字节大端长度 + 内容）。
     */
    private static void putLengthPrefixed(ByteBuffer buffer, byte[] value) {
        buffer.putInt(value.length);
        buffer.put(value);
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static byte[] sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 测试辅助：字节数组相等。
     */
    public static boolean digestEquals(byte[] left, byte[] right) {
        return Arrays.equals(left, right);
    }
}
