/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import test.model.MateriaAsignada;
import test.model.Profesor;

/**
 * Acceso a datos para profesores y asignaciones de materias.
 */
public class ProfesorDAO {

    /**
     * Reutiliza la conexión del proyecto mediante DatabaseConnection.
     * Si no encuentra el archivo db.properties, usa la conexión directa por defecto.
     */
    private Connection obtenerConexion() throws SQLException {
        try {
            return DatabaseConnection.getConnection();
        } catch (Exception e) {
            String url = "jdbc:mysql://localhost:3306/erp_little_horizons?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            return DriverManager.getConnection(url, "root", "");
        }
    }

    /**
     * Busca un profesor por su ID.
     * 
     * @param idProfesor ID del profesor a buscar.
     * @return El profesor encontrado, o null si no existe.
     */
    public Profesor buscarPorId(int idProfesor) {
        String sql = "SELECT id_profesor, tipo_documento, numero_documento, "
                + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido "
                + "FROM profesores WHERE id_profesor = ?";

        try (Connection con = obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProfesor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Profesor(
                            rs.getInt("id_profesor"),
                            rs.getString("tipo_documento"),
                            rs.getString("numero_documento"),
                            rs.getString("primer_nombre"),
                            rs.getString("segundo_nombre"),
                            rs.getString("primer_apellido"),
                            rs.getString("segundo_apellido")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar profesor: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene la lista de materias que tiene asignadas el profesor en el año 2025.
     * 
     * @param idProfesor ID del profesor.
     * @return Lista de materias asignadas en 2025.
     */
    public List<MateriaAsignada> listarMaterias2025(int idProfesor) {
        List<MateriaAsignada> lista = new ArrayList<>();
        String sql = "SELECT m.codigo_materia, m.nombre_materia, g.nombre_grado "
                + "FROM asignaciones_profesor ap "
                + "JOIN materias m ON ap.id_materia = m.id_materia "
                + "JOIN grados g ON m.id_grado = g.id_grado "
                + "WHERE ap.id_profesor = ? AND ap.año_academico = 2025 "
                + "ORDER BY ap.id_asignacion ASC";

        try (Connection con = obtenerConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idProfesor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MateriaAsignada materia = new MateriaAsignada(
                            rs.getString("codigo_materia"),
                            rs.getString("nombre_materia"),
                            rs.getString("nombre_grado")
                    );
                    lista.add(materia);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al listar materias: " + e.getMessage());
        }
        return lista;
    }
}
