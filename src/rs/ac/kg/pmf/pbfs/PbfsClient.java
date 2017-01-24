package rs.ac.kg.pmf.pbfs;

import java.io.*;
import java.util.Properties;

import org.apache.log4j.Logger;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.CEInfo;
import yu.ac.bg.rcub.binder.handler.client.ClientConnector;
import yu.ac.bg.rcub.binder.handler.client.ClientConnectorFactory;

public class PbfsClient extends Thread {

	private Properties properties = null;
	private ClientConnector clientConn = null;
	private final long waitTime;
	private DataInputStream in;
	private DataOutputStream out;
	Logger logger = Logger.getLogger(PbfsClient.class);

	public PbfsClient() {
		super("TestClient");
		this.waitTime = 1000;
		init("pbfs.properties");
	}

	public PbfsClient(String propFilePath) {
		super("TestClient");
		this.waitTime = 1000;
		init(propFilePath);
	}

	private void init(String propFilePath) {
		/* Read client properties from the file. */
		properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(propFilePath)));
		} catch (FileNotFoundException e1) {
			logger.error("PBFS properties file not found!", e1);
			System.exit(1);
		} catch (IOException e2) {
			logger.error("Error while reading PBFS properties file!", e2);
			System.exit(1);
		}
	}

	private void communicate() throws BinderCommunicationException, InterruptedException {

		Thread.sleep(waitTime);

		try {
			in = new DataInputStream(clientConn.getInputStream());
			out = new DataOutputStream(clientConn.getOutputStream());
			System.out.println("Before client starts sending data");
	  
			String example_name = properties.getProperty("InputFileName");
			
			BinderUtil.writeString(out, example_name);
			BinderUtil.readString(in);
			// Prefix za izvrsavanje preko wine ili native
			BinderUtil.writeString(out, properties.getProperty("PrefixToExecutable"));
			// Prebacivanje relevantnih fajlova na server
			UtilPbfs.sendFile(example_name + ".dat", in, out);
			UtilPbfs.sendFile(properties.getProperty("ExecutableFileName"), in, out);
			
			// Ispis teksta dobijenog od servera na stdout klijenta
			String line = BinderUtil.readString(in);
			
			while ( !line.equalsIgnoreCase("-finished-") ) {
				System.out.println(line);
				BinderUtil.writeString(out, "-continue-" ); 
				line = BinderUtil.readString(in);
			}
			
			// Prijem zip fajla sa rezultatima i raspakovanje
			UtilPbfs.receiveFile(example_name + ".zip", in, out);
			UtilPbfs.unZIP(example_name + ".zip");
			
			// Brisanje zip fajla i izbacivanje postfiksa "_1"
			(new File(example_name + ".zip")).delete();
			(new File(example_name + ".lst")).delete();
			(new File(example_name + ".unv")).delete();
		    (new File(example_name+"_remote.lst")).renameTo(new File(example_name + ".lst"));
		    (new File(example_name+"_remote.unv")).renameTo(new File(example_name + ".unv"));
			
			// Saopstenje serveru da je posao zavrsen
			BinderUtil.writeString(out, "enough");
			System.out.println("End of client");
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
		logger.info("Starting PBFS client...");
		try {
			clientConn = ClientConnectorFactory.createClientConnector(properties);
			testCEs();
			clientConn.connect();
			/* actual client communication */
			communicate();
			/* client finished */
			logger.info("End of PBFS client, disconnecting from binder...");
			clientConn.disconnect();
		} catch (BinderCommunicationException be) {
			logger.error("Communication with the binder failed.", be);
		} catch (Exception e) {
			logger.error("Something's wrong!", e);
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			new PbfsClient("pbfs.properties").start();
		} else if (args.length == 1) {
			new PbfsClient(args[0]).start();
		} else {
			System.out.println("Usage: PbfsClient [filepath]");
			System.out.println(" - [filepath] is an optional properties file name (pbfs.properties is default)");
		}
	}
}
