/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import test.dao.ProfesorDAO;
import test.model.MateriaAsignada;
import test.model.Profesor;
import test.view.ExportadorView;

/**
 * Controlador que orquesta la ejecución del examen según los pasos solicitados.
 */
public class ExportadorController {

    private final ProfesorDAO profesorDAO;
    private final ExportadorView view;

    public ExportadorController() {
        this.profesorDAO = new ProfesorDAO();
        this.view = new ExportadorView();
    }

    /**
     * Inicia el flujo principal del programa.
     */
    public void iniciar() {
        // Paso 1: Mostrar el encabezado inicial
        view.mostrarEncabezado();

        int idProfesor = 0;
        Profesor profesor = null;

        // Paso 2: Validar el ID
        while (true) {
            String texto = view.pedirIdProfesor();

            // 1. Validar si es un número entero
            try {
                idProfesor = Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                view.mostrarErrorNumeroInvalido();
                continue; // Vuelve a pedir el ID
            }

            // 2. Validar si existe un profesor con ese ID
            profesor = profesorDAO.buscarPorId(idProfesor);
            if (profesor == null) {
                view.mostrarErrorProfesorNoEncontrado(idProfesor);
                continue; // Vuelve a pedir el ID
            }

            // 3. Validar si tiene materias asignadas en el año 2025
            List<MateriaAsignada> materias = profesorDAO.listarMaterias2025(idProfesor);
            if (materias.isEmpty()) {
                view.mostrarErrorSinMaterias(profesor.getNombreCompleto());
                // No crea el archivo y el programa termina
                return;
            }

            // Si pasa todas las validaciones, procede al Paso 3 y Paso 4
            generarArchivo(profesor, materias);
            return;
        }
    }

    /**
     * Paso 3 y Paso 4: Genera el archivo .txt en la raíz del proyecto y confirma la operación.
     */
    private void generarArchivo(Profesor profesor, List<MateriaAsignada> materias) {
        String nombreArchivo = "materias_profesor_" + profesor.getIdProfesor() + "_2025.txt";
        File archivo = new File(nombreArchivo);

        // Se usa codificación UTF-8 para que se vean bien las tildes y caracteres especiales
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
            
            // Encabezado del reporte
            pw.println("=============================================");
            pw.println("COLEGIO INTERNACIONAL \"LITTLE HORIZONS\"");
            pw.println("REPORTE DE MATERIAS ASIGNADAS - AÑO 2025");
            pw.println("=============================================");
            
            // Datos del profesor
            pw.println("Nombre completo : " + profesor.getNombreCompleto());
            pw.println("Documento       : " + profesor.getDocumentoCompleto());
            pw.println();

            // Tabla de materias: Código (10), Materia (35), Grado (20)
            pw.printf("%-10s%-35s%-20s%n", "Código", "Materia", "Grado");
            for (MateriaAsignada m : materias) {
                pw.printf("%-10s%-35s%-20s%n", m.getCodigo(), m.getNombre(), m.getGrado());
            }
            pw.println();

            // Fecha del computador en formato DD/MM/AAAA
            String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            pw.println("Documento generado el: " + fechaActual);

            // Paso 4: Confirmación en consola
            view.mostrarExito(profesor.getNombreCompleto(), nombreArchivo);

        } catch (IOException e) {
            // Paso 4: Error si no se pudo crear el archivo
            view.mostrarErrorCreacionArchivo();
        }
    }
}
