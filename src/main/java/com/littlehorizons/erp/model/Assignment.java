package com.littlehorizons.erp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A teacher assigned to a subject for an academic year and a section
 * (table: asignaciones_profesor), together with its class slots.
 */
public class Assignment {

    private Integer id;
    private Teacher teacher;
    private Subject subject;
    private int academicYear;
    private String section;
    private List<ScheduleSlot> slots = new ArrayList<>();

    public Assignment() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public int getAcademicYear() { return academicYear; }
    public void setAcademicYear(int academicYear) { this.academicYear = academicYear; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public List<ScheduleSlot> getSlots() { return slots; }
    public void setSlots(List<ScheduleSlot> slots) { this.slots = slots; }

    public long getScheduledMinutes() {
        return slots.stream().mapToLong(ScheduleSlot::getDurationMinutes).sum();
    }

    @Override
    public String toString() {
        return "Assignment{id=" + id + ", subject=" + (subject == null ? "-" : subject.getCode())
                + ", year=" + academicYear + ", section=" + section + "}";
    }
}