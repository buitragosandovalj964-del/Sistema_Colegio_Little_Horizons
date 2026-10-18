package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleInput;

/**
 * Menu of the academic module. Everything shown to the user is in Spanish.
 */
public class AcademicView {

    private final ConsoleInput input;

    public AcademicView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("     GESTIÓN ACADÉMICA");
        System.out.println("==============================");
        System.out.println("1. Grados y materias");
        System.out.println("2. Matrículas");
        System.out.println("0. Volver al menú principal");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public void showError(String message) {
        input.showError(message);
    }
}