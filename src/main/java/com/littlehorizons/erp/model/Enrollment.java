package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * A student's enrollment in a grade for an academic year (table: matriculas).
 */
public class Enrollment {

    private Integer id;
    private Student student;
    private Grade grade;
    private int academicYear;
    private String status;
    private LocalDate enrollmentDate;
    private LocalDate withdrawalDate;
    private String notes;

    public Enrollment() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Grade getGrade() { return grade; }
    public void setGrade(Grade grade) { this.grade = grade; }

    public int getAcademicYear() { return academicYear; }
    public void setAcademicYear(int academicYear) { this.academicYear = academicYear; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public LocalDate getWithdrawalDate() { return withdrawalDate; }
    public void setWithdrawalDate(LocalDate withdrawalDate) {
        this.withdrawalDate = withdrawalDate;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "Enrollment{id=" + id + ", year=" + academicYear
                + ", grade=" + (grade == null ? "-" : grade.getName()) + ", status=" + status + "}";
    }
}