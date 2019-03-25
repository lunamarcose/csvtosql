package csvtosql;

import db.IndireccionPersistencia;
import db.PostgresHelper;
import dto.DTOConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.mail.MessagingException;
import notification.MailSender;


public class Controller {
    
    private PostgresHelper cliente;
    private Expert experto = new Expert();
    private CSV csv;
    private DTOConfig dtoConfig;
    private final static Logger LOGGER = Logger.getLogger("csvtosql.App");
    private Email notification;
    
    /**
    * Controla los pasos a seguir durante el procedimiento  de carga
    */
    public void iniciarProceso() throws IOException, SQLException, InterruptedException, MessagingException{
        startLogger();
        // Verificamos el archivo de configuraciones
        if(verificarEstadoConfig()){ // El archivo de configuración posee todos los datos cargados
            // Inicialización de notificaciones
            inicializarNotificaciones();
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
            // Si las notificaciones están habilitadas, notifica
            if(this.notification.isEnabled()){
                notificar();
            }
        }
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
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error de IO al inicializar los logs");
            LOGGER.log(Level.WARNING, Controller.getStackTrace(e));
        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "Error de Seguridad al inicializar los logs");
            LOGGER.log(Level.WARNING, Controller.getStackTrace(e));
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
                LOGGER.log(Level.SEVERE, "No se ha podido conectar a la DB");
                notification.setEmail_subjet("Error");
                notification.setEmail_content("No se ha podido conectar a la DB");
                return false;
            }
        } catch (IOException | SQLException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha podido conectar a la DB");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            notification.setEmail_subjet("Error");
            notification.setEmail_content("No se ha podido conectar a la DB. Detalle del error:\n" + Controller.getStackTrace(e));
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
                notification.setEmail_subjet("Error");
                notification.setEmail_content("No se ha podido desconectar de la DB. Detalle del error:\n" + Controller.getStackTrace(e));
                return false;
            }
        }
        LOGGER.log(Level.SEVERE, "No se ha podido desconectar de la DB");
        notification.setEmail_subjet("Error");
        notification.setEmail_content("No se ha podido desconectar de la DB: ");
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
                    notification.setEmail_subjet("Operación completada");
                    notification.setEmail_content("Se han cargado correctamente los " + csv.getCantidadRegistros() + " registros del archivo .csv. ");
                    desconectar();
                    return true;
                } else {
                    LOGGER.log(Level.SEVERE, "No se ha completado el proceso de carga en su totalidad, o existen datos previos sin procesar en la base de datos. Favor contacte soporte.");
                    notification.setEmail_subjet("Error");
                    notification.setEmail_content("No se ha completado el proceso de carga en su totalidad, o existen datos previos sin procesar en la base de datos. Favor contacte soporte.");
                    desconectar();
                    return false;
                }
            } catch (IOException | InterruptedException | SQLException e){
                LOGGER.log(Level.SEVERE, "ERROR: No se ha podido cargar el .csv a la base de datos");
                LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
                notification.setEmail_subjet("Error");
                notification.setEmail_content("ERROR: No se ha podido cargar el .csv a la base de datos. Detalle del error:\n" + Controller.getStackTrace(e));
                return false;
            }
        }
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
                notification.setEmail_subjet("Operación parcialmente  completada");
                notification.setEmail_content("Se informa que no se ha podido mover el .csv. Favor contacte soporte para regularizar la situación.");
                return false;
            }
        } catch (IOException e){
            LOGGER.log(Level.WARNING, "ERROR: No se ha podido mover el .csv");
            LOGGER.log(Level.WARNING, Controller.getStackTrace(e));
            notification.setEmail_subjet("Operación parcialmente  completada");
            notification.setEmail_content("Se informa que no se ha podido mover el .csv. Favor contacte soporte para regularizar la situación. Detalle del error:\n" + Controller.getStackTrace(e));
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
                LOGGER.log(Level.SEVERE, "El .csv no puede ser procesado debido al siguiente motivo: " + csv.getIsValid_motive());
                notification.setEmail_subjet("Error");
                notification.setEmail_content("El .csv no puede ser procesado debido al siguiente motivo: " + csv.getIsValid_motive());
            }
        } catch(IOException | SQLException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha podido conectar a la DB");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            notification.setEmail_subjet("Error");
            notification.setEmail_content("No se ha podido conectar a la DB. Detalle del error:\n" + Controller.getStackTrace(e));
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
        try{
            csv = experto.verificarExistenciaCSV(dtoConfig);
        } catch (NullPointerException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha encontrado la ruta del .csv especificada");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            notification.setEmail_subjet("Error");
            notification.setEmail_content("No se ha encontrado la ruta del .csv especificada. Detalle del error:\n" + Controller.getStackTrace(e));
            return csv;
        }
        if(csv!=null){
            LOGGER.log(Level.INFO, "Se ha detectado un .csv a procesar en la ruta: " + dtoConfig.getCsv_location() );
        } else {
            LOGGER.log(Level.SEVERE, "No se pudo procesar el .csv. Esto puede deberse a que el mismo no exista, o se encuentre más de un .csv en la ruta: " + dtoConfig.getCsv_location());
            notification.setEmail_subjet("Error");
            notification.setEmail_content("No se pudo procesar el .csv. Esto puede deberse a que el mismo no exista, o se encuentre más de un .csv en la ruta: " + dtoConfig.getCsv_location());
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
            if(dto.isIsValid()){
                this.dtoConfig = dto;
                LOGGER.log(Level.INFO, "El archivo de configuración fue validado exitosamente");
                return true;
            } else {
                LOGGER.log(Level.SEVERE, "El archivo de configuraciones no es correcto: " + dto.getIsValidMotive());
                return false;
            }
        } catch (Exception e){
            LOGGER.log(Level.SEVERE, "ERROR: No se puede leer correctamente el archivo de configuración");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
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
     * Se inicializa el objeto de notificaciones con los valores del archivo de configuración
     */
    public void inicializarNotificaciones(){
        this.notification = new Email(this.dtoConfig.isNotification_flag(), this.dtoConfig.getAddresses(), this.dtoConfig.getNotification_subject());
    }
    
    /**
    * Envía notificaciones una vez finalizado el proceso de carga,
    * ya sea exitoso o no y ante un error inesperado.
    */
    public void notificar(){
        this.notification.setSender(new MailSender());
        try {
            this.notification.getSender().systemSender(notification.getAddresses(), notification.getEmail_subjet(), notification.getEmail_content());
            LOGGER.log(Level.INFO, "Email enviado correctamente");
        } catch (IOException | MessagingException e) {
            LOGGER.log(Level.WARNING, "ERROR: No fue posible enviar notificaciones");
            LOGGER.log(Level.WARNING, Controller.getStackTrace(e));
        }
    }
}
