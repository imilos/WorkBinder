package rs.ac.kg.pmf.R;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import rs.ac.kg.pmf.pbfs.UtilPbfs;
import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;
import yu.ac.bg.rcub.binder.handler.worker.WorkerHandler;

public class UploadWorker implements WorkerHandler {

private DataInputStream in;
private DataOutputStream out;


	public void run(WorkerConnector workerConnector) {

		workerConnector.log("Started a new UPLOAD worker handler thread!");
		
		try {
			in = new DataInputStream(workerConnector.getInputStream());
			out = new DataOutputStream(workerConnector.getOutputStream());

			// Prihvati optimizationUUID da bi se kreirao direktorijum pod tim imenom
            //String optimizationUUID = BinderUtil.readString(in);
            //String optimizationDirectory = System.getProperty("user.dir") + File.separator + optimizationUUID;
            //Files.createDirectories(Paths.get(optimizationDirectory));

			// Prvo prihvati naziv fajla
			String ime_fajla = BinderUtil.readString(in);
			
			// Prihvati fajl sa klijenta
			//UtilPbfs.receiveFile(optimizationDirectory + File.separator + ime_fajla, in, out);
			UtilPbfs.receiveFile( ime_fajla + "-uploaded", in, out);
			
			// Poruka o uspehu
			BinderUtil.writeString(out, "File " + ime_fajla + " successfully uploaded.");
						
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

