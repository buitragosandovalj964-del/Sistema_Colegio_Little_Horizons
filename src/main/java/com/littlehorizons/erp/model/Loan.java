package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The loan of a resource to a student or a teacher
 * (tables: prestamos_estudiante and prestamos_profesor).
 */
public class Loan {

    private Integer id;
    private Resource resource;
    private BorrowerType borrowerType;
    private int borrowerId;
    private String borrowerName;
    private String borrowerDocument;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;
    private String notes;

    public Loan() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Resource getResource() { return resource; }
    public void setResource(Resource resource) { this.resource = resource; }

    public BorrowerType getBorrowerType() { return borrowerType; }
    public void setBorrowerType(BorrowerType borrowerType) { this.borrowerType = borrowerType; }

    public int getBorrowerId() { return borrowerId; }
    public void setBorrowerId(int borrowerId) { this.borrowerId = borrowerId; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public String getBorrowerDocument() { return borrowerDocument; }
    public void setBorrowerDocument(String borrowerDocument) {
        this.borrowerDocument = borrowerDocument;
    }

    public LocalDate getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDate loanDate) { this.loanDate = loanDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    /**
     * Days past the due date while the resource has not been returned (0 otherwise).
     */
    public long getDaysOverdue(LocalDate today) {
        if (returnDate != null || !dueDate.isBefore(today)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, today);
    }

    @Override
    public String toString() {
        return "Loan{id=" + id + ", type=" + borrowerType + ", status=" + status + "}";
    }
}