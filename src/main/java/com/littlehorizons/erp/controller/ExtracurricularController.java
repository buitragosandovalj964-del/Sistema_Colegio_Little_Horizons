package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.view.ExtracurricularView;

/**
 * Runs the extracurricular menu and sends the user to clubs or events.
 */
public class ExtracurricularController {

    private final ExtracurricularView extracurricularView;
    private final ClubController clubController;
    private final EventController eventController;

    public ExtracurricularController(ExtracurricularView extracurricularView,
            ClubController clubController, EventController eventController) {
        this.extracurricularView = extracurricularView;
        this.clubController = clubController;
        this.eventController = eventController;
    }

    public void run() {
        boolean running = true;
        while (running) {
            int option = extracurricularView.showMenuAndReadOption();
            switch (option) {
                case 1 -> clubController.run();
                case 2 -> eventController.run();
                case 0 -> running = false;
                default -> extracurricularView.showError("Opción inválida.");
            }
        }
    }
}