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
                if (this.moverCSV()){
                    System.out.println("Se ha movido el .csv al contenedor correspondiente");
                } else {
                    System.out.println("No se ha podido mover el .csv");
                }
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
        return experto.moverCSV();
    }
}
