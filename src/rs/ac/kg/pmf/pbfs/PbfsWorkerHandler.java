package rs.ac.kg.pmf.pbfs;

import java.io.*;

import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;
import yu.ac.bg.rcub.binder.handler.worker.WorkerHandler;

public class PbfsWorkerHandler implements WorkerHandler {

	private String exampleName;
	private DataInputStream in;
	private DataOutputStream out;
	private static String pbfsRemote = "./pak_remote.exe";
	private Process pakProcess;
	private BufferedReader pakProcessInput;
	private BufferedWriter pakProcessOutput;

	public void run(WorkerConnector workerConnector) {

		workerConnector.log("Started a new PBFS worker handler thread!");
		
		try {
			in = new DataInputStream(workerConnector.getInputStream());
			out = new DataOutputStream(workerConnector.getOutputStream());			

			exampleName = BinderUtil.readString(in);
			workerConnector.log("Received: " + exampleName);
			BinderUtil.writeString(out, "-continue-");
			// Prihvatanje prefixa za izvrsavanje PBFS_command, wine ili native
			String exec_prefix = BinderUtil.readString(in);

			// Sve dok klijent ne posalje string "enough", tece komunikacija
			while ( !exampleName.equalsIgnoreCase("enough") ) {
				String line;
				
				// Prijem relevatnih fajlova
				UtilPbfs.receiveFile(exampleName + "_remote.dat", in, out);
				UtilPbfs.receiveFile(pbfsRemote, in, out);
				
				Runtime.getRuntime().exec("chmod a+x " + pbfsRemote);
				Thread.sleep(100);
				
				// Startovanje PBFS procesa sa odgovarajucim in i out tokovima
				pakProcess = Runtime.getRuntime().exec(exec_prefix + " " + pbfsRemote);
				pakProcessOutput = new BufferedWriter(new OutputStreamWriter(pakProcess.getOutputStream()));
				pakProcessInput = new BufferedReader(new InputStreamReader(pakProcess.getInputStream()));
				pakProcessOutput.write(exampleName+"_remote.dat\n\n\n\n\n\n");
				pakProcessOutput.flush();
				
				// Sve dok ima nesto da se procita kao izlaz procesa
				while ( (line = pakProcessInput.readLine()) != null ) {
					BinderUtil.writeString(out, line);
					BinderUtil.readString(in);
				}

				pakProcessInput.close();
				pakProcessOutput.close();
				pakProcess.destroy();
				
				BinderUtil.writeString( out, "\nPacking result into " + exampleName + ".zip..." );
				BinderUtil.readString(in);
				BinderUtil.writeString( out, "-finished-" );
				
				String[] filenames = new String[] { exampleName+"_remote.unv", exampleName+"_remote.lst" };
				UtilPbfs.createZIP(exampleName + "_remote.zip", filenames);

				// Slanje zip fajla sa rezultatima
				UtilPbfs.sendFile(exampleName + "_remote.zip", in, out);
				
				// Print log
				workerConnector.log("Executed PBFS example named: " + exampleName);
				
				// Read input again
				exampleName = BinderUtil.readString(in);
				workerConnector.log("Received: " + exampleName);
			} // while petlja od uslova "enough"
							
		} catch (FileNotFoundException e) {
			workerConnector.log("ServerDispatcherThread:   *** ERROR *** File not found ", e);			
		} catch (IOException e) {
			workerConnector.log("ServerDispatcherThread:   *** ERROR *** IO Error occured while binder communicated with the client!!!", e);			
		} catch (Exception e) {
			workerConnector.log("*** ERROR ***   Unknown error occured.", e);
		}
		workerConnector.log("PBFS worker handler end.");
	}
}
