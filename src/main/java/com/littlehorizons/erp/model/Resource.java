package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * A resource that can be lent, such as a book or lab equipment (table: recursos).
 */
public class Resource {

    private Integer id;
    private String code;
    private String name;
    private ResourceCategory category;
    private String description;
    private LocalDate acquisitionDate;
    private String location;
    private String status = "Disponible";

    public Resource() {
        // Empty constructor: the DAO fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ResourceCategory getCategory() { return category; }
    public void setCategory(ResourceCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Resource{id=" + id + ", code=" + code + ", name=" + name + ", status=" + status + "}";
    }
}