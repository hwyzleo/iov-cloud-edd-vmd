package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 派生参数/材料模型/锚不受支持异常（VMD-DSN-CR-061）。
 * <p>
 * 例如 BizType 材料模型不是 {@code DERIVED_ROOT}（含 {@code materialModel=null}）、锚类型与
 * {@code prov.anchor} 不一致、purpose/派生版本不受支持、对历史 {@code material_model IS NULL}
 * 行做自动轮换等；属不可重试的参数/能力拒绝（禁止隐式回退到随机材料路径）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
public class BusinessKeyDerivationRejectedException extends VmdBaseException {

    public BusinessKeyDerivationRejectedException(String detail) {
        super(VmdErrorCode.KEYPROV_DERIVATION_UNSUPPORTED, detail);
    }

    public BusinessKeyDerivationRejectedException() {
        super(VmdErrorCode.KEYPROV_DERIVATION_UNSUPPORTED);
    }
}