package csvtosql;

import db.IndireccionPersistencia;
import db.PostgresHelper;
import java.io.IOException;
import java.sql.SQLException;


public class Controller {
    
    private PostgresHelper cliente;
    private Expert experto = new Expert();
    
    public boolean conectar() throws SQLException, IOException{
        this.cliente = IndireccionPersistencia.conectar();
        // Me conecto a la base de datos
        if(this.cliente != null){
            System.out.println("Conectado a la DB");
            return true;
        }
        return false;
    }
    
    public boolean desconectar() throws SQLException{
        if(this.cliente != null){
            this.cliente.disconnect();
            System.out.println("Desconectado de la DB");
            return true;
        }
        return false;
    }
    
    public boolean cargarCSV() throws SQLException, IOException{
        if(conectar()){
            if(this.experto.cargarCSV(this.cliente)){
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
    
    public boolean moverCSV() throws IOException{
        boolean moveCompleted = experto.moverCSV();
        if (moveCompleted){
            System.out.println("Se ha movido el .csv al contenedor correspondiente");
            return true;
        } else {
            System.out.println("No se ha podido mover el .csv");
            return false;
        }
    }
    
    public boolean verificarCSV() throws IOException{
        boolean existeCSV = experto.verificarExistenciaCSV();
        if (existeCSV){
            
        }
        boolean estadoCSV = experto.verificarCSV();
        if(estadoCSV){
            System.out.println("El csv tiene el formato adecuado");
        } else {
            System.out.println("El csv no cumple con el formato");
        }
        return estadoCSV;
    }
    
    public boolean verificarExistenciaCSV() throws IOException{
        boolean existeCSV = experto.verificarExistenciaCSV();
        if(existeCSV){
            System.out.println("Se ha detectado el .csv en el directorio");
            return true;
        } else {
            System.out.println("No se ha encontrado el .csv a procesar en el directorio");
            return false;
        }
    }
    
    public boolean verificarDatosAnteriores(){
        boolean estadoDatosAnteriores = experto.verificarDatosAnteriores();
        if(estadoDatosAnteriores){
            System.out.println("La tabla se encuentra sin datos, es posible cargar el .csv");
            return true;
        } else {
            System.out.println("La tabla posee datos previos. No es posible cargar el csv. Contacte soporte");
            return false;
        }
    }
}
