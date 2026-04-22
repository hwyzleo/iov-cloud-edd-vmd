package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 零件信息表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-26
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_part")
public class VmdPartDo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 零件号
     */
    @TableField("pn")
    private String pn;

    /**
     * 零件中文名称
     */
    @TableField("name")
    private String name;

    /**
     * 零件英文名称
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 零件类型
     */
    @TableField("type")
    private String type;

    /**
     * 零件分类
     */
    @TableField("ffa")
    private String ffa;

    /**
     * 零件状态：PRODUCTION-量产，TRIAL-试生产，DISCONTINUE-停用
     */
    @TableField("status")
    private String status;

    /**
     * 数字模型：NO-无，PART-零件级，PRODUCT-总成级
     */
    @TableField("digital_model")
    private String digitalModel;

    /**
     * 单位：EA-个，KG-千克，ML-毫升，M2-平方米，M-米
     */
    @TableField("unit")
    private String unit;

    /**
     * 是否是架构件
     */
    @TableField("frame_part")
    private Boolean framePart;

    /**
     * 是否是本色件
     */
    @TableField("nature_part")
    private Boolean naturePart;

    /**
     * 颜色区域：INTERNAL-内部，EXTERNAL-外部
     */
    @TableField("color_area")
    private String colorArea;

    /**
     * 本色件零件号
     */
    @TableField("nature_pn")
    private String naturePn;

    /**
     * 是否是法规件
     */
    @TableField("regulatory_part")
    private Boolean regulatoryPart;

    /**
     * 关键程度：KEY-关键，MAJOR-主要，SIMPLE-普通
     */
    @TableField("key_part")
    private String keyPart;

    /**
     * 是否精准追溯
     */
    @TableField("accurately_traced")
    private Boolean accuratelyTraced;

    /**
     * 是否是配件
     */
    @TableField("aftersale_part")
    private Boolean aftersalePart;

    /**
     * 标准件分类
     */
    @TableField("standard_part_class")
    private String standardPartClass;

    /**
     * 扳拧形式
     */
    @TableField("wrench_type")
    private String wrenchType;

    /**
     * 杆部形式
     */
    @TableField("rod_type")
    private String rodType;

    /**
     * 头部形状
     */
    @TableField("head_shape")
    private String headShape;

    /**
     * 末端形状
     */
    @TableField("end_shape")
    private String endShape;

    /**
     * 是否带垫圈
     */
    @TableField("washer")
    private Boolean washer;

    /**
     * 垫圈类型
     */
    @TableField("washer_type")
    private String washerType;

    /**
     * 直径
     */
    @TableField("diameter")
    private String diameter;

    /**
     * 长度
     */
    @TableField("length")
    private String length;

    /**
     * 螺距
     */
    @TableField("pitch")
    private String pitch;

    /**
     * 牙型
     */
    @TableField("dental_form")
    private String dentalForm;

    /**
     * 强度等级
     */
    @TableField("strength_grade")
    private String strengthGrade;

    /**
     * 机械性能
     */
    @TableField("mechanical_property")
    private String mechanicalProperty;

    /**
     * 表面处理
     */
    @TableField("surface_treatment")
    private String surfaceTreatment;

    /**
     * 结构特征
     */
    @TableField("structure_character")
    private String structureCharacter;

    /**
     * 设备形态
     */
    @TableField("device_form")
    private String deviceForm;

    /**
     * 设备代码
     */
    @TableField("device_code")
    private String deviceCode;

    /**
     * 设计工程师
     */
    @TableField("designer")
    private String designer;

    /**
     * 设计工程师部门
     */
    @TableField("designer_dept")
    private String designerDept;

    /**
     * 不作为备件原因
     */
    @TableField("non_repair_reason")
    private String nonRepairReason;

    /**
     * 是否颜色件维修
     */
    @TableField("color_repair")
    private Boolean colorRepair;

    /**
     * 是否底漆件维修
     */
    @TableField("primer_repair")
    private Boolean primerRepair;

    /**
     * 是否电泳件维修
     */
    @TableField("electrophoresis_repair")
    private Boolean electrophoresisRepair;

    /**
     * 对应生产件号
     */
    @TableField("production_code")
    private String productionCode;

    /**
     * 售后配件属性
     */
    @TableField("spare_property")
    private String spareProperty;

    /**
     * 售后备注
     */
    @TableField("sale_note")
    private String saleNote;

    /**
     * 首次投产时间
     */
    @TableField("first_production_date")
    private String firstProductionDate;

    /**
     * 初始车型
     */
    @TableField("initial_model")
    private String initialModel;
}
