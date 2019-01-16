package csvtosql;

import java.sql.SQLException;
import java.io.*;

public class Main {

   public static void main(String[] args) throws SQLException, IOException {
       
       // Controlador
       Controller controlador = new Controller();
       boolean existeCSV = controlador.verificarExistenciaCSV();
       if(existeCSV){ // Hay un archivo a procesar
           // Acá debo verificar si en la tabla intermedia quedaron valores
           // Si quedaron valores y se han cargado (flagcarga = true || flagAD = false)
           // puedo borrar la tabla, caso contrario hubo un error de procesamiento
           // funcionBorrarTabla();
           boolean estadoTabla = controlador.verificarDatosAnteriores();
           if(estadoTabla){
                boolean estadoCSV = controlador.verificarCSV();
                if (estadoCSV){ // El .csv tiene formato válido, iniciar a procesar
                    boolean csvCargado = controlador.cargarCSV(); // Se carga el csv
                    if(csvCargado){ // Si se completó la carga, proceder a mover el csv y renombrar
                        controlador.moverCSV(); // Se mueve de carpeta el csv previamente cargado y renombrado
                    }
                }
           }
       }
       
        //client.execQuery("COPY tabla_intermedia(nombre,apellido,edad)  FROM '/home/marcos/Escritorio/test.csv' WITH (Format csv);");
        
        //IndireccionPersistencia
        //GetPropertyValues properties = new GetPropertyValues();
        //properties.getPropValues();

        //client.execQuery("COPY tabla_intermedia(nombre,apellido,edad)  FROM '/home/marcos/Escritorio/test.csv' WITH (Format csv);");
    }
    
}
