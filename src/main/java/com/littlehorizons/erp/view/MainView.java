package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleInput;

/**
 * Main menu screen: lists the modules of the ERP.
 */
public class MainView {

    private static final int BOX_WIDTH = 46;

    private final ConsoleInput input;

    public MainView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("╔" + "═".repeat(BOX_WIDTH) + "╗");
        System.out.printf("║%-" + BOX_WIDTH + "s║%n", "   ERP Colegio Little Horizons");
        System.out.printf("║%-" + BOX_WIDTH + "s║%n", "   Sistema de Gestión Educativa");
        System.out.println("╚" + "═".repeat(BOX_WIDTH) + "╝");
        System.out.println();
        System.out.println("1. Gestión de Estudiantes");
        System.out.println("2. Gestión Académica");
        System.out.println("3. Gestión de Profesores");
        System.out.println("4. Actividades Extracurriculares");
        System.out.println("5. Gestión Financiera");
        System.out.println("6. Gestión de Recursos");
        System.out.println("7. Reportes y Estadísticas");
        System.out.println("8. Salir");
        return input.readMenuOption("Seleccione una opción: ");
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String message) {
        input.showError(message);
    }
}