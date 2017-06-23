package visnja;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;
import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.CEInfo;
import yu.ac.bg.rcub.binder.handler.client.ClientConnector;
import yu.ac.bg.rcub.binder.handler.client.ClientConnectorFactory;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientExternalXML extends Thread {

    private Properties properties = null;
    private ClientConnector clientConn = null;
    private final long waitTime;
    private DataInputStream in;
    private DataOutputStream out;
    Logger logger = Logger.getLogger(ClientExternalXML.class);
    private String xmlInputData;

    public ClientExternalXML(String[] args) {

        super("TestClient");
        this.waitTime = 1000;
        
        try {
        	// Cita XML fajl iz komandne linije i pakuje u string
        	xmlInputData = new String(Files.readAllBytes(Paths.get(args[0])));
        } catch (IOException e) {
        	System.err.println("Fajl " + args[0] + "nije moguce otvoriti.");
		}

        init("ExternalXML.properties");
    }

    private void init(String propFilePath) {
        /* Read client properties from the file. */
        properties = new Properties();
        try {
            properties.load(new FileInputStream(new File(propFilePath)));
        } catch (FileNotFoundException e1) {
            logger.error("External properties file not found!", e1);
            System.exit(1);
        } catch (IOException e2) {
            logger.error("Error while reading External properties file!", e2);
            System.exit(1);
        }
    }

    private void communicate() throws BinderCommunicationException, InterruptedException {

        Thread.sleep(waitTime);

        try {
            in = new DataInputStream(clientConn.getInputStream());
            out = new DataOutputStream(clientConn.getOutputStream());
            
            String poruka = "OK";
            
            // Poslati prvo Optimization_UUID da bi se znalo o kojoj optimizaciji se radi
            BinderUtil.writeString(out, properties.getProperty("OptimizationUUID"));

            // Posalji ulazni XML
            BinderUtil.writeString(out, xmlInputData);
            
            // Ucitaj liniju sa servera i proveri status
            String line = BinderUtil.readString(in);

            // Ako je radnik izracunao vraca "OK"
            if (line.equalsIgnoreCase("OK")) {
            	
            	// Iscitaj sadrzaj izlaznog XML-a
            	String xmlOutputData = BinderUtil.readString(in);
            	
            	System.out.println(";" + "OK" + ";" + xmlOutputData);
            }
            // Ako je doslo do greske,tumaci gresku
            else if (!poruka.equalsIgnoreCase("")) 
                System.out.println(";" + line + ";" + "EXE_FAILED;");
            
            line = BinderUtil.readString(in);
            
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            logger.error("File not found " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Input/output error", e);
        }
    }

    private void testCEs() throws BinderCommunicationException {
        CEInfo[] ceInfos = clientConn.executeCEListMatch();
        int size = ceInfos.length;
        System.out.println("CE list match report by the binder, total of " + size + " CEs matched.");
        for (CEInfo ceInfo : ceInfos) {
            System.out.println(ceInfo);
        }
    }

    public void run() {
        logger.info("Starting External client...");
        try {
            clientConn = ClientConnectorFactory.createClientConnector(properties);
            testCEs();
            clientConn.connect();
            /* actual client communication */
            communicate();
            /* client finished */
            logger.info("End of External client, disconnecting from binder...");
            clientConn.disconnect();
        } catch (BinderCommunicationException be) {
            logger.error("Communication with the binder failed.", be);
        } catch (Exception e) {
            logger.error("Something's wrong!", e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // argumenti su niz parametara na osnovu kojih se vrsi evaluacija
        new ClientExternalXML(args).start();
    }
}
