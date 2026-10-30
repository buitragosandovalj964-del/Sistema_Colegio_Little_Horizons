package test.model;

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
