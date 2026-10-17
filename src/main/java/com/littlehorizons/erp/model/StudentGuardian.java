package com.littlehorizons.erp.model;

/**
 * A guardian linked to a student, with the data of the link
 * (table: estudiante_acudiente).
 */
public record StudentGuardian(Guardian guardian, boolean primary, boolean emergencyContact) { }