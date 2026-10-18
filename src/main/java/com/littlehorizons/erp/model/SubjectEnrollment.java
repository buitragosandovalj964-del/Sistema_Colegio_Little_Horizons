package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * A student's enrollment in a subject for an academic year and period
 * (table: inscripciones_materia).
 */
public class SubjectEnrollment {

    private Integer id;
    private Student student;
    private Subject subject;
    private int academicYear;
    private String period;
    private String section;
    private LocalDate enrollmentDate;
    private String status;

    public SubjectEnrollment() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public int getAcademicYear() { return academicYear; }
    public void setAcademicYear(int academicYear) { this.academicYear = academicYear; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "SubjectEnrollment{id=" + id + ", year=" + academicYear + ", period=" + period
                + ", section=" + section + ", status=" + status + "}";
    }
}