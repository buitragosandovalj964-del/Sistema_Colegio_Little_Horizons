package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.AcademicRecordController;
import com.littlehorizons.erp.controller.AssignmentController;
import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.MainController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.StudentGuardianController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.SubjectEnrollmentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.service.AcademicRecordService;
import com.littlehorizons.erp.service.AssignmentService;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.service.ClubService;
import com.littlehorizons.erp.service.EnrollmentService;
import com.littlehorizons.erp.service.GuardianService;
import com.littlehorizons.erp.service.StudentGuardianService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.SubjectEnrollmentService;
import com.littlehorizons.erp.service.SubjectService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.view.AcademicRecordView;
import com.littlehorizons.erp.view.AcademicView;
import com.littlehorizons.erp.view.AssignmentView;
import com.littlehorizons.erp.view.AttendanceView;
import com.littlehorizons.erp.view.ClubView;
import com.littlehorizons.erp.view.EnrollmentView;
import com.littlehorizons.erp.view.GuardianView;
import com.littlehorizons.erp.view.MainView;
import com.littlehorizons.erp.view.StudentGuardianView;
import com.littlehorizons.erp.view.StudentView;
import com.littlehorizons.erp.view.SubjectEnrollmentView;
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
        EnrollmentController enrollmentController = new EnrollmentController(
                new EnrollmentService(), studentService, new EnrollmentView(input));
        SubjectEnrollmentController subjectEnrollmentController =
                new SubjectEnrollmentController(new SubjectEnrollmentService(),
                        studentService, subjectService, new SubjectEnrollmentView(input));
        AcademicRecordController academicRecordController = new AcademicRecordController(
                new AcademicRecordService(), studentService, teacherService, subjectService,
                new AcademicRecordView(input));
        AttendanceController attendanceController = new AttendanceController(
                new AttendanceService(), studentService, teacherService, subjectService,
                new AttendanceView(input));
        AcademicController academicController = new AcademicController(
                new AcademicView(input), subjectController, enrollmentController,
                subjectEnrollmentController, academicRecordController, attendanceController);

        AssignmentController assignmentController = new AssignmentController(
                new AssignmentService(), teacherService, subjectService,
                new AssignmentView(input));
        TeacherController teacherController = new TeacherController(
                teacherService, new TeacherView(input), assignmentController);

        ClubController clubController = new ClubController(
                new ClubService(), studentService, teacherService, new ClubView(input));

        MainController mainController = new MainController(new MainView(input),
                studentController, academicController, teacherController, clubController);

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