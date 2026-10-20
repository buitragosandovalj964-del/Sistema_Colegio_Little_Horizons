package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for the resources catalog. User-facing messages are in Spanish.
 */
public class ResourceService {

    public static final String AVAILABLE = "Disponible";
    public static final String LOANED = "Prestado";
    public static final String MAINTENANCE = "En Mantenimiento";
    public static final String RETIRED = "Dado de Baja";
    public static final List<String> STATUSES = List.of(AVAILABLE, LOANED, MAINTENANCE, RETIRED);
    public static final List<String> MANUAL_STATUSES = List.of(AVAILABLE, MAINTENANCE, RETIRED);

    public static final String CODE_PATTERN = "[A-Za-z0-9-]{3,50}";
    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_LOCATION_LENGTH = 100;

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final ResourceDao resourceDao;

    public ResourceService() {
        this(new ResourceDao());
    }

    public ResourceService(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    public void registerResource(Resource resource) throws ValidationException, SQLException {
        if (resource.getCode() == null || !resource.getCode().matches(CODE_PATTERN)) {
            throw new ValidationException("El código debe tener entre 3 y 50 caracteres "
                    + "(letras, números o guiones).");
        }
        resource.setCode(resource.getCode().toUpperCase());
        validate(resource);
        resource.setName(capitalizeFirst(resource.getName()));
        resource.setStatus(AVAILABLE);
        if (resourceDao.findByCode(resource.getCode()).isPresent()) {
            throw new ValidationException(
                    "Ya existe un recurso con el código " + resource.getCode() + ".");
        }
        resourceDao.insert(resource);
    }

    public void updateResource(Resource resource) throws ValidationException, SQLException {
        if (resource.getId() == null) {
            throw new ValidationException(
                    "El recurso no tiene id, por lo que no se puede actualizar.");
        }
        validate(resource);
        resource.setName(capitalizeFirst(resource.getName()));
        if (!resourceDao.update(resource)) {
            throw new ValidationException(
                    "No se encontró el recurso con id " + resource.getId() + ".");
        }
    }

    /**
     * Changes the status by hand: only among Disponible, En Mantenimiento and Dado de Baja.
     * Lent resources change through the return of the loan, and retired ones never come back.
     */
    public void changeStatus(int resourceId, String newStatus)
            throws ValidationException, SQLException {
        FieldValidator.requireOneOf(newStatus, MANUAL_STATUSES, "El nuevo estado");
        Resource resource = resourceDao.findById(resourceId).orElseThrow(
                () -> new ValidationException(
                        "No se encontró el recurso con id " + resourceId + "."));
        if (LOANED.equals(resource.getStatus())) {
            throw new ValidationException("El recurso " + resource.getCode()
                    + " está prestado; registre primero su devolución.");
        }
        if (RETIRED.equals(resource.getStatus())) {
            throw new ValidationException("El recurso " + resource.getCode()
                    + " está dado de baja y no se puede reactivar.");
        }
        if (newStatus.equals(resource.getStatus())) {
            throw new ValidationException(
                    "El recurso " + resource.getCode() + " ya está en estado " + newStatus + ".");
        }
        resourceDao.updateStatus(resourceId, newStatus);
    }

    public void deleteResource(int resourceId) throws ValidationException, SQLException {
        if (resourceDao.countLoans(resourceId) > 0) {
            throw new ValidationException("No se puede eliminar un recurso con historial de "
                    + "préstamos. Cámbielo a 'Dado de Baja'.");
        }
        if (!resourceDao.deleteById(resourceId)) {
            throw new ValidationException("No se encontró el recurso con id " + resourceId + ".");
        }
    }

    public Optional<Resource> findByCode(String code) throws SQLException {
        return resourceDao.findByCode(code.toUpperCase());
    }

    /**
     * Lists the resources, optionally filtered by category and/or status (null = no filter).
     */
    public List<Resource> listResources(ResourceCategory category, String status)
            throws SQLException {
        return resourceDao.findAll().stream()
                .filter(resource -> category == null
                        || resource.getCategory().id() == category.id())
                .filter(resource -> status == null || status.equals(resource.getStatus()))
                .toList();
    }

    public List<ResourceCategory> listCategories() throws SQLException {
        return resourceDao.findCategories();
    }

    private void validate(Resource resource) throws ValidationException {
        if (resource.getName() == null || resource.getName().isBlank()) {
            throw new ValidationException("El nombre del recurso es obligatorio.");
        }
        FieldValidator.validateMaxLength(resource.getName(), "El nombre", MAX_NAME_LENGTH);
        if (resource.getCategory() == null) {
            throw new ValidationException("La categoría es obligatoria.");
        }
        FieldValidator.validateMaxLength(resource.getDescription(), "La descripción",
                MAX_DESCRIPTION_LENGTH);
        FieldValidator.validateMaxLength(resource.getLocation(), "La ubicación",
                MAX_LOCATION_LENGTH);
        LocalDate acquisitionDate = resource.getAcquisitionDate();
        if (acquisitionDate != null && acquisitionDate.isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de adquisición no puede ser futura.");
        }
    }

    private String capitalizeFirst(String text) {
        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
    }
}