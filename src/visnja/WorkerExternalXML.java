package visnja;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.OpenOption;
import java.util.Properties;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;
import yu.ac.bg.rcub.binder.handler.worker.WorkerHandler;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;


public class WorkerExternalXML implements WorkerHandler {

    private DataInputStream in;
    private DataOutputStream out;

    private BufferedReader ProcessInput;
    
    // Direktorijum u kome se drze sve optimizacije, svaka u svom poddirektorijumu
    public static String OPTIMIZATIONS_DIR = "optimizacije";
    // Naziv izvrsnog fajla
    public static String EXECUTABLE = "run_exe_xml.sh";
    // Naziv ulaznog XML fajla
    public static String XMLINPUT = "input.xml";
    // Naziv izlaznog XML fajla
    public static String XMLOUTPUT = "output.xml";
    

    public void run(WorkerConnector workerConnector) {

        workerConnector.log("Started a new EXTERNAL worker handler thread!");

        try {
            in = new DataInputStream(workerConnector.getInputStream());
            out = new DataOutputStream(workerConnector.getOutputStream());

            // Prihvati optimizationUUID
            String optimizationUUID = BinderUtil.readString(in);
            // Pretpostavka je da direktorijum optimizationDirectory vec postoji
            String optimizationDirectory = System.getProperty("user.dir") + 
                    File.separator + OPTIMIZATIONS_DIR + File.separator +  optimizationUUID;
            
            // Worker prima XML 
            String xmlData = BinderUtil.readString(in);
            
            // Snimi dobijeni string sa XML-om u fajl
            OpenOption[] options = new OpenOption[] { CREATE_NEW };
            Files.write(Paths.get(optimizationDirectory + File.separator + XMLINPUT), xmlData.getBytes(), options);
            
            // EXECUTABLE-u se salje lokacija maticne optimizacije da bi znao gde da smesti fajlove za statistiku
            String commandFullPath = optimizationDirectory + File.separator + EXECUTABLE;
            String[] commandArray = { commandFullPath, getBinderDir("Worker.properties"), OPTIMIZATIONS_DIR, optimizationUUID, XMLINPUT };
            (new File(commandFullPath)).setExecutable(true);
            
            // Parametri procesa
            ProcessBuilder pb = new ProcessBuilder(commandArray);
            pb.directory(new File(optimizationDirectory));
            pb.redirectErrorStream(true);

            try {
                final Process pr = pb.start();
                workerConnector.log("Started exe");
                
                // Cekanje da se proces zavrsi
                pr.waitFor();
                workerConnector.log("Finished exe");

                // Preuzimanje izlaza iz EXECUTABLE sa stdout i prosledjivanje klijentu 
                ProcessInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                String s = ProcessInput.readLine();
                workerConnector.log("Primio sam " + s);
                
                // Lokacija output xml fajla
                Path xmlOutputPath = Paths.get(optimizationDirectory + File.separator + XMLOUTPUT);
                
                // Ako EXECUTABLE nije digao exception i postoji izlazni XML
                if (s.equalsIgnoreCase("OK") && Files.exists(xmlOutputPath)) {
                	
                	// Procitaj sadrzaj izlaznog XML-a
                	String xmlOutputData = new String(Files.readAllBytes(xmlOutputPath));

                	// Prvo posalji OK
                	BinderUtil.writeString(out, "OK");
                    
                	// Onda posalji XML spakovan u string
                    BinderUtil.writeString(out, xmlOutputData);
                    
                // Inace hendlaj gresku
                } else {
                    workerConnector.log("Greska je " + s);
                    BinderUtil.writeString(out, s);
                }

                ProcessInput.close();
                pr.destroy();

                BinderUtil.writeString(out, "-finished-");
                
                in.close();
                out.close();
            } catch (IOException e) {
                workerConnector.log("MojExe nije startovao kako treba>>>> " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (FileNotFoundException e) {
            workerConnector.log("ServerDispatcherThread:   *** ERROR *** File not found ", e);
        } catch (IOException e) {
            workerConnector.log("ServerDispatcherThread:   *** ERROR *** IO Error occured while binder communicated with the client!!!", e);
        } catch (Exception e) {
            workerConnector.log("*** ERROR ***   Unknown error occured.", e);
        }
        workerConnector.log("EXTERNAL worker handler end.");
    }
    
    /**
     * Vraca direktorijum maticne instalacije Bindera, naveden u propFilePath
     * 
     * @param propFilePath
     * @return
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private String getBinderDir(String propFilePath) throws FileNotFoundException, IOException {

        Properties properties = new Properties();
        
        properties.load(new FileInputStream(new File(propFilePath)));
        
        return properties.getProperty("BinderDir");
    }

}
