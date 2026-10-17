package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.service.FieldValidator;
import com.littlehorizons.erp.service.GuardianService;
import com.littlehorizons.erp.util.ConsoleInput;
import java.util.List;

/**
 * Console screens for guardian management: asks for data and shows results.
 * Everything shown to the user is in Spanish.
 */
public class GuardianView {

    /**
     * Identifies a guardian by document, as typed by the user.
     */
    public record DocumentId(String type, String number) { }

    private final ConsoleInput input;

    public GuardianView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("    GESTIÓN DE ACUDIENTES");
        System.out.println("==============================");
        System.out.println("1. Registrar acudiente");
        System.out.println("2. Listar acudientes");
        System.out.println("3. Buscar acudiente por documento");
        System.out.println("4. Actualizar acudiente");
        System.out.println("5. Eliminar acudiente");
        System.out.println("0. Volver");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public Guardian askNewGuardian() {
        showMessage("Escriba '" + ConsoleInput.CANCEL_WORD
                + "' en cualquier momento para volver al menú.");
        Guardian guardian = new Guardian();
        guardian.setDocumentType(input.readChoice(
                "Tipo de documento", GuardianService.DOCUMENT_TYPES));
        guardian.setDocumentNumber(input.readRequiredText(
                "Número de documento: ", FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH));
        askRemainingData(guardian);
        return guardian;
    }

    /**
     * Asks for the data of a new guardian whose document is already known.
     */
    public Guardian askNewGuardian(String documentType, String documentNumber) {
        Guardian guardian = new Guardian();
        guardian.setDocumentType(documentType);
        guardian.setDocumentNumber(documentNumber);
        askRemainingData(guardian);
        return guardian;
    }

    private void askRemainingData(Guardian guardian) {
        guardian.setFirstName(input.readRequiredName("Primer nombre: "));
        guardian.setMiddleName(input.readOptionalName("Segundo nombre"));
        guardian.setFirstLastName(input.readRequiredName("Primer apellido: "));
        guardian.setSecondLastName(input.readOptionalName("Segundo apellido"));
        guardian.setRelationship(input.readChoice("Parentesco", GuardianService.RELATIONSHIPS));
        guardian.setPrimaryPhone(input.readRequiredPhone("Teléfono principal: ",
                FieldValidator.PHONE_MIN_LENGTH, FieldValidator.PHONE_MAX_LENGTH));
        guardian.setSecondaryPhone(input.readOptionalPhone("Teléfono secundario",
                FieldValidator.PHONE_MIN_LENGTH, FieldValidator.PHONE_MAX_LENGTH));
        guardian.setEmail(input.readOptionalEmail("Correo electrónico"));
        guardian.setAddress(input.readOptionalText("Dirección"));
        guardian.setOccupation(input.readOptionalText("Ocupación"));
        guardian.setFinanciallyResponsible(
                input.readYesNo("¿Es responsable financiero de los pagos?"));
    }

    public void askGuardianChanges(Guardian guardian) {
        showMessage("Presione Enter para conservar el valor actual. Escriba '"
                + ConsoleInput.CANCEL_WORD + "' para volver al menú.");
        guardian.setFirstName(input.readNameKeepingCurrent(
                "Primer nombre", guardian.getFirstName()));
        guardian.setMiddleName(input.readNameKeepingCurrent(
                "Segundo nombre", guardian.getMiddleName()));
        guardian.setFirstLastName(input.readNameKeepingCurrent(
                "Primer apellido", guardian.getFirstLastName()));
        guardian.setSecondLastName(input.readNameKeepingCurrent(
                "Segundo apellido", guardian.getSecondLastName()));
        guardian.setRelationship(input.readChoiceKeepingCurrent(
                "Parentesco", guardian.getRelationship(), GuardianService.RELATIONSHIPS));
        guardian.setPrimaryPhone(input.readPhoneKeepingCurrent("Teléfono principal",
                guardian.getPrimaryPhone(),
                FieldValidator.PHONE_MIN_LENGTH, FieldValidator.PHONE_MAX_LENGTH));
        guardian.setSecondaryPhone(input.readPhoneKeepingCurrent("Teléfono secundario",
                guardian.getSecondaryPhone(),
                FieldValidator.PHONE_MIN_LENGTH, FieldValidator.PHONE_MAX_LENGTH));
        guardian.setEmail(input.readEmailKeepingCurrent(
                "Correo electrónico", guardian.getEmail()));
        guardian.setAddress(input.readTextKeepingCurrent("Dirección", guardian.getAddress()));
        guardian.setOccupation(input.readTextKeepingCurrent(
                "Ocupación", guardian.getOccupation()));
        guardian.setFinanciallyResponsible(input.readYesNoKeepingCurrent(
                "¿Es responsable financiero de los pagos?", guardian.isFinanciallyResponsible()));
    }

    public DocumentId askDocumentId() {
        String type = input.readChoice("Tipo de documento", GuardianService.DOCUMENT_TYPES);
        String number = input.readRequiredText(
                "Número de documento: ", FieldValidator.MAX_DOCUMENT_NUMBER_LENGTH);
        return new DocumentId(type, number);
    }

    public boolean confirm(String question) {
        return input.confirm(question);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String message) {
        input.showError(message);
    }

    public void showGuardianList(List<Guardian> guardians) {
        if (guardians.isEmpty()) {
            showMessage("No hay acudientes registrados.");
            return;
        }
        System.out.printf("%-5s %-10s %-12s %-34s %-12s %-12s%n",
                "ID", "TIPO", "DOCUMENTO", "NOMBRE", "PARENTESCO", "TELÉFONO");
        guardians.forEach(guardian -> System.out.printf("%-5d %-10s %-12s %-34s %-12s %-12s%n",
                guardian.getId(), guardian.getDocumentType(), guardian.getDocumentNumber(),
                guardian.getFullName(), guardian.getRelationship(), guardian.getPrimaryPhone()));
    }

    public void showGuardianDetail(Guardian guardian) {
        System.out.println("------------------------------");
        printField("Id", String.valueOf(guardian.getId()));
        printField("Nombre", guardian.getFullName());
        printField("Documento", guardian.getDocumentType() + " " + guardian.getDocumentNumber());
        printField("Parentesco", guardian.getRelationship());
        printField("Teléfono principal", guardian.getPrimaryPhone());
        printField("Teléfono secundario", guardian.getSecondaryPhone());
        printField("Correo electrónico", guardian.getEmail());
        printField("Dirección", guardian.getAddress());
        printField("Ocupación", guardian.getOccupation());
        printField("Responsable financiero", guardian.isFinanciallyResponsible() ? "Sí" : "No");
        System.out.println("------------------------------");
    }

    private void printField(String label, String value) {
        System.out.printf("%-24s%s%n", label + ":", value == null ? "-" : value);
    }
}