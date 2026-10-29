/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test.view;

import java.util.Scanner;

/**
 * Vista por consola para el exportador de materias asignadas.
 */
public class ExportadorView {

    private final Scanner scanner;

    public ExportadorView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Paso 1: Muestra el encabezado exactamente como en la imagen del examen.
     */
    public void mostrarEncabezado() {
        System.out.println("=============================================");
        System.out.println("EXPORTADOR DE MATERIAS ASIGNADAS - AÑO 2025");
        System.out.println("=============================================");
    }

    /**
     * Paso 1: Pide el ID del profesor por consola.
     */
    public String pedirIdProfesor() {
        System.out.print("Ingrese el ID del profesor: ");
        return scanner.nextLine().trim();
    }

    /**
     * Paso 2.1: Error si lo escrito no es un número entero.
     */
    public void mostrarErrorNumeroInvalido() {
        System.out.println("Error: Debe ingresar un número válido.");
    }

    /**
     * Paso 2.2: Error si no existe un profesor con ese ID.
     */
    public void mostrarErrorProfesorNoEncontrado(int id) {
        System.out.println("Error: No se encontró ningún profesor con el ID " + id + ".");
    }

    /**
     * Paso 2.3: Error si el profesor existe pero no tiene materias en 2025.
     */
    public void mostrarErrorSinMaterias(String nombreCompleto) {
        System.out.println("Error: El profesor " + nombreCompleto + " no tiene materias asignadas en 2025.");
    }

    /**
     * Paso 4: Confirmación de archivo generado con éxito.
     */
    public void mostrarExito(String nombreCompleto, String nombreArchivo) {
        System.out.println("Archivo generado exitosamente.");
        System.out.println("Profesor: " + nombreCompleto);
        System.out.println("Archivo: " + nombreArchivo);
    }

    /**
     * Paso 4: Error si no se pudo crear el archivo.
     */
    public void mostrarErrorCreacionArchivo() {
        System.out.println("Error: No se pudo crear el archivo de exportación.");
    }
}
