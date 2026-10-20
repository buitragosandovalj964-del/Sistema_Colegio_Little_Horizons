package com.littlehorizons.erp.model;

/**
 * Who borrows a resource. Each type has its own loans table.
 */
public enum BorrowerType {

    STUDENT("Estudiante", "prestamos_estudiante", "id_estudiante"),
    TEACHER("Profesor", "prestamos_profesor", "id_profesor");

    private final String label;
    private final String table;
    private final String borrowerColumn;

    BorrowerType(String label, String table, String borrowerColumn) {
        this.label = label;
        this.table = table;
        this.borrowerColumn = borrowerColumn;
    }

    public String label() { return label; }

    /** Name of the loans table (a fixed constant, never typed by the user). */
    public String table() { return table; }

    /** Name of the column that references the borrower in that table. */
    public String borrowerColumn() { return borrowerColumn; }
}