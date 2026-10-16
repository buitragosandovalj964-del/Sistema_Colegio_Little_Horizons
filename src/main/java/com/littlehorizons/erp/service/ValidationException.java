package com.littlehorizons.erp.service;

/**
 * Thrown when user data breaks a business rule (for example, a missing name).
 * The message is meant to be shown to the user.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}