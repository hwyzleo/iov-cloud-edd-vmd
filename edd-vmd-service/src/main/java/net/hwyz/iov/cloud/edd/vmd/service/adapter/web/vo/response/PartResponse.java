package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response;

import lombok.*;

import java.util.Date;

/**
 * 管理后台零件信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PartResponse {

    /**
     * 主键
     */
    private Long id;

    /**
     * 零件号
     */
    private String pn;

    /**
     * 零件中文名称
     */
    private String name;

    /**
     * 零件英文名称
     */
    private String nameEn;

    /**
     * 零件类型
     */
    private String type;

    /**
     * 零件分类
     */
    private String ffa;

    /**
     * 零件状态：PRODUCTION-量产，TRIAL-试生产，DISCONTINUE-停用
     */
    private String status;

    /**
     * 数字模型：NO-无，PART-零件级，PRODUCT-总成级
     */
    private String digitalModel;

    /**
     * 单位：EA-个，KG-千克，ML-毫升，M2-平方米，M-米
     */
    private String unit;

    /**
     * 是否是架构件
     */
    private Boolean framePart;

    /**
     * 是否是本色件
     */
    private Boolean naturePart;

    /**
     * 颜色区域：INTERNAL-内部，EXTERNAL-外部
     */
    private String colorArea;

    /**
     * 本色件零件号
     */
    private String naturePn;

    /**
     * 是否是法规件
     */
    private Boolean regulatoryPart;

    /**
     * 关键程度：KEY-关键，MAJOR-主要，SIMPLE-普通
     */
    private String keyPart;

    /**
     * 是否精准追溯
     */
    private Boolean accuratelyTraced;

    /**
     * 是否是配件
     */
    private Boolean aftersalePart;

    /**
     * 标准件分类
     */
    private String standardPartClass;

    /**
     * 扳拧形式
     */
    private String wrenchType;

    /**
     * 杆部形式
     */
    private String rodType;

    /**
     * 头部形状
     */
    private String headShape;

    /**
     * 末端形状
     */
    private String endShape;

    /**
     * 是否带垫圈
     */
    private Boolean washer;

    /**
     * 垫圈类型
     */
    private String washerType;

    /**
     * 直径
     */
    private String diameter;

    /**
     * 长度
     */
    private String length;

    /**
     * 螺距
     */
    private String pitch;

    /**
     * 牙型
     */
    private String dentalForm;

    /**
     * 强度等级
     */
    private String strengthGrade;

    /**
     * 机械性能
     */
    private String mechanicalProperty;

    /**
     * 表面处理
     */
    private String surfaceTreatment;

    /**
     * 结构特征
     */
    private String structureCharacter;

    /**
     * 设备形态
     */
    private String deviceForm;

    /**
     * 设备代码
     */
    private String deviceCode;

    /**
     * 设计工程师
     */
    private String designer;

    /**
     * 设计工程师部门
     */
    private String designerDept;

    /**
     * 不作为备件原因
     */
    private String nonRepairReason;

    /**
     * 是否颜色件维修
     */
    private Boolean colorRepair;

    /**
     * 是否底漆件维修
     */
    private Boolean primerRepair;

    /**
     * 是否电泳件维修
     */
    private Boolean electrophoresisRepair;

    /**
     * 对应生产件号
     */
    private String productionCode;

    /**
     * 售后配件属性
     */
    private String spareProperty;

    /**
     * 售后备注
     */
    private String saleNote;

    /**
     * 首次投产时间
     */
    private String firstProductionDate;

    /**
     * 初始车型
     */
    private String initialModel;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 关键词
     */
    private String key;

}
