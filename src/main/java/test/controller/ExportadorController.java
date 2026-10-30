package test.controller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import test.dao.ProfesorDAO;
import test.model.MateriaAsignada;
import test.model.Profesor;
import test.view.ExportadorView;

public class ExportadorController {

    private final ProfesorDAO profesorDAO;
    private final ExportadorView view;

    public ExportadorController() {
        this.profesorDAO = new ProfesorDAO();
        this.view = new ExportadorView();
    }

    public void iniciar() {
        view.mostrarEncabezado();

        while (true) {
            String texto = view.pedirIdProfesor();

            int idProfesor;
            try {
                idProfesor = Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                view.mostrarErrorNumeroInvalido();
                continue;
            }

            if (idProfesor <= 0) {
                view.mostrarErrorNumeroInvalido();
                continue;
            }

            try {
                Profesor profesor = profesorDAO.buscarPorId(idProfesor);
                if (profesor == null) {
                    view.mostrarErrorProfesorNoEncontrado(idProfesor);
                    continue;
                }

                List<MateriaAsignada> materias = profesorDAO.listarMaterias2025(idProfesor);
                if (materias.isEmpty()) {
                    view.mostrarErrorSinMaterias(profesor.getNombreCompleto());
                    return;
                }

                generarArchivo(profesor, materias);
                return;
            } catch (SQLException e) {
                view.mostrarErrorBaseDatos(e.getMessage());
                return;
            }
        }
    }

    private void generarArchivo(Profesor profesor, List<MateriaAsignada> materias) {
        String nombreArchivo = "materias_profesor_" + profesor.getIdProfesor() + "_2025.txt";
        Path archivo = Path.of(nombreArchivo);

        try (BufferedWriter writer = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            writer.write("=============================================");
            writer.newLine();
            writer.write("COLEGIO INTERNACIONAL \"LITTLE HORIZONS\"");
            writer.newLine();
            writer.write("REPORTE DE MATERIAS ASIGNADAS - AÑO 2025");
            writer.newLine();
            writer.write("=============================================");
            writer.newLine();
            writer.newLine();

            writer.write("Nombre completo : " + profesor.getNombreCompleto());
            writer.newLine();
            writer.write("Documento       : " + profesor.getDocumentoCompleto());
            writer.newLine();
            writer.newLine();

            writer.write(String.format("%-10.10s%-35.35s%-20.20s%n",
                    "Código", "Materia", "Grado"));
            for (MateriaAsignada materia : materias) {
                writer.write(String.format("%-10.10s%-35.35s%-20.20s%n",
                        materia.getCodigo(), materia.getNombre(), materia.getGrado()));
            }
            writer.newLine();

            String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            writer.write("Documento generado el: " + fechaActual);
            writer.newLine();
        } catch (IOException e) {
            view.mostrarErrorCreacionArchivo(e.getMessage());
            return;
        }
        view.mostrarExito(profesor.getNombreCompleto(), nombreArchivo);
    }
}
