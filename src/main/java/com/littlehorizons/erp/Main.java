package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AssignmentController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.MainController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.StudentGuardianController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.service.AssignmentService;
import com.littlehorizons.erp.service.GuardianService;
import com.littlehorizons.erp.service.StudentGuardianService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.view.AssignmentView;
import com.littlehorizons.erp.view.GuardianView;
import com.littlehorizons.erp.view.MainView;
import com.littlehorizons.erp.view.StudentGuardianView;
import com.littlehorizons.erp.view.StudentView;
import com.littlehorizons.erp.view.SubjectView;
import com.littlehorizons.erp.view.TeacherView;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        configureConsoleEncoding();

        ConsoleInput input = new ConsoleInput();

        StudentService studentService = new StudentService();
        GuardianService guardianService = new GuardianService();
        TeacherService teacherService = new TeacherService();
        SubjectService subjectService = new SubjectService();
        GuardianView guardianView = new GuardianView(input);

        GuardianController guardianController =
                new GuardianController(guardianService, guardianView);
        StudentGuardianController studentGuardianController = new StudentGuardianController(
                new StudentGuardianService(), studentService, guardianService,
                new StudentGuardianView(input), guardianView);
        StudentController studentController = new StudentController(
                studentService, new StudentView(input), guardianController,
                studentGuardianController);
        SubjectController subjectController =
                new SubjectController(subjectService, new SubjectView(input));
        AssignmentController assignmentController = new AssignmentController(
                new AssignmentService(), teacherService, subjectService,
                new AssignmentView(input));
        TeacherController teacherController = new TeacherController(
                teacherService, new TeacherView(input), assignmentController);
        MainController mainController = new MainController(
                new MainView(input), studentController, subjectController, teacherController);

        mainController.run();
        System.out.println("Hasta pronto.");
    }

    /**
     * Makes the console print accents and special characters correctly (UTF-8).
     */
    private static void configureConsoleEncoding() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
    }
}