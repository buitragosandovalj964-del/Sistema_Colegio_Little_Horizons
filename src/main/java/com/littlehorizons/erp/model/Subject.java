package com.littlehorizons.erp.model;

/**
 * Represents a subject taught in a grade (table: materias).
 */
public class Subject {

    private Integer id;
    private String code;
    private String name;
    private String description;
    private Grade grade;
    private int weeklyHours = 2;
    private boolean active = true;

    public Subject() {
        // Empty constructor: the DAO fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Grade getGrade() { return grade; }
    public void setGrade(Grade grade) { this.grade = grade; }

    public int getWeeklyHours() { return weeklyHours; }
    public void setWeeklyHours(int weeklyHours) { this.weeklyHours = weeklyHours; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "Subject{id=" + id + ", code=" + code + ", name=" + name
                + ", grade=" + (grade == null ? "-" : grade.getName()) + "}";
    }
}