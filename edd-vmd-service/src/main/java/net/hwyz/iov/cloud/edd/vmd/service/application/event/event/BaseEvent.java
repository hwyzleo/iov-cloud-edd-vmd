package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * 基础领域事件
 *
 * @author hwyz_leo
 */
@Getter
public class BaseEvent extends ApplicationEvent {

    /**
     * 领域事件唯一ID
     */
    private final String id;

    public BaseEvent() {
        super(UUID.randomUUID().toString());
        this.id = String.valueOf(getSource());
    }

    public BaseEvent(Object source) {
        super(source);
        this.id = UUID.randomUUID().toString();
    }

}
