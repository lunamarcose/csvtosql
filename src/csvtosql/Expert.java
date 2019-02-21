package csvtosql;

import config.GetPropertyValues;
import db.PostgresHelper;
import dto.DTOConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class Expert {
    
    GetPropertyValues properties = new GetPropertyValues();
    
    /**
    * Ejecuta la carga mediante el comando copy del .csv a la base de datos y tablas especificadas
    * @param csv CSV, cliente PostgresHelper, dtoConfig DTOConfig
    * @return boolean
    */
    public boolean cargarCSV(PostgresHelper cliente, CSV csv, DTOConfig dtoConfig) throws IOException, SQLException, InterruptedException{  
        // Parámetros utilizados para el proceso de carga
        String tabla = dtoConfig.getTabla_intermedia();
        String tabla_aux = dtoConfig.getTabla_auxiliar();
        String columnas = dtoConfig.getColumns();
        String tabla_columnas = tabla + "(" +columnas + ")";
        String location = csv.getFullPath();
        char separatorChar = dtoConfig.getSeparator_char();
        char quotesChar = dtoConfig.getQuotes_char();
        boolean procesoCompletado = false;
        boolean incluir_columnas = Boolean.parseBoolean(properties.getPropValue("include_columns"));
        String header_condition = "";
        if(incluir_columnas){
            header_condition = " HEADER"; // Se agrega al comando copy en caso de incluir encabezados
        }
        
        // Antes de cargar los valores a la tabla intermedia, debo verificar que se hayan procesado correctamente los anteriores
        // Query para encontrar los campos que NO han sido procesados correctamente en la tabla intermedia, debe devolver vacío para continuar con la carga
        //String queryNoCopiados = "SELECT COUNT (*) FROM " + tabla + "WHERE flag_registro_copiado != 't'";
        String queryNoCopiados = "SELECT * FROM tabla_intermedia WHERE flag_registro_copiado IS NULL";
        ResultSet rs1 = cliente.execQuery(queryNoCopiados);
        if (!rs1.next()){
            // Vacio la tabla de usuarios antes de la siguiente ejecución
            String queryDelete = "DELETE FROM " + tabla;
            cliente.execUpdate(queryDelete);
            // Query para ejecutar el copy
            String queryCopy = "COPY " + tabla_columnas + " FROM " + "'" + location + "'" + "DELIMITER '" + separatorChar +"' QUOTE '"+ quotesChar + quotesChar +"' CSV" + header_condition + ";"; // Agregar parametrizado
            // Ejecuto la query
            cliente.execUpdate(queryCopy);
            // Verifico si se terminaron de cargar los datos mediante el comando COPY
            for (int i = 1; i < 11; i++) { // Durante 10 segundos, cada un segundo
                String query2 = "SELECT COUNT(*) FROM " + tabla;
                ResultSet rs = cliente.execQuery(query2);
                if(rs.next()){
                    rs.getInt(1);
                    if(rs.getInt(1) == csv.getCantidadRegistros()){ // Cantidad de registros en la tabla vs cantidad de registros del .csv
                        procesoCompletado = true; // Se terminaron de cargar todos los registros
                        break;
                    }
                }
                Thread.sleep(1000);
            }
            if(procesoCompletado){ // Si se cargaron todos los registros, cargo en la tabla auxiliar
                String queryAux = "INSERT INTO " + tabla_aux + "(porcentaje_bajas_calculado,porcentaje_bajas_tolerado,cantidad_registros_cargados,fecha_procesamiento) VALUES (" + csv.getPorcentaje() + "," + dtoConfig.getTolerance_percentage() + "," + csv.getCantidadRegistros() + ",CURRENT_TIMESTAMP(2));";
                cliente.execUpdate(queryAux);
                return true;
            }
        }
        return false;
    }
    
    /**
    * Se encarga de mover y renombrar el .csv al directorio correspondiente al finalizar la carga
    * @param csv CSV, dtoConfig DTOConfig
    * @return boolean
    */
    public boolean moverCSV(CSV csv,DTOConfig dtoConfig) throws IOException{
        
        // Obtengo los valores del archivo de propiedades
        String csv_path = csv.getFullPath();
        String csv_location_old = dtoConfig.getCsv_location_old();
        
        // Muevo .csv a otro directorio
        Path source = Paths.get(csv_path);
        Date fecha_actual= new Date();
        String fecha_actual_str = new SimpleDateFormat("yyyy-MM-dd").format(fecha_actual);
        Path target = Paths.get(csv_location_old + "/" + fecha_actual_str + ".csv.old");
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e){
            throw(e);
        }
        return true;
    }
    
    /**
    * Esta funcion verifica que el contenido del .csv cumpla con el formato establecido para su carga
    * @param cliente PostgresHelper, csv CSV, dtoConfig DTOConfig
    * @return boolean
    */
    public CSV verificarCSV(PostgresHelper cliente, CSV csv, DTOConfig dtoConfig) throws FileNotFoundException, IOException, SQLException{       
        
        // Valores utilizados en la función
        String csv_path = csv.getFullPath();
        String encoding = dtoConfig.getEncoding();
        String[] nombres_columnas = dtoConfig.getColumns().split(",");
        String[] nombres_columnas_req = dtoConfig.getColumns_required().split(",");
        boolean incluir_columnas = dtoConfig.isInclude_columns();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csv_path), encoding));
        String line;
        int lineNumber = 0;
        
        // Por cada linea del archivo .csv
        while((line=br.readLine())!=null){
            lineNumber++;
            String line_trim = line.trim();
            // En caso de incluir el encabezado, verifico que conicida con las columnas con lo definido en el archivo de configuraciones
            if((incluir_columnas) && (lineNumber < 2)){
                String[] campos_header = line.split(",");
                if(!Arrays.equals(campos_header, nombres_columnas)){
                    csv.setIsValid(false);
                    String mensajeError = "No coinciden los nombres de las columnas con los del encabezado del archivo .csv";
                    csv.setIsValid_motive(mensajeError);
                    return csv;
                }
                continue;
            }
            
            ArrayList<String> columnas = obtenerColumnas(line_trim, dtoConfig); // Obtengo ArrayList con los campos (columnas) que existen en esta línea
            int cantidad_columnas = columnas.size();
            if (cantidad_columnas == nombres_columnas.length){ // Deben ser la cantidad de campos definida en el archivo de config
                // Compruebo que para los valores requeridos existan datos
                HashMap<String,String> hashColumns = new HashMap<>();
                ArrayList<String> col_req_noVal = new ArrayList();
                for(int i=0; i < nombres_columnas.length; i++){
                    for(int j=0; j < nombres_columnas_req.length;j++){
                        if(nombres_columnas[i].equals(nombres_columnas_req[j])){
                            if(!columnas.get(i).equals("")){
                                hashColumns.put(nombres_columnas_req[j],columnas.get(i));
                            } else {
                                col_req_noVal.add(nombres_columnas_req[j]);
                            }
                        }
                    }
                }
                if(col_req_noVal.size() > 0){
                    csv.setIsValid(false);
                    String mensajeError = "Los siguientes campos requeridos no poseen valor para los registros en las filas:";
                    for(int i = 0; i < col_req_noVal.size(); i++){
                        mensajeError += "\n" + lineNumber + " - " + col_req_noVal.get(i);
                    }
                    csv.setIsValid_motive(mensajeError);
                    return csv;
                }
            } else {
                String mensajeError = "El registro " + lineNumber + " no tiene la cantidad de filas necesaria (" + cantidad_columnas + ")";
                csv.setIsValid_motive(mensajeError);
                csv.setIsValid(false);
                return csv;
            }
        }
        
        // Si en el .csv se incluye el header, la cantidad de registros se debe disminuir en 1
        if(incluir_columnas){
            csv.setCantidadRegistros(lineNumber -1);
        } else {
            csv.setCantidadRegistros(lineNumber);
        }
        br.close();
        
        // Verificación de cantidad de registros, en comparación con los registros anteriores en la tabla
        // Verificar si el porcentaje de tolerancia de bajas se cumple
        // Obtengo de la tabla auxiliar la cantidad de registros cargados en la última ejecución
        int cantidadRegistrosTabla = 0;
        String query_latestRecords = "SELECT cantidad_registros_cargados FROM auxiliar ORDER BY fecha_procesamiento DESC LIMIT 1;"; // especificar el esquema y revisar
        ResultSet rs = cliente.execQuery(query_latestRecords);
        if(rs.next()){
            cantidadRegistrosTabla = rs.getInt(1);
        }
        // Obtengo del .csv la cantidad de registros a cargar
        int cantidadRegistrosCSV = csv.getCantidadRegistros();
        // Máxima tolerancia
        double tolerancia = Double.parseDouble(properties.getPropValue("tolerance_percentage"));
        // Realizo el cálculo del porcentaje
        int diferenciaRegistros = cantidadRegistrosTabla - cantidadRegistrosCSV;
        if(diferenciaRegistros > 0){
            double porcentaje = diferenciaRegistros * 100 / cantidadRegistrosTabla;
            csv.setPorcentaje(porcentaje);
            // Si es mayor al porcentaje de tolerancia no se debe procesar
            if(porcentaje > tolerancia){
                csv.setIsValid(false);
                csv.setIsValid_motive("No se cumple el porcentaje de bajas definido por configuración. Procentaje definido: " + tolerancia + ", porcentaje calculado: " + porcentaje);
                return csv;
            }
        }
        // Si el .csv pasó todas las validaciones
        csv.setIsValid(true);
        return csv;
    }
    
    /**
    * Recibe una línea (Str) del .csv, y lo divide en los campos
    * @param cadena String, dtoConfig DTOConfig
    * @return ArrayList con los diferentes campos que posee una línea del .csv
    */
    public ArrayList obtenerColumnas(String cadena, DTOConfig dtoConfig) throws IOException{
        
        char caracter_delimitador = dtoConfig.getQuotes_char();
        char caracter_separador = dtoConfig.getSeparator_char();
        boolean flag_conteo = true;
        String palabra = "";
        ArrayList<String> valores = new ArrayList();
        int cantidad_columnas = 0;
        
        for (int i = 0; i < cadena.length(); i++){
            char c = cadena.charAt(i);
            if(c == caracter_delimitador){
                if((palabra.length() > 0) && ((i == cadena.length()-1) || (""+c+cadena.charAt(i+1)).equals("\',"))){ // Fin de palabra
                    valores.add(palabra);
                    palabra = "";
                }
                if(i == 0){ // Es el primer caracter de la cadena
                    flag_conteo = !flag_conteo;
                } else if(i == cadena.length()-1) { // Es el ultimo caracter de la cadena
                        flag_conteo = !flag_conteo;
                    } else if((""+c+cadena.charAt(i+1)).equals("\',")){ // Uno de los posibles caracteres intermedios
                            flag_conteo = !flag_conteo;
                        } else if((""+cadena.charAt(i-1)+c).equals(",\'")){ // Otro de los posibles caracteres intermedios
                                flag_conteo = !flag_conteo;
                            }
            } else {
                if(!flag_conteo){
                    palabra = palabra+c;
                }
            }
            if((c == caracter_separador) && flag_conteo){
                cantidad_columnas++;
                if(i == 0){ // El primer item venia vacio
                    valores.add("");
                } else if(((""+cadena.charAt(i-1)).equals(",")) || (i == cadena.length()-1)){ // Cadenas intermedias vacías
                    valores.add("");
                }
            }
        }
        return valores;
    }
    
    /**
    * Se comprueba que exista un .csv a procesar, de ser así se crea un objeto del tipo
    * CSV, donde se cargan los parametros principales del mismo utilizados en el proceso de carga
    * @param dtoConfig DTOConfig
    * @return CSV objeto con los datos principales utilizados en el procesamiento del mismo
    */
    public CSV verificarExistenciaCSV(DTOConfig dtoConfig){
        
        // Obtengo los valores del archivo de propiedades
        String csv_location = "";
        csv_location = dtoConfig.getCsv_location();
        int cantidadCSV = 0;
        String nombreCSV = "";
        
        if(!csv_location.equals("")){
            File folder = new File(csv_location);
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                  String fileName = listOfFiles[i].getName();
                  if(fileName.endsWith(".csv")){
                      cantidadCSV++;
                      nombreCSV = fileName;
                  }
                }
            }
        } else {
            return null;
        }
        
        // No pueden exisitr más de un archivo .csv en el directorio
        if(cantidadCSV > 1){
            return null;
        } else if (cantidadCSV == 1){
            String fullPath = csv_location + "/" + nombreCSV;
            CSV csv = new CSV(fullPath);
            return csv;
        } else {
            return null;
        }
    }
    
    /**
    * Verifica y carga las configuraciones almacenadas en el .properties.
    * @return DTOConfig
    */
    public DTOConfig verificarEstadoConfig() throws IOException{
        try{
            DTOConfig dto = properties.getPropValues();
            if(dto.isIsValid()){
                dto.cargarValores(properties.getPropValue("db_name"),
                properties.getPropValue("csv_location"),
                properties.getPropValue("csv_location_old"),
                properties.getPropValue("log_location"),
                properties.getPropValue("intermediate_table"),
                properties.getPropValue("aux_table"),
                properties.getPropValue("columns"),
                properties.getPropValue("columns_required"),
                properties.getPropValue("encoding"),
                properties.getPropValue("mail_addresses"),
                properties.getPropValue("include_columns"),
                properties.getPropValue("separator_char"),
                properties.getPropValue("quotes_char"),
                properties.getPropValue("notification_flag"),
                properties.getPropValue("notification_subject"),
                properties.getPropValue("pre_verification"),
                properties.getPropValue("tolerance_percentage"));
            }
            return dto;
        } catch(IOException | NullPointerException e){
            throw e;
        }
    }
}