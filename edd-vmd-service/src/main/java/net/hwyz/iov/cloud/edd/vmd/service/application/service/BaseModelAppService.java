package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelFeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BaseModelAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BaseModelFeatureCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModel;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModelFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBaseModelRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBuildConfigRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础车型应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaseModelAppService {

    private final VehBaseModelRepository vehBaseModelRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final VehBuildConfigRepository vehBuildConfigRepository;

    // ==================== 基础车型 ====================

    /**
     * 查询基础车型信息
     *
     * @param platformCode 车辆平台代码
     * @param seriesCode   车系代码
     * @param modelCode    车型代码
     * @param code         基础车型代码
     * @param name         基础车型名称
     * @param beginTime    开始时间
     * @param endTime      结束时间
     * @return 基础车型列表
     */
    public List<BaseModelVo> search(String platformCode, String seriesCode, String modelCode, String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformCode);
        map.put("seriesCode", seriesCode);
        map.put("modelCode", modelCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<BaseModel> baseModelList = vehBaseModelRepository.selectByMap(map);
        return PageUtil.convert(baseModelList, BaseModelAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查基础车型代码是否唯一
     *
     * @param baseModelId 基础车型ID
     * @param code        基础车型代码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long baseModelId, String code) {
        if (ObjUtil.isNull(baseModelId)) {
            baseModelId = -1L;
        }
        BaseModel baseModel = getBaseModelByCode(code);
        return !ObjUtil.isNotNull(baseModel) || baseModel.getId().longValue() == baseModelId.longValue();
    }

    /**
     * 检查基础车型下是否存在车型配置
     *
     * @param baseModelId 基础车型ID
     * @return 结果
     */
    public Boolean checkBaseModelBuildConfigExist(Long baseModelId) {
        BaseModel baseModel = vehBaseModelRepository.selectById(baseModelId);
        Map<String, Object> map = new HashMap<>();
        map.put("baseModelCode", baseModel.getCode());
        return vehBuildConfigRepository.countByMap(map) > 0;
    }

    /**
     * 检查基础车型下是否存在车辆
     *
     * @param baseModelId 基础车型ID
     * @return 结果
     */
    public Boolean checkBaseModelVehicleExist(Long baseModelId) {
        BaseModel baseModel = vehBaseModelRepository.selectById(baseModelId);
        Map<String, Object> map = new HashMap<>();
        map.put("baseModelCode", baseModel.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    /**
     * 根据主键ID获取基础车型信息
     *
     * @param id 主键ID
     * @return 基础车型信息
     */
    public BaseModelVo getBaseModelById(Long id) {
        return BaseModelAssembler.INSTANCE.fromDomain(vehBaseModelRepository.selectById(id));
    }

    /**
     * 根据基础车型代码获取基础车型信息
     *
     * @param code 基础车型代码
     * @return 基础车型领域对象
     */
    public BaseModel getBaseModelByCode(String code) {
        return vehBaseModelRepository.selectByCode(code);
    }

    /**
     * 新增基础车型
     *
     * @param baseModelVo 基础车型信息
     * @param userId      操作用户ID
     * @return 结果
     */
    public int createBasicModel(BaseModelVo baseModelVo, String userId) {
        BaseModel baseModel = BaseModelAssembler.INSTANCE.toDomain(baseModelVo);
        return vehBaseModelRepository.insert(baseModel);
    }

    /**
     * 修改基础车型
     *
     * @param baseModelVo 基础车型信息
     * @param userId      操作用户ID
     * @return 结果
     */
    public int modifyBasicModel(BaseModelVo baseModelVo, String userId) {
        BaseModel baseModel = BaseModelAssembler.INSTANCE.toDomain(baseModelVo);
        return vehBaseModelRepository.update(baseModel);
    }

    /**
     * 批量删除基础车型
     *
     * @param ids 基础车型ID数组
     * @return 结果
     */
    public int deleteBasicModelByIds(Long[] ids) {
        return vehBaseModelRepository.batchPhysicalDelete(ids);
    }

    // ==================== 基础车型特征 ====================

    /**
     * 查询基础车型特征关系信息
     *
     * @param baseModelCode 基础车型编码
     * @param familyCode    特征族编码
     * @param beginTime     开始时间
     * @param endTime       结束时间
     * @return 基础车型特征关系列表
     */
    public List<BaseModelFeatureCodeVo> searchFeatureCode(String baseModelCode, String familyCode, Date beginTime, Date endTime) {
        BaseModelFeatureCode example = BaseModelFeatureCode.builder()
                .baseModelCode(baseModelCode)
                .familyCode(familyCode)
                .build();
        List<BaseModelFeatureCode> list = vehBaseModelRepository.selectFeatureCodeByExample(example);
        return PageUtil.convert(list, BaseModelFeatureCodeAssembler.INSTANCE::fromDomain);
    }

    /**
     * 根据主键ID获取基础车型特征关系信息
     *
     * @param id 主键ID
     * @return 基础车型特征关系信息
     */
    public BaseModelFeatureCodeVo getBaseModelFeatureCodeById(Long id) {
        List<BaseModelFeatureCode> list = vehBaseModelRepository.selectFeatureCodeByExample(BaseModelFeatureCode.builder().id(id).build());
        return list.isEmpty() ? null : BaseModelFeatureCodeAssembler.INSTANCE.fromDomain(list.get(0));
    }

    /**
     * 检查基础车型特征关系是否唯一
     *
     * @param id            主键ID
     * @param baseModelCode 基础车型编码
     * @param familyCode    特征族编码
     * @return 结果
     */
    public Boolean checkFeatureCodeUnique(Long id, String baseModelCode, String familyCode) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        List<BaseModelFeatureCode> list = vehBaseModelRepository.selectFeatureCodeByExample(BaseModelFeatureCode.builder()
                .baseModelCode(baseModelCode)
                .familyCode(familyCode)
                .build());
        return list.isEmpty() || list.get(0).getId().longValue() == id.longValue();
    }

    /**
     * 新增基础车型特征关系
     *
     * @param featureCodeVo 基础车型特征关系信息
     * @param userId        操作用户ID
     * @return 结果
     */
    public int createBasicModelFeatureCode(BaseModelFeatureCodeVo featureCodeVo, String userId) {
        BaseModelFeatureCode featureCode = BaseModelFeatureCodeAssembler.INSTANCE.toDomain(featureCodeVo);
        return vehBaseModelRepository.batchInsertFeatureCode(List.of(featureCode));
    }

    /**
     * 修改基础车型特征关系
     *
     * @param featureCodeVo 基础车型特征关系信息
     * @param userId        操作用户ID
     * @return 结果
     */
    public int modifyBaseModelFeatureCode(BaseModelFeatureCodeVo featureCodeVo, String userId) {
        BaseModelFeatureCode featureCode = BaseModelFeatureCodeAssembler.INSTANCE.toDomain(featureCodeVo);
        return vehBaseModelRepository.updateFeatureCode(featureCode);
    }

    /**
     * 批量删除基础车型特征关系
     *
     * @param ids 主键ID数组
     * @return 结果
     */
    public int deleteBaseModelFeatureCodeByIds(Long[] ids) {
        return vehBaseModelRepository.batchPhysicalDeleteFeatureCode(ids);
    }

}
