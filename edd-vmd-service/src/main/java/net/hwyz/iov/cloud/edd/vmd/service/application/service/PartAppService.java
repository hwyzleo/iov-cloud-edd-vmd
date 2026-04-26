package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PartVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.PartAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 零件应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartAppService {

    private final PartRepository partRepository;

    /**
     * 查询零件信息
     *
     * @param key        关键程度
     * @param pn         零件号
     * @param name       零件名称
     * @param type       零件类型
     * @param deviceCode 设备代码
     * @param beginTime  开始时间
     * @param endTime    结束时间
     * @return 零件列表
     */
    public List<PartVo> search(String key, String pn, String name, String type, String deviceCode, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("keyPart", key);
        map.put("pn", pn);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("type", type);
        map.put("deviceCode", deviceCode);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<Part> partList = partRepository.selectByMap(map);
        return PageUtil.convert(partList, PartAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查零件号是否唯一
     *
     * @param partId 零件ID
     * @param pn     零件号
     * @return 结果
     */
    public Boolean checkPnUnique(Long partId, String pn) {
        if (ObjUtil.isNull(partId)) {
            partId = -1L;
        }
        Part part = partRepository.selectByPn(pn);
        return !ObjUtil.isNotNull(part) || part.getId().longValue() == partId.longValue();
    }

    /**
     * 根据主键ID获取零件信息
     *
     * @param id 主键ID
     * @return 零件信息
     */
    public PartVo getPartById(Long id) {
        return PartAssembler.INSTANCE.fromDomain(partRepository.selectById(id));
    }

    /**
     * 根据零件号获取零件信息
     *
     * @param pn 零件号
     * @return 零件领域对象
     */
    public Part getPartByPn(String pn) {
        return partRepository.selectByPn(pn);
    }

    /**
     * 获取所有FOTA升级零件信息
     *
     * @param software 是否是软件零件
     * @return 零件列表
     */
    public List<Part> listAllFota(Boolean software) {
        Map<String, Object> map = new HashMap<>();
        if (software != null) {
            map.put("naturePart", software ? 1 : 0);
        }
        return partRepository.selectByMap(map);
    }

    /**
     * 新增零件
     *
     * @param partVo 零件信息
     * @param userId 操作用户ID
     * @return 结果
     */
    public int createPart(PartVo partVo, String userId) {
        Part part = PartAssembler.INSTANCE.toDomain(partVo);
        return partRepository.insert(part);
    }

    /**
     * 修改零件
     *
     * @param partVo 零件信息
     * @param userId 操作用户ID
     * @return 结果
     */
    public int modifyPart(PartVo partVo, String userId) {
        Part part = PartAssembler.INSTANCE.toDomain(partVo);
        return partRepository.update(part);
    }

    /**
     * 批量删除零件
     *
     * @param ids 零件ID数组
     * @return 结果
     */
    public int deletePartByIds(Long[] ids) {
        return partRepository.batchPhysicalDelete(ids);
    }

}
