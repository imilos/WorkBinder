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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
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
			String[] commandArray = { commandFullPath, getBinderDir("Worker.properties"), OPTIMIZATIONS_DIR,
					optimizationUUID };
			(new File(commandFullPath)).setExecutable(true);

			// Parametri procesa
			ProcessBuilder pb = new ProcessBuilder(commandArray);
			pb.directory(new File(optimizationDirectory));
			pb.redirectErrorStream(true);

			try {
				final Process pr = pb.start();
				workerConnector.log("Started exe");

				// Citanje parametara iz ulaznog XML-a 
				SolutionForXML inp = GetParametersFromXML(xmlData);				

				/*
				 * Slanje niza doublova EXECUTABLE-u preko stdinput-a. Prvo se salje broj
				 * parametara, a onda jedan po jedan parametar
				 */
				ProcessOutput = new BufferedWriter(new OutputStreamWriter(pr.getOutputStream()));
				ProcessOutput.write(Integer.toString(inp.NumOfPar));
				ProcessOutput.newLine();
				
				for (Double p : inp.Parameters) {
					ProcessOutput.write(Double.toString(p));
					ProcessOutput.newLine();
				}

				ProcessOutput.flush();
				ProcessOutput.close();
				
				EvaluationResult res = new EvaluationResult();

				// Preuzimanje izlaza iz EXECUTABLE sa stdout i prosledjivanje klijentu
				ProcessInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				String s = ProcessInput.readLine();
				workerConnector.log("Primio sam " + s);

				if (s.equalsIgnoreCase("OK")) {

					BinderUtil.writeString(out, "OK");
					// Prvo primi broj rezultata koje ce ocitati
					int duzina = Integer.parseInt(ProcessInput.readLine());

					res.Status = "DONE";
					res.Message = "OK";
										
					// Prima jedan po jedan rezultat
					for (int i = 0; i < duzina; i++)
						res.Result.add(Double.parseDouble(ProcessInput.readLine()));
										
					// Onda posalji XML spakovan u string					
					BinderUtil.writeString(out, EvaluationResultToXML(res));
				} else {
					workerConnector.log("Greska je " + s);
					BinderUtil.writeString(out, s);
				}

				ProcessInput.close();

				// Cekanje da se proces zavrsi
				pr.waitFor();
				pr.destroy();
				workerConnector.log("Finished exe");
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
			workerConnector.log(
					"ServerDispatcherThread:   *** ERROR *** IO Error occured while binder communicated with the client!!!",
					e);
		} catch (Exception e) {
			workerConnector.log("*** ERROR ***   Unknown error occured.", e);
		}
		workerConnector.log("EXTERNAL worker handler end.");
	}

	
	// Klasa SolutionXML za ulaz
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "SolutionForXML")
	static class SolutionForXML {

		@XmlElement(name = "NumOfPar")
		public int NumOfPar;

		@XmlElementWrapper(name = "Parameters")
		@XmlElement(name = "double")
		public List<Double> Parameters = new ArrayList<>();
		
		int getNumOfPar() {
			return this.NumOfPar;
		}
	}

	// Klasa EvaluationResult za izlaz
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "EvaluationResult")
	static class EvaluationResult {

		@XmlElement(name = "Status")
		public String Status;

		@XmlElement(name = "Message")
		public String Message;

		@XmlElementWrapper(name = "Result")
		@XmlElement(name = "double")
		public List<Double> Result = new ArrayList<>();

		@XmlElement(name = "Metadata")
		public String Metadata = "";
	}

	/**
	 * Vraca niz parametara iz XMLINPUT-a
	 * 
	 * @param String xmlData
	 * @return SolutionForXML
	 * @throws 
	 */
	SolutionForXML GetParametersFromXML(String xmlData) {

		SolutionForXML inp = new SolutionForXML();
		StringReader reader = new StringReader(xmlData);
		try {
			
			JAXBContext jaxbContext = JAXBContext.newInstance(SolutionForXML.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			inp = (SolutionForXML) jaxbUnmarshaller.unmarshal(reader);
			
			return inp;
			
		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Pakuje rezultate u XML format
	 * 
	 * @param EvaluationResult
	 * @return List
	 * @throws 
	 */
	String EvaluationResultToXML(EvaluationResult res) {
		
		StringWriter xmlOutput = new StringWriter();

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(EvaluationResult.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			//jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-16");
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(res, xmlOutput);
			
			return xmlOutput.toString(); 
			
		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		}

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
