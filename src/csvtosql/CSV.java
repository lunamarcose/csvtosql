package csvtosql;

public class CSV {
    private String fullPath;
    private boolean isValid;
    private String isValid_motive;
    private int cantidadRegistros;
    private double porcentaje;
    
    public CSV(String fullPath){
        this.fullPath = fullPath;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public boolean isIsValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public int getCantidadRegistros() {
        return cantidadRegistros;
    }

    public void setCantidadRegistros(int cantidadRegistros) {
        this.cantidadRegistros = cantidadRegistros;
    }

    public double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public String getIsValid_motive() {
        return isValid_motive;
    }

    public void setIsValid_motive(String isValid_motive) {
        this.isValid_motive = isValid_motive;
    }
    
}
