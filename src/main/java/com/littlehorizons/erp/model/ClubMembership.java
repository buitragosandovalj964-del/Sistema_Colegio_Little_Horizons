package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * A student's membership in a club (table: membresias_club).
 */
public class ClubMembership {

    private Integer id;
    private Student student;
    private Club club;
    private LocalDate joinDate;
    private String status;
    private LocalDate withdrawalDate;
    private String withdrawalReason;

    public ClubMembership() {
        // Empty constructor: the service fills the fields one by one.
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }

    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getWithdrawalDate() { return withdrawalDate; }
    public void setWithdrawalDate(LocalDate withdrawalDate) {
        this.withdrawalDate = withdrawalDate;
    }

    public String getWithdrawalReason() { return withdrawalReason; }
    public void setWithdrawalReason(String withdrawalReason) {
        this.withdrawalReason = withdrawalReason;
    }

    @Override
    public String toString() {
        return "ClubMembership{id=" + id + ", status=" + status + ", joined=" + joinDate + "}";
    }
}