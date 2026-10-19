package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * A student's attendance in a subject on a given day (table: asistencias).
 */
public class Attendance {

    private Integer id;
    private Student student;
    private Subject subject;
    private Teacher registeredBy;
    private LocalDate date;
    private String status;
    private String notes;

    public Attendance() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Teacher getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(Teacher registeredBy) { this.registeredBy = registeredBy; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "Attendance{id=" + id + ", date=" + date + ", status=" + status + "}";
    }
}