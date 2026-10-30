package test.view;

import java.util.Scanner;

public class ExportadorView {

    private final Scanner scanner;

    public ExportadorView() {
        this.scanner = new Scanner(System.in);
    }

    public void mostrarEncabezado() {
        System.out.println("=============================================");
        System.out.println("EXPORTADOR DE MATERIAS ASIGNADAS - AÑO 2025");
        System.out.println("=============================================");
    }

    public String pedirIdProfesor() {
        System.out.print("Ingrese el ID del profesor: ");
        return scanner.nextLine().trim();
    }

    public void mostrarErrorNumeroInvalido() {
        System.out.println("Error: Debe ingresar un número válido.");
    }

    public void mostrarErrorProfesorNoEncontrado(int id) {
        System.out.println("Error: No se encontró ningún profesor con el ID " + id + ".");
    }

    public void mostrarErrorSinMaterias(String nombreCompleto) {
        System.out.println("Error: El profesor " + nombreCompleto + " no tiene materias asignadas en 2025.");
    }

    public void mostrarExito(String nombreCompleto, String nombreArchivo) {
        System.out.println("Archivo generado exitosamente.");
        System.out.println("Profesor: " + nombreCompleto);
        System.out.println("Archivo: " + nombreArchivo);
    }

    public void mostrarErrorCreacionArchivo() {
        System.out.println("Error: No se pudo crear el archivo de exportación.");
    }

    public void mostrarErrorCreacionArchivo(String detalle) {
        System.out.println("Error: No se pudo crear el archivo de exportación. " + detalle);
    }

    public void mostrarErrorBaseDatos(String detalle) {
        System.out.println("Error: No se pudo consultar la base de datos. " + detalle);
    }
}
