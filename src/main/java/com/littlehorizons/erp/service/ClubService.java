package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for clubs and club memberships. User-facing messages are in Spanish.
 */
public class ClubService {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_SCHEDULE_LENGTH = 100;
    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 200;
    public static final int DEFAULT_CAPACITY = 30;

    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final String MEMBERSHIP_WITHDRAWN = "Retirada";
    private static final String STUDENT_ACTIVE = "Activo";
    private static final String TEACHER_ACTIVE = "Activo";

    private final ClubDao clubDao;
    private final ClubMembershipDao membershipDao;
    private final TeacherDao teacherDao;
    private final StudentDao studentDao;

    public ClubService() {
        this(new ClubDao(), new ClubMembershipDao(), new TeacherDao(), new StudentDao());
    }

    public ClubService(ClubDao clubDao, ClubMembershipDao membershipDao,
            TeacherDao teacherDao, StudentDao studentDao) {
        this.clubDao = clubDao;
        this.membershipDao = membershipDao;
        this.teacherDao = teacherDao;
        this.studentDao = studentDao;
    }

    // ---------- Clubs ----------

    public void registerClub(Club club) throws ValidationException, SQLException {
        validate(club);
        club.setName(capitalizeFirst(club.getName()));
        if (clubDao.findByName(club.getName()).isPresent()) {
            throw new ValidationException("Ya existe un club llamado '" + club.getName() + "'.");
        }
        clubDao.insert(club);
    }

    public void updateClub(Club club) throws ValidationException, SQLException {
        if (club.getId() == null) {
            throw new ValidationException("El club no tiene id, por lo que no se puede actualizar.");
        }
        validate(club);
        club.setName(capitalizeFirst(club.getName()));
        Optional<ClubDao.ClubRow> sameName = clubDao.findByName(club.getName());
        if (sameName.isPresent() && sameName.get().id() != club.getId()) {
            throw new ValidationException("Ya existe otro club llamado '" + club.getName() + "'.");
        }
        int members = membershipDao.countActive(club.getId());
        if (club.getMaxCapacity() < members) {
            throw new ValidationException("El cupo no puede ser menor que los " + members
                    + " miembros activos que tiene el club.");
        }
        if (!clubDao.update(club)) {
            throw new ValidationException("No se encontró el club con id " + club.getId() + ".");
        }
    }

    public void setActive(int clubId, boolean active) throws ValidationException, SQLException {
        Club club = findClubById(clubId);
        club.setActive(active);
        clubDao.update(club);
    }

    public void deleteClub(int clubId) throws ValidationException, SQLException {
        if (!clubDao.deleteById(clubId)) {
            throw new ValidationException("No se encontró el club con id " + clubId + ".");
        }
    }

    public Optional<Club> findByName(String name) throws SQLException {
        Optional<ClubDao.ClubRow> row = clubDao.findByName(name.trim());
        return row.isEmpty() ? Optional.empty() : Optional.of(toClub(row.get()));
    }

    public List<Club> listClubs() throws SQLException {
        List<Club> clubs = new ArrayList<>();
        for (ClubDao.ClubRow row : clubDao.findAll()) {
            clubs.add(toClub(row));
        }
        return clubs;
    }

    // ---------- Memberships ----------

    public void enrollStudent(Club club, Student student, LocalDate joinDate)
            throws ValidationException, SQLException {
        Club current = findClubById(club.getId());
        if (!current.isActive()) {
            throw new ValidationException("El club '" + current.getName() + "' está inactivo.");
        }
        if (!STUDENT_ACTIVE.equals(student.getStatus())) {
            throw new ValidationException("El estudiante " + student.getFullName()
                    + " está en estado " + student.getStatus()
                    + "; solo se inscribe a estudiantes activos.");
        }
        if (joinDate == null) {
            throw new ValidationException("La fecha de ingreso es obligatoria.");
        }
        if (joinDate.isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de ingreso no puede ser futura.");
        }
        if (membershipDao.findActive(student.getId(), current.getId()).isPresent()) {
            throw new ValidationException(student.getFullName()
                    + " ya es miembro activo del club '" + current.getName() + "'.");
        }
        if (current.getActiveMembers() >= current.getMaxCapacity()) {
            throw new ValidationException("El club '" + current.getName() + "' está lleno (cupo "
                    + "máximo de " + current.getMaxCapacity() + " estudiantes).");
        }
        membershipDao.insert(student.getId(), current.getId(), joinDate);
    }

    public void withdrawStudent(Club club, Student student, String reason)
            throws ValidationException, SQLException {
        FieldValidator.validateMaxLength(reason, "El motivo", MAX_DESCRIPTION_LENGTH);
        ClubMembershipDao.MembershipRow membership = membershipDao
                .findActive(student.getId(), club.getId())
                .orElseThrow(() -> new ValidationException(student.getFullName()
                        + " no es miembro activo del club '" + club.getName() + "'."));
        membershipDao.withdraw(membership.id(), LocalDate.now(), reason);
    }

    public List<ClubMembership> findMembers(Club club) throws SQLException {
        List<ClubMembership> memberships = new ArrayList<>();
        for (ClubMembershipDao.MembershipRow row : membershipDao.findByClub(club.getId())) {
            memberships.add(toMembership(row, club, null));
        }
        return memberships;
    }

    public List<ClubMembership> findMembershipsOf(Student student) throws SQLException {
        List<ClubMembership> memberships = new ArrayList<>();
        for (ClubMembershipDao.MembershipRow row : membershipDao.findByStudent(student.getId())) {
            Optional<ClubDao.ClubRow> clubRow = clubDao.findById(row.clubId());
            Club club = clubRow.isPresent() ? toClub(clubRow.get()) : null;
            memberships.add(toMembership(row, club, student));
        }
        return memberships;
    }

    // ---------- Helpers ----------

    private Club findClubById(int clubId) throws ValidationException, SQLException {
        ClubDao.ClubRow row = clubDao.findById(clubId).orElseThrow(
                () -> new ValidationException("No se encontró el club con id " + clubId + "."));
        return toClub(row);
    }

    private Club toClub(ClubDao.ClubRow row) throws SQLException {
        Club club = new Club();
        club.setId(row.id());
        club.setName(row.name());
        club.setDescription(row.description());
        club.setMaxCapacity(row.maxCapacity());
        club.setMeetingSchedule(row.meetingSchedule());
        club.setActive(row.active());
        club.setActiveMembers(membershipDao.countActive(row.id()));
        if (row.supervisorId() != null) {
            teacherDao.findById(row.supervisorId()).ifPresent(club::setSupervisor);
        }
        return club;
    }

    private ClubMembership toMembership(ClubMembershipDao.MembershipRow row, Club club,
            Student knownStudent) throws SQLException {
        ClubMembership membership = new ClubMembership();
        membership.setId(row.id());
        membership.setClub(club);
        membership.setJoinDate(row.joinDate());
        membership.setStatus(row.status());
        membership.setWithdrawalDate(row.withdrawalDate());
        membership.setWithdrawalReason(row.withdrawalReason());
        membership.setStudent(knownStudent != null ? knownStudent
                : studentDao.findById(row.studentId()).orElse(null));
        return membership;
    }

    private void validate(Club club) throws ValidationException {
        if (club.getName() == null || club.getName().isBlank()) {
            throw new ValidationException("El nombre del club es obligatorio.");
        }
        FieldValidator.validateMaxLength(club.getName(), "El nombre del club", MAX_NAME_LENGTH);
        FieldValidator.validateMaxLength(club.getDescription(), "La descripción",
                MAX_DESCRIPTION_LENGTH);
        FieldValidator.validateMaxLength(club.getMeetingSchedule(), "El horario de reuniones",
                MAX_SCHEDULE_LENGTH);
        if (club.getMaxCapacity() < MIN_CAPACITY || club.getMaxCapacity() > MAX_CAPACITY) {
            throw new ValidationException("El cupo máximo debe estar entre " + MIN_CAPACITY
                    + " y " + MAX_CAPACITY + ".");
        }
        Teacher supervisor = club.getSupervisor();
        if (supervisor != null && !TEACHER_ACTIVE.equals(supervisor.getStatus())) {
            throw new ValidationException("El profesor " + supervisor.getFullName()
                    + " está en estado " + supervisor.getStatus()
                    + "; el encargado del club debe estar activo.");
        }
    }

    private String capitalizeFirst(String text) {
        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
    }
}