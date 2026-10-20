package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.view.MainView;

/**
 * Runs the main menu and sends the user to each module controller.
 */
public class MainController {

    private final MainView mainView;
    private final StudentController studentController;
    private final AcademicController academicController;
    private final TeacherController teacherController;
    private final ExtracurricularController extracurricularController;
    private final ResourceController resourceController;

    public MainController(MainView mainView, StudentController studentController,
            AcademicController academicController, TeacherController teacherController,
            ExtracurricularController extracurricularController,
            ResourceController resourceController) {
        this.mainView = mainView;
        this.studentController = studentController;
        this.academicController = academicController;
        this.teacherController = teacherController;
        this.extracurricularController = extracurricularController;
        this.resourceController = resourceController;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = mainView.showMenuAndReadOption();
            switch (option) {
                case 1 -> studentController.run();
                case 2 -> academicController.run();
                case 3 -> teacherController.run();
                case 4 -> extracurricularController.run();
                case 6 -> resourceController.run();
                case 5, 7 ->
                    mainView.showMessage("Este módulo aún está en construcción.");
                case 8 -> running = false;
                default -> mainView.showError("Opción inválida.");
            }
        }
    }
}