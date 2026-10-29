/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test.model;

/**
 * Clase que representa los datos de una materia asignada.
 */
public class MateriaAsignada {

    private String codigo;
    private String nombre;
    private String grado;

    public MateriaAsignada() {
    }

    public MateriaAsignada(String codigo, String nombre, String grado) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.grado = grado;
    }

    public String getCodigo() {
        return codigo != null ? codigo : "";
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre != null ? nombre : "";
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getGrado() {
        return grado != null ? grado : "";
    }

    public void setGrado(String grado) {
        this.grado = grado;
    }
}
