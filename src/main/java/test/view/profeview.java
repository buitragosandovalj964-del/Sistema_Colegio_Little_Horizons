/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test.view;

import com.littlehorizons.erp.util.ConsoleInput;

/**
 *
 * @author camper
 */
public class profeview {
    public record DocumentId(String type, String number) { }

    private final ConsoleInput input;

    public profeview(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("    EXPORTADOR DE MATERIAS ASIGNADAS - AÑO 2025   ");
        System.out.println("==============================");
        ConsoleInput("ingrese el id del profesor ");
}
