package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleInput;

/**
 * Menu of the extracurricular module. Everything shown to the user is in Spanish.
 */
public class ExtracurricularView {

    private final ConsoleInput input;

    public ExtracurricularView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println(" ACTIVIDADES EXTRACURRICULARES");
        System.out.println("==============================");
        System.out.println("1. Clubes");
        System.out.println("2. Eventos");
        System.out.println("0. Volver al menú principal");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public void showError(String message) {
        input.showError(message);
    }
}