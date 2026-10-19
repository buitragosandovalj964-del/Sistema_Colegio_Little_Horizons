package com.littlehorizons.erp.model;

/**
 * An extracurricular club (table: clubes).
 */
public class Club {

    private Integer id;
    private String name;
    private String description;
    private Teacher supervisor;
    private int maxCapacity = 30;
    private String meetingSchedule;
    private boolean active = true;
    private int activeMembers;

    public Club() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Teacher getSupervisor() { return supervisor; }
    public void setSupervisor(Teacher supervisor) { this.supervisor = supervisor; }

    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }

    public String getMeetingSchedule() { return meetingSchedule; }
    public void setMeetingSchedule(String meetingSchedule) {
        this.meetingSchedule = meetingSchedule;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Not stored in the table: the service fills it with the current active members. */
    public int getActiveMembers() { return activeMembers; }
    public void setActiveMembers(int activeMembers) { this.activeMembers = activeMembers; }

    @Override
    public String toString() {
        return "Club{id=" + id + ", name=" + name + ", capacity=" + maxCapacity + "}";
    }
}