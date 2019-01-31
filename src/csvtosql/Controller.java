package csvtosql;

import db.IndireccionPersistencia;
import db.PostgresHelper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;


public class Controller {
    
    private PostgresHelper cliente;
    private Expert experto = new Expert();
    private CSV csv;
    
    // Inicia las verificaciones y posterior carga del archivo .csv
    public void iniciarProceso() throws IOException, SQLException{
        // Comienza el proceso de carga del CSV
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
    }
    
    // Utilizado para conectarse a la base de datos destino
    public boolean conectar() throws SQLException, IOException{
        this.cliente = IndireccionPersistencia.conectar();
        if(this.cliente != null){
            System.out.println("Conectado a la DB");
            return true;
        }
        return false;
    }
    
    // Utilizado para desconectarse de la base de datos destino
    public boolean desconectar() throws SQLException{
        if(this.cliente != null){
            this.cliente.disconnect();
            System.out.println("Desconectado de la DB");
            return true;
        }
        return false;
    }
    
    public boolean cargarCSV(CSV csv) throws SQLException, IOException{
        if(this.cliente != null){
            if(this.experto.cargarCSV(this.cliente, csv)){
                System.out.println("Se ha cargado correctamente el .csv");
                desconectar();
                return true;
            } else {
                System.out.println("No se ha cargado el .csv, favor contacte soporte");
                desconectar();
                return false;
            }
        }
        System.out.println("No es posible conectarse a la DB");
        return false;
    }
    
    public boolean moverCSV(CSV csv) throws IOException{
        boolean moveCompleted = experto.moverCSV(csv);
        if (moveCompleted){
            System.out.println("Se ha movido el .csv al contenedor correspondiente");
            return true;
        } else {
            System.out.println("No se ha podido mover el .csv");
            return false;
        }
    }
    
    public boolean verificarCSV(PostgresHelper cliente, CSV csv) throws IOException, FileNotFoundException, SQLException{
        boolean estadoCSV = experto.verificarCSV(cliente, csv).isIsValid();
        if(estadoCSV){
            System.out.println("El csv tiene el formato adecuado");
        } else {
            System.out.println("El csv no cumple con el formato");
        }
        return estadoCSV;
    }
    
    public CSV verificarExistenciaCSV() throws IOException{
        return experto.verificarExistenciaCSV();
    }
    
    public boolean verificarEstadoConfig() throws IOException{
        return experto.verificarEstadoConfig();
    }
}
