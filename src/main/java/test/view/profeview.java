package test.view;

import com.littlehorizons.erp.util.ConsoleInput;

public class profeview {

    private final ConsoleInput input;

    public profeview(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("=============================================");
        System.out.println("EXPORTADOR DE MATERIAS ASIGNADAS - AÑO 2025");
        System.out.println("=============================================");
        return input.readIntInRange("Ingrese el ID del profesor: ", 1, Integer.MAX_VALUE);
    }
}
