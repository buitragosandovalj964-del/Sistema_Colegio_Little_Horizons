package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * A school event (table: eventos).
 */
public class Event {

    private Integer id;
    private String name;
    private String description;
    private String type;
    private LocalDate date;
    private String place;
    private boolean hasCompetition;
    private Teacher organizer;
    private boolean active = true;
    private int participantCount;

    public Event() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public boolean isHasCompetition() { return hasCompetition; }
    public void setHasCompetition(boolean hasCompetition) { this.hasCompetition = hasCompetition; }

    public Teacher getOrganizer() { return organizer; }
    public void setOrganizer(Teacher organizer) { this.organizer = organizer; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Not stored in the table: the service fills it with the current participants. */
    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    @Override
    public String toString() {
        return "Event{id=" + id + ", name=" + name + ", date=" + date + ", type=" + type + "}";
    }
}