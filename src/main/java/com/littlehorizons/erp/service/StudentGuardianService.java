package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardian;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Business rules for the links between students and guardians.
 * User-facing messages are in Spanish.
 */
public class StudentGuardianService {

    private final StudentGuardianDao linkDao;
    private final GuardianDao guardianDao;
    private final StudentDao studentDao;

    public StudentGuardianService() {
        this(new StudentGuardianDao(), new GuardianDao(), new StudentDao());
    }

    public StudentGuardianService(StudentGuardianDao linkDao, GuardianDao guardianDao,
            StudentDao studentDao) {
        this.linkDao = linkDao;
        this.guardianDao = guardianDao;
        this.studentDao = studentDao;
    }

    public List<StudentGuardian> findGuardiansOf(int studentId) throws SQLException {
        List<StudentGuardian> links = new ArrayList<>();
        for (StudentGuardianDao.LinkRow row : linkDao.findLinksOfStudent(studentId)) {
            Optional<Guardian> guardian = guardianDao.findById(row.guardianId());
            guardian.ifPresent(found -> links.add(
                    new StudentGuardian(found, row.primary(), row.emergencyContact())));
        }
        return links;
    }

    public List<Student> findStudentsOf(int guardianId) throws SQLException {
        List<Student> students = new ArrayList<>();
        for (int studentId : linkDao.findStudentIdsOfGuardian(guardianId)) {
            studentDao.findById(studentId).ifPresent(students::add);
        }
        return students;
    }

    public Set<Integer> findStudentIdsWithoutGuardian() throws SQLException {
        return linkDao.findStudentIdsWithoutGuardian();
    }

    public StudentGuardian getLink(Student student, Guardian guardian)
            throws ValidationException, SQLException {
        return findLink(findGuardiansOf(student.getId()), guardian).orElseThrow(
                () -> new ValidationException(guardian.getFullName()
                        + " no es acudiente de " + student.getFullName() + "."));
    }

    public void linkGuardian(Student student, Guardian guardian, boolean primary,
            boolean emergencyContact) throws ValidationException, SQLException {
        List<StudentGuardian> links = findGuardiansOf(student.getId());
        if (findLink(links, guardian).isPresent()) {
            throw new ValidationException(guardian.getFullName()
                    + " ya es acudiente de " + student.getFullName() + ".");
        }
        boolean makePrimary = primary || links.isEmpty();
        linkDao.link(student.getId(), guardian.getId(), makePrimary, emergencyContact);
    }

    public void unlinkGuardian(Student student, Guardian guardian)
            throws ValidationException, SQLException {
        List<StudentGuardian> links = findGuardiansOf(student.getId());
        StudentGuardian target = findLink(links, guardian).orElseThrow(
                () -> new ValidationException(guardian.getFullName()
                        + " no es acudiente de " + student.getFullName() + "."));
        if (links.size() == 1) {
            throw new ValidationException("Un estudiante debe tener al menos un acudiente. "
                    + "Vincule otro acudiente antes de desvincular a "
                    + guardian.getFullName() + ".");
        }
        Integer newPrimaryId = null;
        if (target.primary()) {
            newPrimaryId = links.stream()
                    .map(link -> link.guardian().getId())
                    .filter(id -> !id.equals(guardian.getId()))
                    .findFirst()
                    .orElse(null);
        }
        linkDao.unlink(student.getId(), guardian.getId(), newPrimaryId);
    }

    public void changeFlags(Student student, Guardian guardian, boolean primary,
            boolean emergencyContact) throws ValidationException, SQLException {
        StudentGuardian current = getLink(student, guardian);
        if (current.primary() && !primary) {
            throw new ValidationException("Debe haber un acudiente principal. "
                    + "Para cambiarlo, marque a otro acudiente como principal.");
        }
        linkDao.updateFlags(student.getId(), guardian.getId(), primary, emergencyContact);
    }

    private Optional<StudentGuardian> findLink(List<StudentGuardian> links, Guardian guardian) {
        return links.stream()
                .filter(link -> link.guardian().getId().equals(guardian.getId()))
                .findFirst();
    }
}