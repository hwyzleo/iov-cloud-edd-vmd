# Series-Brand Model-Platform Relationship Refactor Design

## Summary
Change Series from platformCode to brandCode. Model's platformCode becomes independent input (not derived from Series).

## Context
Current: Series has platformCode → belongs to Platform
Change: Series has brandCode → belongs to Brand (Brand and Platform are independent)

## Design Decisions
- Approach: Minimal change (no validation logic added)
- Brand-Platform relationship: Independent entities (no relation)
- Migration: User handles manually
- Series code format: Keep unchanged

## Database Changes

### tb_veh_series
```sql
ALTER TABLE tb_veh_series DROP COLUMN platform_code;
ALTER TABLE tb_veh_series ADD COLUMN brand_code VARCHAR(32);
CREATE INDEX idx_series_brand_code ON tb_veh_series(brand_code);
```

### tb_veh_model
No changes needed - already has platform_code and series_code.

## Code Changes

### Series Layer (10 files)
Replace `platformCode` with `brandCode` in:
- Entity: `Series.java`
- PO: `VehSeriesPo.java`
- CMD: `SeriesCmd.java`
- DTO: `SeriesDto.java`
- Query: `SeriesQuery.java`
- Request: `SeriesRequest.java`
- Response: `SeriesResponse.java`
- Service: `SeriesAppService.java` (line 44)
- Controller: `MptSeriesController.java` (rename listByPlatformCode → listByBrandCode)
- Mapper: `VehSeriesMapper.xml` (replace all platform_code → brand_code)

### Model Layer
No changes needed - platformCode is already independent input.

## API Changes
- `/api/mpt/series/v1/list`: parameter platformCode → brandCode
- `/api/mpt/series/v1/listByPlatformCode`: rename to `listByBrandCode`
- `/api/mpt/model/v1/listByPlatformCodeAndSeriesCode`: no change

## Affected Files List
```
domain/model/entity/Series.java
infrastructure/persistence/po/VehSeriesPo.java
application/dto/cmd/SeriesCmd.java
application/dto/result/SeriesDto.java
application/dto/query/SeriesQuery.java
adapter/web/vo/request/SeriesRequest.java
adapter/web/vo/response/SeriesResponse.java
application/service/SeriesAppService.java
adapter/web/controller/mpt/MptSeriesController.java
resources/mappers/VehSeriesMapper.xml
```

## Migration Order
1. Add brand_code column (allow user to populate)
2. Remove platform_code column after user confirms data ready

## Testing Checklist
1. Series creation requires brand selection
2. Series query filters by brand
3. Model creation accepts platform and series as independent inputs
4. Model query filters by platform + series combination