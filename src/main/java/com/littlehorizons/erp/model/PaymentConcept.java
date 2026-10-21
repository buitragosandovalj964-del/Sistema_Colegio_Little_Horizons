package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/**
 * A type of charge the school can collect (table: conceptos_pago).
 */
public class PaymentConcept {

    private Integer id;
    private String code;
    private String name;
    private String description;
    private BigDecimal baseAmount;
    private boolean recurring;
    private boolean active;

    public PaymentConcept() { }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "PaymentConcept{id=" + id + ", code=" + code + ", name=" + name + "}";
    }
}

