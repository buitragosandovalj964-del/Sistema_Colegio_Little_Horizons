package test.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import test.model.MateriaAsignada;
import test.model.Profesor;

public class ProfesorDAO {

    private Connection obtenerConexion() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    public Profesor buscarPorId(int idProfesor) throws SQLException {
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
        }
        return null;
    }

    public List<MateriaAsignada> listarMaterias2025(int idProfesor) throws SQLException {
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
        }
        return lista;
    }
}
