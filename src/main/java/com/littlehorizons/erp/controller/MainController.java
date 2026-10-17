package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.view.MainView;

/**
 * Runs the main menu and sends the user to each module controller.
 */
public class MainController {

    private final MainView mainView;
    private final StudentController studentController;

    public MainController(MainView mainView, StudentController studentController) {
        this.mainView = mainView;
        this.studentController = studentController;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = mainView.showMenuAndReadOption();
            switch (option) {
                case 1 -> studentController.run();
                case 2, 3, 4, 5, 6, 7 ->
                    mainView.showMessage("Este módulo aún está en construcción.");
                case 8 -> running = false;
                default -> mainView.showError("Opción inválida.");
            }
        }
    }
}