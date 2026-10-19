package net.hwyz.iov.cloud.edd.vmd.service.support;

import net.hwyz.iov.cloud.framework.security.crypto.codec.CanonicalEncoding;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Date;

/**
 * VMD CR-062 测试用 P-256 设备证书材料（keyUsage=digitalSignature 自签名）。
 * <p>
 * 仅用于以真实 {@code DefaultImportProofVerifier} 驱动导入证明验签端到端测试；不进入生产。
 *
 * @author hwyz_leo
 * @since 2026-10-19
 */
public final class TestDeviceCertificate {

    private TestDeviceCertificate() {
    }

    public record Material(KeyPair keyPair, X509Certificate certificate, byte[] certificateDer) {

        public byte[] certificateDigest() {
            return CanonicalEncoding.sha256(certificateDer);
        }

        public String fingerprintHex() {
            return CanonicalEncoding.hexLower(certificateDigest());
        }
    }

    public static Material generate() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = generator.generateKeyPair();

        Date notBefore = Date.from(Instant.now().minusSeconds(3600));
        Date notAfter = Date.from(Instant.now().plusSeconds(24 * 3600));
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=TBOX-CR062-TEST"), BigInteger.valueOf(2026L), notBefore, notAfter,
                new X500Name("CN=TBOX-CR062-TEST"), keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate())));
        return new Material(keyPair, certificate, certificate.getEncoded());
    }
}
