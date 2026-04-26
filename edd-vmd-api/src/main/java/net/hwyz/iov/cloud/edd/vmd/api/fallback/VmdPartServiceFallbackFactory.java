package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PartExService;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdPartService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 零件相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdPartServiceFallbackFactory implements FallbackFactory<VmdPartService> {

    @Override
    public VmdPartService create(Throwable throwable) {
        return new VmdPartService() {
            @Override
            public PartExService getByPn(String pn) {
                log.error("零件服务根据零件号[{}]查询零件信息调用失败", pn, throwable);
                return null;
            }

            @Override
            public List<PartExService> listAllFota(Boolean software) {
                log.error("零件服务获取所有FOTA升级零件（是否软件[{}]）信息调用失败", software, throwable);
                return null;
            }
        };
    }
}
