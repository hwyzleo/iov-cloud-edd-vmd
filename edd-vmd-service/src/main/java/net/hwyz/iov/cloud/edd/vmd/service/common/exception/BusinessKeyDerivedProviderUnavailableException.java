package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 根派生/Transit 根能力未就绪异常（VMD-DSN-CR-061）。
 * <p>
 * {@code KeyProvisioningTemplate}/{@code deriveAndWrap} 能力缺失、Transit/HSM 受治理根
 * 不可用（缺失/类型错误/权限/版本/sealed）时抛出，属依赖不可用，可经原请求重试。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
public class BusinessKeyDerivedProviderUnavailableException extends VmdBaseException {

    public BusinessKeyDerivedProviderUnavailableException(String detail) {
        super(VmdErrorCode.KEYPROV_DERIVED_PROVIDER_NOT_READY, detail);
    }

    public BusinessKeyDerivedProviderUnavailableException() {
        super(VmdErrorCode.KEYPROV_DERIVED_PROVIDER_NOT_READY);
    }
}