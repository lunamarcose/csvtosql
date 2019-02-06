
package dto;


public class DTOConfig {
    private String db_name;
    private String csv_location;
    private String csv_name;
    private String csv_location_old;
    private String tabla_intermedia;
    private String tabla_auxiliar;
    private String[] addresses;

    public DTOConfig(String db_name, String csv_location, String csv_name, String csv_location_old, String tabla_intermedia, String tabla_auxiliar, String[] addresses) {
        this.db_name = db_name;
        this.csv_location = csv_location;
        this.csv_name = csv_name;
        this.csv_location_old = csv_location_old;
        this.tabla_intermedia = tabla_intermedia;
        this.tabla_auxiliar = tabla_auxiliar;
        this.addresses = addresses;
    }

    public String[] getAddresses() {
        return addresses;
    }

    public void setAddresses(String[] addresses) {
        this.addresses = addresses;
    }

    public String getDb_name() {
        return db_name;
    }

    public void setDb_name(String db_name) {
        this.db_name = db_name;
    }

    public String getCsv_location() {
        return csv_location;
    }

    public void setCsv_location(String csvl_location) {
        this.csv_location = csvl_location;
    }

    public String getCsv_name() {
        return csv_name;
    }

    public void setCsv_name(String csv_name) {
        this.csv_name = csv_name;
    }

    public String getCsv_location_old() {
        return csv_location_old;
    }

    public void setCsv_location_old(String csv_location_old) {
        this.csv_location_old = csv_location_old;
    }

    public String getTabla_intermedia() {
        return tabla_intermedia;
    }

    public void setTabla_intermedia(String tabla_intermedia) {
        this.tabla_intermedia = tabla_intermedia;
    }

    public String getTabla_auxiliar() {
        return tabla_auxiliar;
    }

    public void setTabla_auxiliar(String tabla_auxiliar) {
        this.tabla_auxiliar = tabla_auxiliar;
    }
    
}
