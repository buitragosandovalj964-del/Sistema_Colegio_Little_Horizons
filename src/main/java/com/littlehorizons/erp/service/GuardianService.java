package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.model.Guardian;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for guardians. Validates data before it reaches the database.
 * User-facing messages are in Spanish.
 */
public class GuardianService {

    public static final List<String> DOCUMENT_TYPES = List.of("CC", "CE", "Pasaporte");
    public static final List<String> RELATIONSHIPS = List.of("Padre", "Madre", "Abuelo",
            "Abuela", "Tío", "Tía", "Hermano", "Hermana", "Tutor Legal", "Otro");

    private final GuardianDao guardianDao;

    public GuardianService() {
        this(new GuardianDao());
    }

    public GuardianService(GuardianDao guardianDao) {
        this.guardianDao = guardianDao;
    }

    public void registerGuardian(Guardian guardian) throws ValidationException, SQLException {
        validate(guardian);
        if (guardianDao.findByDocument(guardian.getDocumentType(),
                guardian.getDocumentNumber()).isPresent()) {
            throw new ValidationException("Ya existe un acudiente con el documento "
                    + guardian.getDocumentType() + " " + guardian.getDocumentNumber() + ".");
        }
        guardianDao.insert(guardian);
    }

    public void updateGuardian(Guardian guardian) throws ValidationException, SQLException {
        if (guardian.getId() == null) {
            throw new ValidationException(
                    "El acudiente no tiene id, por lo que no se puede actualizar.");
        }
        validate(guardian);
        Optional<Guardian> sameDocument = guardianDao.findByDocument(
                guardian.getDocumentType(), guardian.getDocumentNumber());
        if (sameDocument.isPresent() && !sameDocument.get().getId().equals(guardian.getId())) {
            throw new ValidationException("Otro acudiente ya usa el documento "
                    + guardian.getDocumentType() + " " + guardian.getDocumentNumber() + ".");
        }
        if (!guardianDao.update(guardian)) {
            throw new ValidationException(
                    "No se encontró el acudiente con id " + guardian.getId() + ".");
        }
    }

    public void deleteGuardian(int id) throws ValidationException, SQLException {
        boolean deleted;
        try {
            deleted = guardianDao.deleteById(id);
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("No se puede eliminar el acudiente porque tiene "
                    + "registros asociados (por ejemplo, pagos).");
        }
        if (!deleted) {
            throw new ValidationException("No se encontró el acudiente con id " + id + ".");
        }
    }

    public Optional<Guardian> findById(int id) throws SQLException {
        return guardianDao.findById(id);
    }

    public Optional<Guardian> findByDocument(String documentType, String documentNumber)
            throws SQLException {
        return guardianDao.findByDocument(documentType, documentNumber);
    }

    public List<Guardian> listGuardians() throws SQLException {
        return guardianDao.findAll();
    }

    private void validate(Guardian guardian) throws ValidationException {
        FieldValidator.requireOneOf(guardian.getDocumentType(), DOCUMENT_TYPES,
                "El tipo de documento");
        FieldValidator.validateDocumentNumber(guardian.getDocumentNumber());
        FieldValidator.validateName(guardian.getFirstName(), "El primer nombre", true);
        FieldValidator.validateName(guardian.getMiddleName(), "El segundo nombre", false);
        FieldValidator.validateName(guardian.getFirstLastName(), "El primer apellido", true);
        FieldValidator.validateName(guardian.getSecondLastName(), "El segundo apellido", false);
        FieldValidator.requireOneOf(guardian.getRelationship(), RELATIONSHIPS, "El parentesco");
        FieldValidator.validatePhone(guardian.getPrimaryPhone(), "El teléfono principal", true);
        FieldValidator.validatePhone(guardian.getSecondaryPhone(), "El teléfono secundario", false);
        FieldValidator.validateEmail(guardian.getEmail());
        FieldValidator.validateMaxLength(guardian.getAddress(), "La dirección", 255);
        FieldValidator.validateMaxLength(guardian.getOccupation(), "La ocupación", 100);
    }
}