package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security;

import org.bouncycastle.asn1.x509.KeyUsage;

/**
 * CSR 请求 KeyUsage 位标志（VMD-DSN-CR-063 §4.2）。
 * <p>
 * 值语义对齐 BouncyCastle {@link KeyUsage} 位定义，供策略判定 CSR 请求的 KeyUsage 位集合。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
public enum KeyUsageFlag {

    DIGITAL_SIGNATURE(KeyUsage.digitalSignature),
    NON_REPUDIATION(KeyUsage.nonRepudiation),
    KEY_ENCIPHERMENT(KeyUsage.keyEncipherment),
    DATA_ENCIPHERMENT(KeyUsage.dataEncipherment),
    KEY_AGREEMENT(KeyUsage.keyAgreement),
    KEY_CERT_SIGN(KeyUsage.keyCertSign),
    CRL_SIGN(KeyUsage.cRLSign),
    ENCIPHER_ONLY(KeyUsage.encipherOnly),
    DECIPHER_ONLY(KeyUsage.decipherOnly);

    private final int bit;

    KeyUsageFlag(int bit) {
        this.bit = bit;
    }

    public int bit() {
        return bit;
    }

    /**
     * 从 KeyUsage 位掩码提取置位标志集合
     */
    public static java.util.Set<KeyUsageFlag> fromMask(int mask) {
        java.util.EnumSet<KeyUsageFlag> flags = java.util.EnumSet.noneOf(KeyUsageFlag.class);
        for (KeyUsageFlag flag : values()) {
            if ((mask & flag.bit) != 0) {
                flags.add(flag);
            }
        }
        return flags;
    }
}
