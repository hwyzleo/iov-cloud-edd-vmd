package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.adapter;

import net.hwyz.iov.cloud.framework.common.constant.CustomHeaders;
import net.hwyz.iov.cloud.framework.common.util.ServletUtil;
import org.springframework.stereotype.Component;

/**
 * Keyprov Service 调用方身份解析（VMD-DSN-CR-059 §4/§6）。
 * <p>
 * 从服务认证上下文（内部调用透传的 client-id 头）解析调用方 service identity；
 * 不以请求体自报 caller 授权。无请求上下文或未携带身份时返回 null，由应用门面 fail-closed。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Component
public class KeyProvisioningCallerResolver {

    /**
     * 解析当前调用的 service identity（可能为空）
     */
    public String resolveCaller() {
        try {
            return ServletUtil.getHeader(CustomHeaders.CLIENT_ID);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
