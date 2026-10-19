package com.littlehorizons.erp.model;

/**
 * A student's participation in an event (table: participantes_evento).
 */
public class EventParticipation {

    private Integer id;
    private Event event;
    private Student student;
    private String role;
    private String position;
    private String notes;

    public EventParticipation() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "EventParticipation{id=" + id + ", role=" + role + ", position=" + position + "}";
    }
}