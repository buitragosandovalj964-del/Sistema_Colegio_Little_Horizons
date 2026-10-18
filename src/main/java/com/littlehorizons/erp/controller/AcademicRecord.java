package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A grade of a student in a subject for an academic year and period
 * (table: registros_academicos).
 */
public class AcademicRecord {

    private Integer id;
    private Student student;
    private Subject subject;
    private Teacher teacher;
    private int academicYear;
    private String period;
    private BigDecimal value;
    private String notes;
    private LocalDate recordDate;

    public AcademicRecord() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public int getAcademicYear() { return academicYear; }
    public void setAcademicYear(int academicYear) { this.academicYear = academicYear; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    @Override
    public String toString() {
        return "AcademicRecord{id=" + id + ", year=" + academicYear + ", period=" + period
                + ", value=" + value + "}";
    }
}