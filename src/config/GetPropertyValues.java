package config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
 
public class GetPropertyValues {
    
    String result = "";
    InputStream inputStream;
    StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();     

    public boolean getPropValues() throws IOException {
        try {
                Properties prop = new Properties();
                String propFileName = "config.properties";

                inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

                if (inputStream != null) {
                        prop.load(inputStream);
                } else {
                        throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
                }
                
                String[] parametros = {"csv_location_old","intermediate_table","columns","columns_required","include_columns","separator_char","quotes_char","encoding","tolerance_percentage","aux_table","mail_addresses"};
                for (int i = 0; i < parametros.length; i++) {
                    String valor = prop.getProperty(parametros[i]);
                    if(valor == ""){
                        return false;
                    }
                }
        } catch (IOException e) {
            throw(e);
        } catch (NullPointerException e){
            throw(e);
        }
        finally {
            inputStream.close();
        }
        return true;
    }
    
    public String getPropValue(String property) throws IOException{        
        String property_value = "";
            try {
                Properties prop = new Properties();
                String propFileName = "config.properties";
                inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
                if (inputStream != null) {
                        prop.load(inputStream);
                } else {
                        throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
                }       
                String cfg_file_loc = prop.getProperty("cfg_path");   
                this.encryptor.setPassword("PASS"); // could be got from web, env variable...    
                Properties propEnc = new EncryptableProperties(this.encryptor);  
                propEnc.load(new FileInputStream(cfg_file_loc));
                property_value = propEnc.getProperty(property);
            } catch (Exception e){
                System.out.println("Exception: " + e);
            } finally {
                inputStream.close();
            }
        return property_value;
    }
}