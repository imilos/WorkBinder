package rs.ac.kg.pmf.R;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
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
            String optimizationUUID = BinderUtil.readString(in);
            String optimizationDirectory = System.getProperty("user.dir") + File.separator + optimizationUUID;
            Files.createDirectories(Paths.get(optimizationDirectory));

			// Prvo prihvati naziv fajla
			String fileName = BinderUtil.readString(in);
			String fileFullPath = optimizationDirectory + File.separator + fileName;

			// Prihvati fajl sa klijenta i smesti ga u odgovarajuci direktorijum
			UtilPbfs.receiveFile(fileFullPath, in, out);

            // Ukoliko se radi o ZIP arhivi, raspakuj je u odgovarajuci direktorijum

            if (fileName.endsWith("zip") || fileName.endsWith("ZIP")) {
                ZipFile zipFile = new ZipFile(fileFullPath);
                zipFile.extractAll(optimizationDirectory);
            }

            // Poruka o uspehu
			BinderUtil.writeString(out, "File " + fileFullPath + " successfully uploaded.");


		} /*catch (ZipException e) {
            e.printStackTrace();
        } */catch (FileNotFoundException e) {
			workerConnector.log("ServerDispatcherThread:   *** ERROR *** File not found ", e);			
		} catch (IOException e) {
			workerConnector.log("ServerDispatcherThread:   *** ERROR *** IO Error occured while binder communicated with the client!!!", e);
		}
		catch (Exception e) {
			workerConnector.log("*** ERROR ***   Unknown error occured.", e);
		}
		workerConnector.log("EXTERNAL worker handler end.");
	}
}	

