package com.littlehorizons.erp;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.ValidationException;
import com.littlehorizons.erp.util.ErrorLogger;
import java.sql.SQLException;
import java.time.LocalDate;

public class Main {

    private interface Action {
        void run() throws ValidationException, SQLException;
    }

    public static void main(String[] args) {
        StudentService service = new StudentService();

        attempt("Blank first name",
                () -> service.registerStudent(buildStudent("1000000002", "", "TI")));
        attempt("Invalid document type",
                () -> service.registerStudent(buildStudent("1000000003", "Ana", "XX")));
        attempt("Duplicate document",
                () -> service.registerStudent(buildStudent("1098765432", "Ana", "TI")));

        Student temporary = buildStudent("1000000004", "Ana", "TI");
        attempt("Valid student", () -> service.registerStudent(temporary));
        attempt("Withdraw student", () -> {
            service.withdrawStudent(temporary.getId());
            System.out.println("   status is now: "
                    + service.findById(temporary.getId()).orElseThrow().getStatus());
        });
        attempt("Delete student", () -> service.deleteStudent(temporary.getId()));
    }

    private static void attempt(String label, Action action) {
        try {
            action.run();
            System.out.println(label + " -> OK");
        } catch (ValidationException e) {
            System.out.println(label + " -> Rejected: " + e.getMessage());
        } catch (SQLException e) {
            ErrorLogger.log("Testing StudentService: " + label, e);
            System.out.println(label + " -> Database error. See error_log.txt for details.");
        }
    }

    private static Student buildStudent(String documentNumber, String firstName,
            String documentType) {
        Student student = new Student();
        student.setDocumentType(documentType);
        student.setDocumentNumber(documentNumber);
        student.setFirstName(firstName);
        student.setFirstLastName("Test");
        student.setBirthDate(LocalDate.of(2015, 1, 1));
        student.setGender("F");
        student.setAdmissionDate(LocalDate.of(2026, 1, 20));
        return student;
    }
}