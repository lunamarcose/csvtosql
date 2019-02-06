package csvtosql;

import db.IndireccionPersistencia;
import db.PostgresHelper;
import dto.DTOConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
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
    
    // Gestión de logs
    private final static Logger LOGGER = Logger.getLogger("bitacora.App");
    
    public void startLogger(){
        try {
            Handler consoleHandler = new ConsoleHandler();
            Handler fileHandler = new FileHandler("./bitacora.log", false);
            SimpleFormatter simpleFormatter = new SimpleFormatter();
            fileHandler.setFormatter(simpleFormatter);
            LOGGER.addHandler(consoleHandler);
            LOGGER.addHandler(fileHandler);
            consoleHandler.setLevel(Level.ALL);
            fileHandler.setLevel(Level.ALL);

            
            
            //LOGGER.log(Level.INFO, "Bitacora inicializada");
            // Estas llamadas se registraran en el log
            //LOGGER.log(Level.INFO, "Llamadas a los componentes del sistema");
            //LOGGER.log(Level.SEVERE, "ERROR MASIVO");
            //LOGGER.log(Level.INFO, "Proceso exitoso");
            //LOGGER.log(Level.WARNING, "Ocurrio un error de acceso en 0xFF");
            //LOGGER.log(Level.INFO, "Probando manejo de excepciones");
            //try {
            //    throw new Exception("ERROR DE CONTROL DE FLUJO DE PROGRAMA");
            //} catch (Exception e) {
                // Mediante el metodo getStack obtenemos el stackTrace de la excepcion en forma de un objecto String
                // de modo que podamos almacenarlo en bitacora para su analisis posterior
                //LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            //}
            //LOGGER.log(Level.INFO, "Programa terminado exitosamente");
            //System.out.println("Puede ver el log en el archivo bitacora.log");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error de IO");
        } catch (SecurityException ex) {
            LOGGER.log(Level.SEVERE, "Error de Seguridad");
        }
    }
    
    
    // Inicia las verificaciones y posterior carga del archivo .csv
    public void iniciarProceso() throws IOException, SQLException, InterruptedException, MessagingException{
        startLogger();
        LOGGER.log(Level.INFO, "Inicio del proceso de carga");
        // Comienza el proceso de carga del CSV
        // Verificamos el archivo de configuraciones
        if(verificarEstadoConfig()){ // El archivo de configuración posee todos los datos cargados
            this.sender = new MailSender();
            this.addresses = new InternetAddress();
            String [] destinatarios = dtoConfig.getAddresses();
            for (int i = 0; i < destinatarios.length; i++) {
                addresses.setAddress(destinatarios[i]);
            }
            sender.systemSender(addresses, "Inicio de proceso de carga", "Esta notificación es para informarle que se ha iniciado el proceso de carga de csv");
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
                            sender.systemSender(addresses, "Inicio de proceso de carga", "Esta notificación es para informarle que se ha completado el proceso de carga de csv");
                        }
                    }
                }
            }
        }
    }
    
    // Utilizado para conectarse a la base de datos destino
    public boolean conectar(){
        LOGGER.log(Level.INFO, "Intentando conectar a la base de datos...");
        try{
            this.cliente = IndireccionPersistencia.conectar();
            if(this.cliente != null){
                LOGGER.log(Level.INFO, "Conectado a la DB");
                return true;
            } else {
                LOGGER.log(Level.WARNING, "No se ha podido conectar a la DB");
                return false;
            }
        } catch (IOException | SQLException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha podido conectar a la DB");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            return false;
        }
    }
    
    // Utilizado para desconectarse de la base de datos destino
    public boolean desconectar(){
        if(this.cliente != null){
            try{
                this.cliente.disconnect();
                LOGGER.log(Level.INFO, "Desconectado de la DB");
                return true;
            } catch (SQLException e){
                LOGGER.log(Level.SEVERE, "ERROR: No se ha podido conectar a la DB");
                LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
                return false;
            }
        }
        LOGGER.log(Level.WARNING, "No se ha podido desconectar de la DB");
        return false;
    }
    
    public boolean cargarCSV(CSV csv){
        LOGGER.log(Level.INFO, "Se esta cargando el .csv a la DB");
        if(this.cliente != null){
            try{
                if(this.experto.cargarCSV(this.cliente, csv)){
                    LOGGER.log(Level.INFO, "Se ha cargado correctamente el .csv");
                    desconectar();
                    return true;
                } else {
                    LOGGER.log(Level.SEVERE, "No se ha cargado el .csv en su totalidad, o existen datos previos sin procesar. Favor contacte soporte.");
                    desconectar();
                    return false;
                }
            } catch (IOException | InterruptedException | SQLException e){
                LOGGER.log(Level.SEVERE, "ERROR: No se ha podido cargar el .csv");
                LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
                return false;
            }
        }
        //System.out.println("No es posible conectarse a la DB");
        return false;
    }
    
    public boolean moverCSV(CSV csv) throws MessagingException{
        LOGGER.log(Level.INFO, "Se procede a mover el .csv al directorio " + dtoConfig.getCsv_location_old());
        try{
            boolean moveCompleted = experto.moverCSV(csv);
            if (moveCompleted){
                LOGGER.log(Level.INFO, "Se ha movido el .csv al contenedor " + dtoConfig.getCsv_location_old());
                MailSender sender = new MailSender();
                InternetAddress addresses = new InternetAddress();
                addresses.setAddress("mluna@assertiva.biz");
                sender.systemSender(addresses, "Carga de CSV Exitosa", "Esta notificación es para informarle que el csv ha sido cargado de forma exitosa");
                return true;
            } else {
                LOGGER.log(Level.WARNING, "No se ha podido mover el .csv");
                return false;
            }
        } catch (IOException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha podido mover el .csv");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            return false;
        }
    }
    
    public boolean verificarCSV(PostgresHelper cliente, CSV csv){
        LOGGER.log(Level.INFO, "Verificando el contenido del .csv a procesar...");
        boolean estadoCSV = false;
        try{
            estadoCSV = experto.verificarCSV(cliente, csv).isIsValid();
            if(estadoCSV){
                LOGGER.log(Level.INFO, "El csv tiene el formato adecuado");
            } else {
                LOGGER.log(Level.WARNING, "El csv no cumple con el formato establecido: " + csv.getIsValid_motive());
            }
        } catch(IOException | SQLException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha podido conectar a la DB");
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            return estadoCSV;
        }
        return estadoCSV;
    }
    
    public CSV verificarExistenciaCSV(){
        LOGGER.log(Level.INFO, "Verificando existencia de archivo .csv a procesar...");
        CSV csv = null;
        try{
            csv = experto.verificarExistenciaCSV();
            if(csv!=null){
                LOGGER.log(Level.INFO, "Se ha detectado un .csv a procesar en la ruta: " + dtoConfig.getCsv_location() );
                return csv;
            } else {
                LOGGER.log(Level.WARNING, "No se pudo procesar el .csv. Verifique que exista y no se encuentren varios .csv en la ruta: " + dtoConfig.getCsv_location());
            }
            return csv;
        } catch (IOException e){
            LOGGER.log(Level.SEVERE, "ERROR: No se ha encontrado la ruta: " + dtoConfig.getCsv_location());
            LOGGER.log(Level.SEVERE, Controller.getStackTrace(e));
            return null;
        }
    }
    
    public boolean verificarEstadoConfig(){
        LOGGER.log(Level.INFO, "Verificando el archivo de configuraciones...");
        try{
            DTOConfig dto = experto.verificarEstadoConfig();
            if(dtoConfig != null){
                this.dtoConfig = dto;
                LOGGER.log(Level.INFO, "El archivo de configuración fue validado exitosamente");
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Debe completar todos los valores del archivo de configuración");
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
}
