package csvtosql;

import java.io.IOException;
import java.sql.SQLException;

public class Main {

   public static void main(String[] args) throws SQLException, IOException {
       // Controlador
       Controller controlador = new Controller();
       boolean existeCSV = controlador.verificarExistenciaCSV();
       if(existeCSV){ // Hay un archivo a procesar
            boolean estadoConexion = controlador.conectar();
            if(estadoConexion){
                boolean estadoTabla = controlador.verificarDatosAnteriores(); // Se verifica que la tabla intermedia esté con todos sus registros procesados
                if(estadoTabla){
                     boolean estadoCSV = controlador.verificarCSV();
                     if (estadoCSV){ // El .csv tiene formato válido, iniciar a procesar
                         if(controlador.modificarCSV()){
                            boolean csvCargado = controlador.cargarCSV(); // Se carga el csv
                            if(csvCargado){ // Si se completó la carga, proceder a mover el csv y renombrar
                                controlador.moverCSV(); // Se mueve de carpeta el csv previamente cargado y renombrado
                            }
                         }
                     }
                }
            }
       }
    }
}
