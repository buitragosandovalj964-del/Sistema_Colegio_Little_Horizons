package com.littlehorizons.erp.model;

/**
 * Represents a student's guardian (table: acudientes).
 */
public class Guardian {

    private Integer id;
    private String documentType;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String firstLastName;
    private String secondLastName;
    private String relationship;
    private String primaryPhone;
    private String secondaryPhone;
    private String email;
    private String address;
    private String occupation;
    private boolean financiallyResponsible = true;

    public Guardian() {
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

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getPrimaryPhone() { return primaryPhone; }
    public void setPrimaryPhone(String primaryPhone) { this.primaryPhone = primaryPhone; }

    public String getSecondaryPhone() { return secondaryPhone; }
    public void setSecondaryPhone(String secondaryPhone) { this.secondaryPhone = secondaryPhone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public boolean isFinanciallyResponsible() { return financiallyResponsible; }
    public void setFinanciallyResponsible(boolean financiallyResponsible) {
        this.financiallyResponsible = financiallyResponsible;
    }

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
        return "Guardian{id=" + id + ", document=" + documentType + " " + documentNumber
                + ", name=" + getFullName() + ", relationship=" + relationship + "}";
    }
}