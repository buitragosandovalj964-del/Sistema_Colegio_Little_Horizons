package com.littlehorizons.erp.service;

import java.util.List;

/**
 * Shared validation rules for fields used by several modules (students,
 * guardians, teachers). Every method throws ValidationException with a
 * message meant for the user.
 */
public final class FieldValidator {

    public static final int MAX_DOCUMENT_NUMBER_LENGTH = 10;
    public static final int PHONE_MIN_LENGTH = 7;
    public static final int PHONE_MAX_LENGTH = 10;

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final String NAME_PATTERN = "\\p{L}+([ '\\-]\\p{L}+)*";
    private static final String DOCUMENT_NUMBER_PATTERN = "[A-Za-z0-9]+";
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private FieldValidator() {
        // Utility class: it should not be instantiated.
    }

    public static void requireOneOf(String value, List<String> allowed, String label)
            throws ValidationException {
        if (value == null || !allowed.contains(value)) {
            throw new ValidationException(label + " debe ser uno de: "
                    + String.join(", ", allowed) + ".");
        }
    }

    public static void validateDocumentNumber(String number) throws ValidationException {
        if (number == null || number.isBlank()) {
            throw new ValidationException("El número de documento es obligatorio.");
        }
        if (number.length() > MAX_DOCUMENT_NUMBER_LENGTH) {
            throw new ValidationException("El número de documento debe tener máximo "
                    + MAX_DOCUMENT_NUMBER_LENGTH + " caracteres.");
        }
        if (!number.matches(DOCUMENT_NUMBER_PATTERN)) {
            throw new ValidationException(
                    "El número de documento solo puede contener letras y dígitos.");
        }
    }

    public static void validateName(String value, String label, boolean required)
            throws ValidationException {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new ValidationException(label + " es obligatorio.");
            }
            return;
        }
        if (value.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(label + " debe tener máximo " + MAX_NAME_LENGTH
                    + " caracteres.");
        }
        if (!value.matches(NAME_PATTERN)) {
            throw new ValidationException(label
                    + " solo puede contener letras, espacios, guiones y apóstrofes.");
        }
    }

    public static void validatePhone(String phone, String label, boolean required)
            throws ValidationException {
        if (phone == null || phone.isBlank()) {
            if (required) {
                throw new ValidationException(label + " es obligatorio.");
            }
            return;
        }
        if (!phone.matches("\\d+") || phone.length() < PHONE_MIN_LENGTH
                || phone.length() > PHONE_MAX_LENGTH) {
            throw new ValidationException(label + " debe tener solo números, entre "
                    + PHONE_MIN_LENGTH + " y " + PHONE_MAX_LENGTH + " dígitos.");
        }
    }

    public static void validateEmail(String email) throws ValidationException {
        if (email == null || email.isBlank()) {
            return;
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException("El correo electrónico debe tener máximo "
                    + MAX_EMAIL_LENGTH + " caracteres.");
        }
        if (!email.matches(EMAIL_PATTERN)) {
            throw new ValidationException("El formato del correo electrónico no es válido.");
        }
    }

    public static void validateMaxLength(String value, String label, int maxLength)
            throws ValidationException {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(label + " debe tener máximo " + maxLength
                    + " caracteres.");
        }
    }
}