package com.example;

import com.example.phone.PhoneValidationService;
import com.example.phone.TestPhoneValidationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueNullAndEmptySourceExampleParameterizedTest {

    private final PhoneValidationService phoneValidationService = new TestPhoneValidationService();

    @ParameterizedTest
    @ValueSource(strings = {"555 555 55 55", "5555555555", "+15555555555"})
    void testProcessValidPhones(String phone) {
        assertTrue(phoneValidationService.validatePhone(phone));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"555", "@+15555555555", "test"})
    void testProcessInvalidPhones(String phone) {
        assertFalse(phoneValidationService.validatePhone(phone));
    }
}
