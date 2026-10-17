package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * Represents a teacher of the school (table: profesores).
 */
public class Teacher {

    private Integer id;
    private String documentType;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String firstLastName;
    private String secondLastName;
    private String email;
    private String phone;
    private String specialization;
    private LocalDate hireDate;
    private String status;

    public Teacher() {
        // Empty constructor: the DAO fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getFirstLastName() { return firstLastName; }
    public void setFirstLastName(String firstLastName) { this.firstLastName = firstLastName; }

    public String getSecondLastName() { return secondLastName; }
    public void setSecondLastName(String secondLastName) { this.secondLastName = secondLastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFullName() {
        StringBuilder fullName = new StringBuilder(firstName);
        if (middleName != null && !middleName.isBlank()) {
            fullName.append(' ').append(middleName);
        }
        fullName.append(' ').append(firstLastName);
        if (secondLastName != null && !secondLastName.isBlank()) {
            fullName.append(' ').append(secondLastName);
        }
        return fullName.toString();
    }

    @Override
    public String toString() {
        return "Teacher{id=" + id + ", document=" + documentType + " " + documentNumber
                + ", name=" + getFullName() + ", status=" + status + "}";
    }
}