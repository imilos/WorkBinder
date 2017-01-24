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

public class ClientExternal extends Thread {

	private Properties properties = null;
	private ClientConnector clientConn = null;
	private final long waitTime;
	private DataInputStream in;
	private DataOutputStream out;
	Logger logger = Logger.getLogger(ClientExternal.class);
	private double[] parameters;

	public ClientExternal(String[] args) {
		super("TestClient");
		this.waitTime = 1000;
		parameters = new double[args.length];
		for (int i = 0; i < args.length; i++) {
			parameters[i] = Double.parseDouble(args[i]);
		}
		init("External.properties");
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

	private void communicate() throws BinderCommunicationException,
			InterruptedException {

		Thread.sleep(waitTime);

		try {
			in = new DataInputStream(clientConn.getInputStream());
			out = new DataOutputStream(clientConn.getOutputStream());

			// Milos, Januar 2017
            // Poslati prvo Optimization_UUID da bi se na serveru napravio odgovarajuci direktorijum
            BinderUtil.writeString(out, properties.getProperty("OptimizationUUID"));

			BinderUtil.writeDoubles(out, parameters);


			// Ispis teksta dobijenog od servera na stdout klijenta
			String line = BinderUtil.readString(in);
			if (line.equalsIgnoreCase("SUCCESS")) {
				parameters = BinderUtil.readDoubles(in);

				System.out.print(";" + line + ";");
				for (int i = 0; i < parameters.length; i++) {
					System.out.print(Double.toString(parameters[i]) + ";");
				}
			} else {
				System.out.println(";" + line + ";" + "EXE_FAILED;x;");
			}

			line = BinderUtil.readString(in);
		} catch (FileNotFoundException e) {
			logger.error("File not found " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Input/output error", e);
		}
	}

	private void testCEs() throws BinderCommunicationException {
		CEInfo[] ceInfos = clientConn.executeCEListMatch();
		int size = ceInfos.length;
		System.out.println("CE list match report by the binder, total of "
				+ size + " CEs matched.");
		for (CEInfo ceInfo : ceInfos)
			System.out.println(ceInfo);
	}

	public void run() {
		logger.info("Starting External client...");
		try {
			clientConn = ClientConnectorFactory
					.createClientConnector(properties);
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
		new ClientExternal(args).start();
	}
}
