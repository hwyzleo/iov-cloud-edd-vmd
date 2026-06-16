package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.LocalDateTime;

@Slf4j
@Getter
@Setter
@SuperBuilder
public class OptionCode implements DomainObj<OptionCode> {

    private Long id;
    private String optionFamilyCode;
    private String code;
    private String name;
    private String nameLocal;
    private String source;
    private String externalRefId;
    private Long externalVersion;
    private LocalDateTime lastSyncTime;
}
