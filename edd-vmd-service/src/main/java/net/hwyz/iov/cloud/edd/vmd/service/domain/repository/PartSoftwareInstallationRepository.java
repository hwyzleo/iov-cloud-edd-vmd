package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;

import java.util.List;

/**
 * 软件实装时态记录数据仓库接口
 *
 * @author hwyz_leo
 */
public interface PartSoftwareInstallationRepository {

    /**
     * 根据主键ID查询软件实装记录
     *
     * @param id 主键ID
     * @return 软件实装记录
     */
    PartSoftwareInstallation selectById(Long id);

    /**
     * 根据零件ID查询软件实装记录列表
     *
     * @param partId 零件ID
     * @return 软件实装记录列表
     */
    List<PartSoftwareInstallation> selectByPartId(Long partId);

    /**
     * 根据零件ID和软件目标代码查询当前活跃的软件实装记录
     *
     * @param partId 零件ID
     * @param softwareTargetCode 软件目标代码
     * @return 软件实装记录
     */
    PartSoftwareInstallation selectActiveByPartIdAndTargetCode(Long partId, String softwareTargetCode);

    /**
     * 新增软件实装记录
     *
     * @param partSoftwareInstallation 软件实装记录
     * @return 影响行数
     */
    int insert(PartSoftwareInstallation partSoftwareInstallation);

    /**
     * 更新软件实装记录
     *
     * @param partSoftwareInstallation 软件实装记录
     * @return 影响行数
     */
    int update(PartSoftwareInstallation partSoftwareInstallation);

    /**
     * 根据零件ID和软件目标代码停用当前活跃的软件实装记录
     *
     * @param partId 零件ID
     * @param softwareTargetCode 软件目标代码
     * @return 影响行数
     */
    int deactivateByPartIdAndTargetCode(Long partId, String softwareTargetCode);

    /**
     * 根据来源和来源事件幂等键查询软件实装记录
     *
     * @param source 来源
     * @param sourceEventId 来源事件幂等键
     * @param softwareTargetCode 软件目标代码
     * @param slot 槽位
     * @return 软件实装记录（可能为null）
     */
    PartSoftwareInstallation selectBySourceAndSourceEventId(String source, String sourceEventId,
                                                            String softwareTargetCode, String slot);
}
