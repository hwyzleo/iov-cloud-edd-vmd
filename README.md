# iov-cloud-edd-vmd

从零开始练手车联网云端企业数字底座车辆主数据

## Security Constant Implementation (CR-031 & CR-032)

### Overview

This implementation covers two design changes:

- **CR-032**: Refactor vehicle security constant storage (remove cipher_blob, rename key_handle to kms_key_ref)
- **CR-031**: Implement device-level security constant preset (ROOT)

### Key Components

- `VehSecurityConstant`: Vehicle-level security constant entity (refactored)
- `PartSecurityConstant`: Device-level security constant entity (new)
- `PartTypeSchema`: Part type schema with security whitelist (new)
- `KmsHsmClient`: KMS/HSM client interface (new)
- `VehicleSecurityPresetAppService`: Vehicle-level preset service (updated)
- `PartSecurityPresetAppService`: Device-level preset service (new)

### Database Changes

- `tb_veh_security_constant`: Remove cipher_blob column, rename key_handle to kms_key_ref
- `tb_part_security_constant`: New table for device-level security constants

### Error Codes

- `202021`: SecurityConstantPresetFailedException
- `202022`: KmsHsmUnavailableException
