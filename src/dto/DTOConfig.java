package dto;

public class DTOConfig {
    private String db_name;
    private String csv_location;
    private String csv_location_old;
    private String log_location;
    private String tabla_intermedia;
    private String tabla_auxiliar;
    private String columns;
    private String columns_required;
    private String encoding;
    private String[] addresses;
    private boolean include_columns;
    private char separator_char;
    private char quotes_char;
    private boolean notification_flag;
    private String notification_subject;
    private boolean isValid;
    private String isValidMotive;
    private boolean pre_verification;
    private double tolerance_percentage;
    
    // Carga de valores de configuración
    public void cargarValores(String db_name, String csv_location, String csv_location_old, 
            String log_location, String tabla_intermedia, String tabla_auxiliar, 
            String columns, String columns_required, String encoding, String addresses, 
            String include_columns, String separator_char, String quotes_char, 
            String notification_flag, String notification_subject, String pre_verification, String tolerance_percentage) {
        this.db_name = db_name;
        this.csv_location = csv_location;
        this.log_location = log_location;
        this.csv_location_old = csv_location_old;
        this.tabla_intermedia = tabla_intermedia;
        this.tabla_auxiliar = tabla_auxiliar;
        this.columns = columns;
        this.columns_required = columns_required;
        this.encoding = encoding;
        this.addresses = addresses.split(",");
        this.include_columns = Boolean.parseBoolean(include_columns);
        this.separator_char = separator_char.charAt(0);
        this.quotes_char = quotes_char.charAt(0);
        this.notification_flag = Boolean.parseBoolean(notification_flag);
        this.notification_subject = notification_subject;
        this.pre_verification = Boolean.parseBoolean(pre_verification);
        this.tolerance_percentage = Double.parseDouble(tolerance_percentage);
    }
    
    // Se inicializa como valido
    public DTOConfig(){
        this.isValid = true;
    }

    public double getTolerance_percentage() {
        return tolerance_percentage;
    }

    public void setTolerance_percentage(double tolerance_percentage) {
        this.tolerance_percentage = tolerance_percentage;
    }

    public boolean isPre_verification() {
        return pre_verification;
    }

    public void setPre_verification(String pre_verification) {
        this.pre_verification = Boolean.parseBoolean(pre_verification);
    }
    
    public boolean isIsValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public String getIsValidMotive() {
        return isValidMotive;
    }

    public void setIsValidMotive(String isValidMotive) {
        this.isValidMotive = isValidMotive;
    }
    
    public boolean isNotification_flag() {
        return notification_flag;
    }

    public void setNotification_flag(boolean notification_flag) {
        this.notification_flag = notification_flag;
    }

    public String getNotification_subject() {
        return notification_subject;
    }

    public void setNotification_subject(String notification_subject) {
        this.notification_subject = notification_subject;
    }

    public char getSeparator_char() {
        return separator_char;
    }

    public void setSeparator_char(char separator_char) {
        this.separator_char = separator_char;
    }

    public char getQuotes_char() {
        return quotes_char;
    }

    public void setQuotes_char(char quotes_char) {
        this.quotes_char = quotes_char;
    }

    public boolean isInclude_columns() {
        return include_columns;
    }

    public void setInclude_columns(boolean include_columns) {
        this.include_columns = include_columns;
    }
    
    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public String getLog_location() {
        return log_location;
    }

    public void setLog_location(String log_location) {
        this.log_location = log_location;
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

    public String getColumns_required() {
        return columns_required;
    }

    public void setColumns_required(String columns_required) {
        this.columns_required = columns_required;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
      
}
