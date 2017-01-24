package rs.ac.kg.pmf.R;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import rs.ac.kg.pmf.pbfs.UtilPbfs;
import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;
import yu.ac.bg.rcub.binder.handler.worker.WorkerHandler;

public class WorkerR implements WorkerHandler {

private DataInputStream in;
private DataOutputStream out;
	

	private BufferedReader ProcessInput;
	private BufferedWriter ProcessOutput;

	public void run(WorkerConnector workerConnector) {

		workerConnector.log("Started a new EXTERNAL worker handler thread!");
		
		try {
			in = new DataInputStream(workerConnector.getInputStream());
			out = new DataOutputStream(workerConnector.getOutputStream());			
						
			// Primi fajl sa slucajnim nazivom
			String pondersFileName = "/tmp/weights-" + UUID.randomUUID().toString();
			UtilPbfs.receiveFile(pondersFileName, in, out);
			
			String[] commandArray = {"./run_exe.sh", pondersFileName};
			
				ProcessBuilder pb = new ProcessBuilder(commandArray);
				pb.redirectErrorStream(true);
			
				try
				{					
					final Process pr = pb.start();
					workerConnector.log("Startovao R skript");
					
					// preuzimanje izlaza iz skripta sa stdout i prosledjivanje klijentu 
					ProcessInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
					String s = ProcessInput.readLine();
					workerConnector.log("Primio sam " + s);
					// Ako skript nije digao exception
					if (s.equalsIgnoreCase("OK")) {
						
						BinderUtil.writeString(out, "OK");
						// Prvo primi broj rezultata koji ce se ocitati
						int len = Integer.parseInt(ProcessInput.readLine().trim());
						workerConnector.log("Broj dablova " + len);
						//int len = 1;
						double[] results = new double[len];
						// prima jedan po jedan rezultat
						for (int i = 0; i<len; i++) {
							results[i] = Double.parseDouble(ProcessInput.readLine().trim());
						}
						BinderUtil.writeDoubles(out, results);

					} 
					else {
						workerConnector.log("Greska je " + s);
						BinderUtil.writeString(out, s);
					}
					
					pr.waitFor();
					ProcessInput.close();
					pr.destroy();
					workerConnector.log("Finished exe");
					BinderUtil.writeString( out, "-finished-" );
					
					// Obrisi fajlove sa ponderima
					Files.deleteIfExists(Paths.get(pondersFileName));
					Files.deleteIfExists(Paths.get(pondersFileName+"-1"));
				}
				catch (IOException e)
				{
					workerConnector.log("MojExe nije startovao kako treba>>>> " + e.getMessage());
				}
				catch(InterruptedException e)
				{
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
}	

