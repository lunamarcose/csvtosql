package csvtosql;

import db.IndireccionPersistencia;
import db.PostgresHelper;
import dto.DTOConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import notification.MailSender;


public class Controller {
    
    private PostgresHelper cliente;
    private Expert experto = new Expert();
    private CSV csv;
    private DTOConfig dtoConfig;
    private MailSender sender;
    private InternetAddress addresses;
    private String email_subjet;
    private ArrayList<String> email_content = new ArrayList();
    private final static Logger LOGGER = Logger.getLogger("csvtosql.App");

    public String getEmail_subjet() {
        return email_subjet;
    }

    public void setEmail_subjet(String email_subjet) {
        this.email_subjet = "Proceso de Importación de datos - " + email_subjet;
    }

    public String getEmail_content() {
        String contenido = "";
        for (int i = 0; i < this.email_content.size(); i++) {
            contenido += this.email_content.get(i);
        }
        return contenido;
    }

    public void setEmail_content(String email_content) {
        this.email_content.add(email_content);
    }
    
    /**
    * Controla los pasos a seguir durante el procedimiento  de carga
    */
    public void iniciarProceso() throws IOException, SQLException, InterruptedException, MessagingException{
        ArrayList<String> mensajes = new ArrayList();
        startLogger();
        // Verificamos el archivo de configuraciones
        if(verificarEstadoConfig()){ // El archivo de configuración posee todos los datos cargados
            // Verificamos exista el csv a procesar
            // Si existe, devuelve el objeto
            this.csv = verificarExistenciaCSV();
            if(csv != null){ // Existe el csv a procesar
                // Verificamos conexión con la DB
                if(conectar()){ // Conectado OK
                    // Verificamos el contenido del .csv
                    if(verificarCSV(cliente,csv)){ // CSV correcto
                        // Se procede a cargar el .csv
                        if(cargarCSV(csv)){
                            // Se mueve el csv al directorio de históricos
                            moverCSV(csv);
                        }
                    }
                }
            }
        }
        notificar();
    }
    
    /**
    * Genera logs por consola y en un archivo .log en el directorio especificado
    */
    public void startLogger(){
        try {
            Handler consoleHandler = new ConsoleHandler();
            Date fecha_actual= new Date();
            String fecha_actual_str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(fecha_actual);
            String log_location = experto.properties.getPropValue("log_location");
            Handler fileHandler = new FileHandler(log_location + "/csvtosql_" + fecha_actual_str + ".log", false);
            SimpleFormatter simpleFormatter = new SimpleFormatter();
            fileHandler.setFormatter(simpleFormatter);
            LOGGER.addHandler(consoleHandler);
            LOGGER.addHandler(fileHandler);
            consoleHandler.setLevel(Level.ALL);
            fileHandler.setLevel(Level.ALL);
            LOGGER.log(Level.INFO, "Inicio del proceso de carga");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error de IO");
        } catch (SecurityException ex) {
            LOGGER.log(Level.SEVERE, "Error de Seguridad");
        }
    }
    
    /**
    * Conectarse a la DB
    * @return boolean
    */
    public boolean conectar(){
        LOGGER.log(Level.INFO, "Intentando conectar a la base de datos...");
        try{
            this.cliente = IndireccionPersistencia.conectar();
            if(this.cliente != null){
                LOGGER.log(Level.INFO, "Conectado a la DB");
                return true;
            } else {
                LOGGER.log(Level.WARNING, "No se ha podido conectar a la DB");
                setEmail_subjet("Error");
                setEmail_content("No se ha podido conectar a la DB");
                return false;
            }
        } catch (IOException | SQLException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha podido conectar a la DB");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            setEmail_subjet("Error");
            setEmail_content("No se ha podido conectar a la DB: " + Controller.getStackTrace(e));
            return false;
        }
    }
    
    /**
    * Desconectarse de la DB
    * @return boolean
    */
    public boolean desconectar(){
        if(this.cliente != null){
            try{
                this.cliente.disconnect();
                LOGGER.log(Level.INFO, "Desconectado de la DB");
                return true;
            } catch (SQLException e){
                LOGGER.log(Level.SEVERE, "ERROR: No se ha podido conectar a la DB");
                LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
                setEmail_subjet("Error");
                setEmail_content("No se ha podido desconectar de la DB: " + Controller.getStackTrace(e));
                return false;
            }
        }
        LOGGER.log(Level.WARNING, "No se ha podido desconectar de la DB");
        setEmail_subjet("Error");
        setEmail_content("No se ha podido desconectar de la DB: ");
        return false;
    }

    /**
    * Ejecuta la carga mediante el comando copy del .csv a la base de datos y tablas especificadas
    * @param csv CSV
    * @return boolean
    */
    public boolean cargarCSV(CSV csv){
        LOGGER.log(Level.INFO, "Se esta cargando el .csv a la DB");
        if(this.cliente != null){
            try{
                if(this.experto.cargarCSV(this.cliente, csv,dtoConfig)){
                    LOGGER.log(Level.INFO, "Se ha cargado correctamente el .csv");
                    setEmail_subjet("Operación completada");
                    setEmail_content("Se han cargado correctamente los " + csv.getCantidadRegistros() + " registros del archivo .csv. ");
                    desconectar();
                    return true;
                } else {
                    LOGGER.log(Level.SEVERE, "No se ha cargado el .csv en su totalidad, o existen datos previos sin procesar. Favor contacte soporte.");
                    setEmail_subjet("Error");
                    setEmail_content("No se ha cargado el .csv en su totalidad, o existen datos previos sin procesar. Favor contacte soporte.");
                    desconectar();
                    return false;
                }
            } catch (IOException | InterruptedException | SQLException e){
                LOGGER.log(Level.SEVERE, "ERROR: No se ha podido cargar el .csv");
                LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
                setEmail_subjet("Error");
                setEmail_content("ERROR: No se ha podido cargar el .csv " + Controller.getStackTrace(e));
                return false;
            }
        }
        //System.out.println("No es posible conectarse a la DB");
        return false;
    }
    
    /**
    * Se encarga de mover y renombrar el .csv al directorio correspondiente al finalizar la carga
    * @param csv CSV
    * @return boolean
    */
    public boolean moverCSV(CSV csv) throws MessagingException{
        LOGGER.log(Level.INFO, "Se procede a mover el .csv al directorio " + dtoConfig.getCsv_location_old());
        try{
            boolean moveCompleted = experto.moverCSV(csv, dtoConfig);
            if (moveCompleted){
                LOGGER.log(Level.INFO, "Se ha movido el .csv al contenedor " + dtoConfig.getCsv_location_old());
                return true;
            } else {
                LOGGER.log(Level.WARNING, "No se ha podido mover el .csv");
                setEmail_subjet("Operación parcialmente  completada");
                setEmail_content(" Se informa que no se ha podido mover el .csv. Favor contacte soporte para regularizar la situación.");
                return false;
            }
        } catch (IOException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha podido mover el .csv");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            setEmail_subjet("Operación parcialmente  completada");
            setEmail_content(" Se informa que no se ha podido mover el .csv. Favor contacte soporte para regularizar la situación. " + Controller.getStackTrace(e));
            return false;
        }
    }

    /**
    * Esta funcion verifica que el contenido del .csv cumpla con el formato establecido para su carga
    * @param cliente PostgresHelper, csv CSV
    * @return boolean
    */
    public boolean verificarCSV(PostgresHelper cliente, CSV csv){
        LOGGER.log(Level.INFO, "Verificando el contenido del .csv a procesar...");
        boolean estadoCSV = false;
        try{
            estadoCSV = experto.verificarCSV(cliente, csv,dtoConfig).isIsValid();
            if(estadoCSV){
                LOGGER.log(Level.INFO, "El csv tiene el formato adecuado");
            } else {
                LOGGER.log(Level.WARNING, "El csv no cumple con el formato establecido: " + csv.getIsValid_motive());
                setEmail_subjet("Error");
                setEmail_content(" El csv no cumple con el formato establacido: " + csv.getIsValid_motive());
            }
        } catch(IOException | SQLException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha podido conectar a la DB");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            setEmail_subjet("Error");
            setEmail_content(" No se ha podido conectar a la DB" + Controller.getStackTrace(e));
            return estadoCSV;
        }
        return estadoCSV;
    }
    
    /**
    * Se comprueba que exista un .csv a procesar, de ser así se crea un objeto del tipo
    * CSV, donde se cargan los parametros principales del mismo utilizados en el proceso de carga
    * @return CSV objeto con los datos principales utilizados en el procesamiento del mismo
    */
    public CSV verificarExistenciaCSV(){
        LOGGER.log(Level.INFO, "Verificando existencia de archivo .csv a procesar...");
        CSV csv = null;
        csv = experto.verificarExistenciaCSV(dtoConfig);
        if(csv!=null){
            LOGGER.log(Level.INFO, "Se ha detectado un .csv a procesar en la ruta: " + dtoConfig.getCsv_location() );
        } else {
            LOGGER.log(Level.WARNING, "No se pudo procesar el .csv. Verifique que exista y no se encuentren varios .csv en la ruta: " + dtoConfig.getCsv_location());
            setEmail_subjet("Error");
            setEmail_content(" No se pudo procesar el .csv. Verifique que exista y no se encuentren varios .csv en la ruta: : " + dtoConfig.getCsv_location());
        }
        return csv;
    }
    
    /**
    * Verifica y carga las configuraciones almacenadas en el .properties.
    * @return boolean
    */
    public boolean verificarEstadoConfig(){
        LOGGER.log(Level.INFO, "Verificando el archivo de configuraciones...");
        try{
            DTOConfig dto = experto.verificarEstadoConfig();
            if(dto != null){
                this.dtoConfig = dto;
                LOGGER.log(Level.INFO, "El archivo de configuración fue validado exitosamente");
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Debe completar todos los valores del archivo de configuración");
                setEmail_subjet("Error");
                setEmail_content(" Debe completar todos los valores del archivo de configuración");
                return false;
            }
        } catch (Exception e){
            LOGGER.log(Level.SEVERE, "ERROR: No se puede leer correctamente el archivo de configuración");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            setEmail_subjet("Error");
            setEmail_content(" No se puede leer correctamente el archivo de configuración" + Controller.getStackTrace(e));
            return false;
        }
        
    }
    
    /**
    * Esta funcion nos permite convertir el stackTrace en un String, necesario para poder imprimirlos al log debido a
    * cambios en como Java los maneja internamente
    * @param e Excepcion de la que queremos el StackTrace
    * @return StackTrace de la excepcion en forma de String
    */
    public static String getStackTrace(Exception e) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        e.printStackTrace(pWriter);
        return sWriter.toString();
    }

    /**
    * Envía notificaciones una vez finalizado el proceso de carga,
    * ya sea exitoso o no y ante un error inesperado.
    */
    public void notificar(){
        this.sender = new MailSender();
        this.addresses = new InternetAddress();
        String [] destinatarios = dtoConfig.getAddresses();
        for (int i = 0; i < destinatarios.length; i++) {
            addresses.setAddress(destinatarios[i]);
        }
        try {
            sender.systemSender(addresses, this.getEmail_subjet(), this.getEmail_content());
        } catch (IOException | MessagingException e) {
            LOGGER.log(Level.SEVERE, "ERROR: No se pudo enviar notificaciones");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
        }
    }
}
