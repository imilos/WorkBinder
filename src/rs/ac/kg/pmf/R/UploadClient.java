package rs.ac.kg.pmf.R;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import rs.ac.kg.pmf.pbfs.UtilPbfs;
import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.CEInfo;
import yu.ac.bg.rcub.binder.handler.client.ClientConnector;
import yu.ac.bg.rcub.binder.handler.client.ClientConnectorFactory;

public class UploadClient extends Thread {

	private Properties properties = null;
	private ClientConnector clientConn = null;
	private final long waitTime;
	private DataInputStream in;
	private DataOutputStream out;
	Logger logger = Logger.getLogger(ClientR.class);
	private String ulazni_fajl;

	public UploadClient(String[] args) {
		super("TestClient");
		this.waitTime = 100;
		ulazni_fajl = args[0];
		init("UploadClient.properties");
	}

	
	private void init(String propFilePath) {
		/* Read client properties from the file. */
		properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(propFilePath)));
		} catch (FileNotFoundException e1) {
			logger.error("UPLOAD properties file not found!", e1);
			System.exit(1);
		} catch (IOException e2) {
			logger.error("Error while reading UPLOAD properties file!", e2);
			System.exit(1);
		}
	}

	private void communicate() throws BinderCommunicationException, InterruptedException {

		Thread.sleep(waitTime);

		try {
			in = new DataInputStream(clientConn.getInputStream());
			out = new DataOutputStream(clientConn.getOutputStream());

    		// Milos, Januar 2017.
            // Poslati prvo Optimization_UUID da bi se na serveru napravio odgovarajuci direktorijum
			BinderUtil.writeString(out, properties.getProperty("OptimizationUUID"));

			// Posalji prvo ime fajla
			File f = new File(ulazni_fajl);
			BinderUtil.writeString(out, f.getName());

			// A onda sam fajl
			UtilPbfs.sendFile(ulazni_fajl, in, out);
			
			// Ispis teksta dobijenog od servera na stdout klijenta			
			String poruka = BinderUtil.readString(in);
			System.out.println(poruka);
		}
		catch (FileNotFoundException e) {		
			logger.error("File not found " + e.getMessage(), e );
		} catch (IOException e) {
			logger.error("Input/output error", e);
		}
	}

	private void testCEs() throws BinderCommunicationException {
		CEInfo[] ceInfos = clientConn.executeCEListMatch();
		int size = ceInfos.length;
		System.out.println("CE list match report by the binder, total of " + size + " CEs matched.");
		for (CEInfo ceInfo : ceInfos)
			System.out.println(ceInfo);
	}

	public void run() {
		logger.info("Starting UPLOAD client...");
		try {
			clientConn = ClientConnectorFactory.createClientConnector(properties);
			testCEs();
			clientConn.connect();
			/* actual client communication */
			communicate();
			/* client finished */
			logger.info("End of UPLOAD client, disconnecting from binder...");
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
		new UploadClient(args).start();
	}
}

