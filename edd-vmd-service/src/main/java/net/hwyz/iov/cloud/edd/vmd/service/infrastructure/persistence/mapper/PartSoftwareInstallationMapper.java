package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartSoftwareInstallationPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 软件实装时态表 Mapper 接口
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartSoftwareInstallationMapper extends BaseMapper<PartSoftwareInstallationPo> {

    /**
     * 根据零件ID和软件目标代码停用当前活跃记录
     *
     * @param partId 零件ID
     * @param softwareTargetCode 软件目标代码
     * @return 影响行数
     */
    int deactivateByPartIdAndTargetCode(@Param("partId") Long partId, @Param("softwareTargetCode") String softwareTargetCode);
}
