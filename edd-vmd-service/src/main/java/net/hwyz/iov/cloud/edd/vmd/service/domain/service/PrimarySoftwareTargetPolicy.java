package net.hwyz.iov.cloud.edd.vmd.service.domain.service;

/**
 * 主软件 Target 策略（CR-071 §5.1）
 * <p>
 * 主软件判定：在该零件所有 confirmed ACTIVE 候选（active slot）中，
 * 按 SOFTWARE_TYPE 的显式优先级选最高者作为兼容投影来源；
 * 优先级未配置的类型不参与主选（→ AMBIGUOUS），绝不取 SOFTWARE[0] 或数据库第一条。
 * <p>
 * 策略形态为"类型优先级列表"（如 APP &gt; CALIBRATION &gt; BOOT），
 * 全局通用，不按零件/节点写死映射。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
public interface PrimarySoftwareTargetPolicy {

    /**
     * 返回软件类型优先级（数值越小越主）
     *
     * @param softwareType 软件类型（SOFTWARE_TYPE，如 APP/BOOT/CALIBRATION）
     * @return 优先级索引（0 为最主）；未配置或为空返回 -1（不参与主选）
     */
    int priorityOf(String softwareType);

}
