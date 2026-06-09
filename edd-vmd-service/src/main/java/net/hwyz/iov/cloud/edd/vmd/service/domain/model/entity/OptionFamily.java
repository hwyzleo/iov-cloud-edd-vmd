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
public class OptionFamily implements DomainObj<OptionFamily> {

    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String type;
    private Boolean mandatory;
    private Boolean enable;
    private Integer sort;
    private String source;
    private String externalRefId;
    private Long externalVersion;
    private LocalDateTime lastSyncTime;
}
