package com.littlehorizons.erp.model;

import java.time.Duration;
import java.time.LocalTime;

/**
 * One class slot (day and hours) of an assignment (table: horarios_clase).
 */
public class ScheduleSlot {

    private Integer id;
    private Integer assignmentId;
    private String day;
    private LocalTime startTime;
    private LocalTime endTime;

    public ScheduleSlot() {
        // Empty constructor: the DAO fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Integer assignmentId) { this.assignmentId = assignmentId; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public long getDurationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Two slots of the same day overlap when each one starts before the other ends.
     * A class ending at 09:00 and another starting at 09:00 do not overlap.
     */
    public boolean overlaps(LocalTime otherStart, LocalTime otherEnd) {
        return startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
    }

    @Override
    public String toString() {
        return "ScheduleSlot{id=" + id + ", " + day + " " + startTime + "-" + endTime + "}";
    }
}