package csvtosql;

import java.io.IOException;
import java.sql.SQLException;
import javax.mail.MessagingException;

public class Main {

    public static void main(String[] args) throws SQLException, IOException, InterruptedException, MessagingException {
        // Objeto controlador
        Controller controlador = new Controller();
        controlador.iniciarProceso(); // Se inicia el procesamiento
    }
}
