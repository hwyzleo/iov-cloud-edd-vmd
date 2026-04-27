package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 零件 DTO
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartDto {

    private Long id;
    private String pn;
    private String name;
    private String nameEn;
    private String type;
    private String ffa;
    private String status;
    private String digitalModel;
    private String unit;
    private Boolean framePart;
    private Boolean naturePart;
    private String colorArea;
    private String naturePn;
    private Boolean regulatoryPart;
    private String keyPart;
    private Boolean accuratelyTraced;
    private Boolean aftersalePart;
    private String standardPartClass;
    private String wrenchType;
    private String rodType;
    private String headShape;
    private String endShape;
    private Boolean washer;
    private String washerType;
    private String diameter;
    private String length;
    private String pitch;
    private String dentalForm;
    private String strengthGrade;
    private String mechanicalProperty;
    private String surfaceTreatment;
    private String structureCharacter;
    private String deviceForm;
    private String deviceCode;
    private String designer;
    private String designerDept;
    private String nonRepairReason;
    private Boolean colorRepair;
    private Boolean primerRepair;
    private Boolean electrophoresisRepair;
    private String productionCode;
    private String spareProperty;
    private String saleNote;
    private String firstProductionDate;
    private String initialModel;
    private String description;

}
