package net.hwyz.iov.cloud.edd.vmd.service.application;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.PartVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.PartMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.PartDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdPartDo;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 零件信息应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartAppService {

    private final PartDao partDao;

    /**
     * 查询零件信息
     *
     * @param key        关键字
     * @param pn         零件号
     * @param name       零件名称
     * @param type       零件类型
     * @param deviceCode 设备代码
     * @param beginTime  开始时间
     * @param endTime    结束时间
     * @return 查询车载列表
     */
    public List<PartVo> search(String key, String pn, String name, String type, String deviceCode, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("key", ParamHelper.fuzzyQueryParam(key));
        map.put("pn", StringUtils.isBlank(pn) ? null : pn.trim() + "%");
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("type", type);
        map.put("deviceCode", deviceCode);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<VmdPartDo> vmdPartDoList = partDao.selectPoByMap(map);
        return PageUtil.convert(vmdPartDoList, PartMapper.INSTANCE::fromDo);
    }

    /**
     * 获取所有FOTA升级零件
     *
     * @param software 是否是软件零件
     * @return 零件信息列表
     */
    public List<VmdPartDo> listAllFota(Boolean software) {
        return partDao.selectAllFotaPo(software);
    }

    /**
     * 检查零件号是否唯一
     *
     * @param partId 零件信息ID
     * @param pn     零件号
     * @return 结果
     */
    public Boolean checkPnUnique(Long partId, String pn) {
        if (ObjUtil.isNull(partId)) {
            partId = -1L;
        }
        VmdPartDo partPo = getPartByPn(pn);
        return !ObjUtil.isNotNull(partPo) || partPo.getId().longValue() == partId.longValue();
    }

    /**
     * 根据主键ID获取零件信息
     *
     * @param id 主键ID
     * @return 零件信息
     */
    public VmdPartDo getPartById(Long id) {
        return partDao.selectPoById(id);
    }

    /**
     * 根据零件号获取零件信息
     *
     * @param pn 零件号
     * @return 零件信息
     */
    public VmdPartDo getPartByPn(String pn) {
        return partDao.selectPoByPn(pn);
    }

    /**
     * 新增零件信息
     *
     * @param part 零件信息
     * @return 结果
     */
    public int createPart(VmdPartDo part) {
        return partDao.insertPo(part);
    }

    /**
     * 修改零件信息
     *
     * @param part 零件信息
     * @return 结果
     */
    public int modifyPart(VmdPartDo part) {
        return partDao.updatePo(part);
    }

    /**
     * 批量删除零件信息
     *
     * @param ids 零件信息ID数组
     * @return 结果
     */
    public int deletePartByIds(Long[] ids) {
        return partDao.batchPhysicalDeletePo(ids);
    }

}
