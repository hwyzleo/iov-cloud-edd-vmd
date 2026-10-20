package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security;

import java.util.Arrays;

/**
 * CSR 请求扩展（VMD-DSN-CR-063 §4.2）。
 * <p>
 * 扩展校验采用 OID + criticality + semantic validator allowlist：value 字节防御性复制，
 * toString 不输出扩展值（可能含高基数身份/敏感字节）。
 *
 * @param oid      扩展 OID
 * @param critical 是否 critical
 * @param value    扩展值 DER（防御性复制）
 * @author hwyz_leo
 * @since 2026-10-20
 */
public record CsrExtension(String oid, boolean critical, byte[] value) {

    public CsrExtension {
        value = value == null ? null : value.clone();
    }

    @Override
    public byte[] value() {
        return value == null ? null : value.clone();
    }

    @Override
    public String toString() {
        return "CsrExtension{oid=" + oid + ", critical=" + critical + ", valueLength="
                + (value == null ? 0 : value.length) + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CsrExtension other)) {
            return false;
        }
        return critical == other.critical
                && java.util.Objects.equals(oid, other.oid)
                && Arrays.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * java.util.Objects.hashCode(oid) + Boolean.hashCode(critical))
                + Arrays.hashCode(value);
    }
}
