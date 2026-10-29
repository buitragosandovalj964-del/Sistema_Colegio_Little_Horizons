/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test;

import test.controller.ExportadorController;

/**
 * Punto de entrada para ejecutar el examen.
 * Se ejecuta directamente sin pasar por el login ni el menú principal.
 */
public class Main {

    public static void main(String[] args) {
        ExportadorController controller = new ExportadorController();
        controller.iniciar();
    }
}
