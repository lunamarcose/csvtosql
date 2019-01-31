package csvtosql;

import java.io.IOException;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws SQLException, IOException {
        // Objeto controlador
        Controller controlador = new Controller();
        controlador.iniciarProceso(); // Se inicia el procesamiento
    }
}
