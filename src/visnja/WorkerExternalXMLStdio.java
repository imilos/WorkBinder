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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.globus.ftp.vanilla.Command;

import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;
import yu.ac.bg.rcub.binder.handler.worker.WorkerHandler;

public class WorkerExternalXMLStdio implements WorkerHandler {

	private DataInputStream in;
	private DataOutputStream out;
	private BufferedReader ProcessInput;
	private BufferedWriter ProcessOutput;

	// Direktorijum u kome se drze sve optimizacije, svaka u svom poddirektorijumu
	public static String OPTIMIZATIONS_DIR = "optimizacije";
	// Naziv izvrsnog fajla
	public static String EXECUTABLE = "run_exe.sh";

	
	public void run(WorkerConnector workerConnector) {

		workerConnector.log("Started a new EXTERNAL worker handler thread!");

		try {
			in = new DataInputStream(workerConnector.getInputStream());
			out = new DataOutputStream(workerConnector.getOutputStream());

			// Prihvati optimizationUUID
			String optimizationUUID = BinderUtil.readString(in);
			// Pretpostavka je da direktorijum optimizationDirectory vec postoji
			String optimizationDirectory = System.getProperty("user.dir") + File.separator + OPTIMIZATIONS_DIR
					+ File.separator + optimizationUUID;

			// Worker prima XML
			String xmlData = BinderUtil.readString(in);

			// EXECUTABLE-u se salje lokacija maticne optimizacije da bi znao gde da smesti
			// fajlove za statistiku
			String commandFullPath = optimizationDirectory + File.separator + EXECUTABLE;
			String[] commandArray = { commandFullPath, getBinderDir("Worker.properties"), OPTIMIZATIONS_DIR, optimizationUUID };
			(new File(commandFullPath)).setExecutable(true);
			
			// Citanje parametara iz ulaznog XML-a 
			ArrayOfSolutionForXML inpTemp = ArrayOfSolutionForXML.readInputFromXML(xmlData);
			//ArrayOfSolutionForXML.SolutionForXML inp = ArrayOfSolutionForXML.readInputFromXMLSingle(xmlData);
			
			// Objekat za smestanje rezultata evaluacija
			ArrayOfEvaluationResult outTemp = new ArrayOfEvaluationResult();
			
			for (ArrayOfSolutionForXML.SolutionForXML inp : inpTemp.solutionsForXML) {
				
				workerConnector.log("Started exe");
				
				// Izvrsi evaluaciju
				ArrayOfEvaluationResult.EvaluationResult res = runEvaluation(commandArray, optimizationDirectory, inp);
				
				// Ispitaj status evaluacije
				if (res.Status.equalsIgnoreCase("DONE")) {
				    workerConnector.log("Finished EXE");
					// Dodaj rezultate evaluacije u izlazni objekat
				    outTemp.evaluationResults.add(res);
				}
				else {
					workerConnector.log("Greska u izvrsenju!");
					BinderUtil.writeString(out, res.Message);
				}
				
			}
			BinderUtil.writeString(out, "OK");
			// Posalji XML spakovan u string
			//BinderUtil.writeString(out, ArrayOfEvaluationResult.writeResultsToXMLSingle(res));
			BinderUtil.writeString(out, ArrayOfEvaluationResult.writeResultsToXML(outTemp));
			// Zavrsi komunikaciju sa klijentom
			BinderUtil.writeString(out, "-finished-");
			
			in.close();
			out.close();

		} catch (IOException e) {
			workerConnector.log("ServerDispatcherThread:   *** ERROR *** IO Error occured while binder communicated with the client!!!", e);
		} catch (Exception e) {
			workerConnector.log("*** ERROR ***   Unknown error occured.", e);
		}
		workerConnector.log("EXTERNAL worker handler end.");
	}



	/**
	 * 
	 * @param commandArray
	 * @param optimizationDirectory
	 * @param params
	 * @return 
	 */
	private ArrayOfEvaluationResult.EvaluationResult runEvaluation(String[] commandArray, String optimizationDirectory, ArrayOfSolutionForXML.SolutionForXML inp) {
		// Parametri procesa
		ProcessBuilder pb = new ProcessBuilder(commandArray);
		pb.directory(new File(optimizationDirectory));
		pb.redirectErrorStream(true);
		
		// Parametri iz ulaza
		List<Double> params = inp.Parameters;
		// Izlaz 
		ArrayOfEvaluationResult.EvaluationResult res = new ArrayOfEvaluationResult.EvaluationResult();
		
		// Dodaj GUID u rezultat
		res.Guid = inp.Guid;
		
		try {
			final Process pr = pb.start();

			/*
			 * Slanje niza doublova EXECUTABLE-u preko stdin-a. Prvo se salje broj
			 * parametara, a onda jedan po jedan parametar
			 */
			ProcessOutput = new BufferedWriter(new OutputStreamWriter(pr.getOutputStream()));
			ProcessOutput.write(Integer.toString(params.size()));
			ProcessOutput.newLine();
			
			for (Double p : params) {
				ProcessOutput.write(Double.toString(p));
				ProcessOutput.newLine();
				// Takodje dodaj varijable u rezultat
				res.Variables.add(p);
			}

			ProcessOutput.flush();
			ProcessOutput.close();

			// Preuzimanje izlaza iz EXECUTABLE sa stdout
			ProcessInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String s = ProcessInput.readLine();
			//workerConnector.log("Primio sam " + s);

			if (s.equalsIgnoreCase("OK")) {

				// Prvo primi broj rezultata koje ce ocitati
				int duzina = Integer.parseInt(ProcessInput.readLine());
									
				// Prima jedan po jedan rezultat
				for (int i = 0; i<duzina; i++)
					res.Result.add(Double.parseDouble(ProcessInput.readLine()));
				
				res.Status = "DONE";
				res.Message = "OK";
			}
			else {
				res.Status = "ERROR";
				res.Message = s;				
			}
			
			ProcessInput.close();
			pr.waitFor();
			pr.destroy();
			
		} catch (IOException e) {
			//workerConnector.log("MojExe nije startovao kako treba>>>> " + e.getMessage());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} 
		
		return res;
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
