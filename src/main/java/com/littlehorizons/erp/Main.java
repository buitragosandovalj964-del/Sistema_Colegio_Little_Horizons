package com.littlehorizons.erp;

import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ErrorLogger;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class Main {

    public static void main(String[] args) {
        StudentDao studentDao = new StudentDao();
        try {
            Optional<Student> existing = studentDao.findByDocument("TI", "1098765432");
            if (existing.isEmpty()) {
                Student student = new Student();
                student.setDocumentType("TI");
                student.setDocumentNumber("1098765432");
                student.setFirstName("Juan");
                student.setMiddleName("Camilo");
                student.setFirstLastName("Perez");
                student.setSecondLastName("Gomez");
                student.setBirthDate(LocalDate.of(2014, 5, 12));
                student.setGender("M");
                student.setAdmissionDate(LocalDate.of(2026, 1, 20));
                student.setStatus("Activo");
                studentDao.insert(student);
                System.out.println("Inserted student with id " + student.getId());
            } else {
                System.out.println("Student already exists, skipping insert.");
            }

            studentDao.findByDocument("TI", "1098765432")
                    .ifPresentOrElse(
                            found -> System.out.println("Found: " + found),
                            () -> System.out.println("Student not found"));
        } catch (SQLException e) {
            ErrorLogger.log("Testing StudentDao", e);
            System.out.println("Test failed. See error_log.txt for details.");
        }
    }
}