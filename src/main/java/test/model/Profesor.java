/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test.model;

/**
 * Clase que representa a un profesor para el reporte de exportación.
 */
public class Profesor {

    private int idProfesor;
    private String tipoDocumento;
    private String numeroDocumento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;

    public Profesor() {
    }

    public Profesor(int idProfesor, String tipoDocumento, String numeroDocumento,
            String primerNombre, String segundoNombre, String primerApellido, String segundoApellido) {
        this.idProfesor = idProfesor;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.primerNombre = primerNombre;
        this.segundoNombre = segundoNombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
    }

    /**
     * Construye el nombre completo.
     * Si no tiene segundo nombre o segundo apellido, se omite sin dejar dos espacios seguidos.
     */
    public String getNombreCompleto() {
        String nombre = (primerNombre != null) ? primerNombre.trim() : "";

        if (segundoNombre != null && !segundoNombre.trim().isEmpty()) {
            nombre += " " + segundoNombre.trim();
        }
        if (primerApellido != null && !primerApellido.trim().isEmpty()) {
            nombre += " " + primerApellido.trim();
        }
        if (segundoApellido != null && !segundoApellido.trim().isEmpty()) {
            nombre += " " + segundoApellido.trim();
        }
        return nombre;
    }

    /**
     * Retorna el tipo de documento, un espacio y el número (ej: CC 80123456).
     */
    public String getDocumentoCompleto() {
        String tipo = (tipoDocumento != null) ? tipoDocumento.trim() : "";
        String num = (numeroDocumento != null) ? numeroDocumento.trim() : "";
        return tipo + " " + num;
    }

    public int getIdProfesor() {
        return idProfesor;
    }

    public void setIdProfesor(int idProfesor) {
        this.idProfesor = idProfesor;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }
}
