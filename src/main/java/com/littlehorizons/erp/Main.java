package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.view.StudentView;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        configureConsoleEncoding();

        StudentView studentView = new StudentView();
        StudentService studentService = new StudentService();
        StudentController studentController = new StudentController(studentService, studentView);

        studentController.run();
        System.out.println("Hasta pronto.");
    }

    /**
     * Makes the console print accents and special characters correctly (UTF-8).
     */
    private static void configureConsoleEncoding() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
    }
}