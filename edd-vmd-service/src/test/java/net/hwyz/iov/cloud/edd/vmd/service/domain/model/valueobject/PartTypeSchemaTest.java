package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PartTypeSchemaTest {

    @Test
    void validateRequired_allFieldsPresent_emptyList() {
        // Given
        PartTypeSchema schema = PartTypeSchema.builder()
                .requiredFields(List.of("sn", "iccid"))
                .build();

        Map<String, String> fields = new HashMap<>();
        fields.put("sn", "SN001");
        fields.put("iccid", "ICCID001");

        // When
        List<String> missing = schema.validateRequired(fields);

        // Then
        assertTrue(missing.isEmpty());
    }

    @Test
    void validateRequired_missingFields_returnsList() {
        // Given
        PartTypeSchema schema = PartTypeSchema.builder()
                .requiredFields(List.of("sn", "iccid"))
                .build();

        Map<String, String> fields = new HashMap<>();
        fields.put("sn", "SN001");

        // When
        List<String> missing = schema.validateRequired(fields);

        // Then
        assertEquals(1, missing.size());
        assertTrue(missing.contains("iccid"));
    }

    @Test
    void validateRequired_nullValue_returnsList() {
        // Given
        PartTypeSchema schema = PartTypeSchema.builder()
                .requiredFields(List.of("sn"))
                .build();

        Map<String, String> fields = new HashMap<>();
        fields.put("sn", null);

        // When
        List<String> missing = schema.validateRequired(fields);

        // Then
        assertEquals(1, missing.size());
        assertTrue(missing.contains("sn"));
    }

    @Test
    void validateRequired_emptyRequiredFields_emptyList() {
        // Given
        PartTypeSchema schema = PartTypeSchema.builder()
                .requiredFields(List.of())
                .build();

        Map<String, String> fields = new HashMap<>();

        // When
        List<String> missing = schema.validateRequired(fields);

        // Then
        assertTrue(missing.isEmpty());
    }

    @Test
    void validateRequired_nullRequiredFields_emptyList() {
        // Given
        PartTypeSchema schema = PartTypeSchema.builder()
                .requiredFields(null)
                .build();

        Map<String, String> fields = new HashMap<>();

        // When
        List<String> missing = schema.validateRequired(fields);

        // Then
        assertTrue(missing.isEmpty());
    }

    @Test
    void normalizeExtra_validFields_returnsJson() {
        // Given
        PartTypeSchema schema = PartTypeSchema.builder().build();

        Map<String, Object> rawFields = new HashMap<>();
        rawFields.put("hsm", "HSM001");
        rawFields.put("mac", "MAC001");

        // When
        String result = schema.normalizeExtra(rawFields);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("hsm"));
        assertTrue(result.contains("HSM001"));
    }

    @Test
    void normalizeExtra_withNullValues_filtersNull() {
        // Given
        PartTypeSchema schema = PartTypeSchema.builder().build();

        Map<String, Object> rawFields = new HashMap<>();
        rawFields.put("hsm", "HSM001");
        rawFields.put("mac", null);

        // When
        String result = schema.normalizeExtra(rawFields);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("hsm"));
        assertFalse(result.contains("mac"));
    }

    @Test
    void normalizeExtra_emptyFields_returnsNull() {
        // Given
        PartTypeSchema schema = PartTypeSchema.builder().build();

        Map<String, Object> rawFields = new HashMap<>();

        // When
        String result = schema.normalizeExtra(rawFields);

        // Then
        assertNull(result);
    }

    @Test
    void normalizeExtra_nullFields_returnsNull() {
        // Given
        PartTypeSchema schema = PartTypeSchema.builder().build();

        // When
        String result = schema.normalizeExtra(null);

        // Then
        assertNull(result);
    }
}
