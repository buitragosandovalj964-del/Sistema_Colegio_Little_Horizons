package com.littlehorizons.erp.model;

/**
 * Represents an academic grade of the school (table: grados).
 */
public class Grade {

    private Integer id;
    private String name;
    private int numericLevel;

    public Grade() {
        // Empty constructor: the DAO fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getNumericLevel() { return numericLevel; }
    public void setNumericLevel(int numericLevel) { this.numericLevel = numericLevel; }

    @Override
    public String toString() {
        return "Grade{id=" + id + ", name=" + name + ", level=" + numericLevel + "}";
    }
}