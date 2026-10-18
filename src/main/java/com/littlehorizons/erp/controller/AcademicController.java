package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.view.AcademicView;

/**
 * Runs the academic menu and sends the user to each academic submodule.
 */
public class AcademicController {

    private final AcademicView academicView;
    private final SubjectController subjectController;
    private final EnrollmentController enrollmentController;
    private final SubjectEnrollmentController subjectEnrollmentController;
    private final AcademicRecordController academicRecordController;

    public AcademicController(AcademicView academicView, SubjectController subjectController,
            EnrollmentController enrollmentController,
            SubjectEnrollmentController subjectEnrollmentController,
            AcademicRecordController academicRecordController) {
        this.academicView = academicView;
        this.subjectController = subjectController;
        this.enrollmentController = enrollmentController;
        this.subjectEnrollmentController = subjectEnrollmentController;
        this.academicRecordController = academicRecordController;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = academicView.showMenuAndReadOption();
            switch (option) {
                case 1 -> subjectController.run();
                case 2 -> enrollmentController.run();
                case 3 -> subjectEnrollmentController.run();
                case 4 -> academicRecordController.run();
                case 0 -> running = false;
                default -> academicView.showError("Opción inválida.");
            }
        }
    }
}