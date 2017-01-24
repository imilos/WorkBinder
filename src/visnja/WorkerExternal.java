package visnja;

import java.io.*;

import rs.ac.kg.pmf.pbfs.UtilPbfs;

import yu.ac.bg.rcub.binder.BinderCommunicationException;
import yu.ac.bg.rcub.binder.BinderUtil;
import yu.ac.bg.rcub.binder.handler.worker.WorkerConnector;
import yu.ac.bg.rcub.binder.handler.worker.WorkerHandler;

public class WorkerExternal implements WorkerHandler {

	private DataInputStream in;
	private DataOutputStream out;

	private BufferedReader ProcessInput;
	private BufferedWriter ProcessOutput;

	public void run(WorkerConnector workerConnector) {

		workerConnector.log("Started a new EXTERNAL worker handler thread!");

		try {
			in = new DataInputStream(workerConnector.getInputStream());
			out = new DataOutputStream(workerConnector.getOutputStream());

            // Prihvati optimizationUUID da bi se kreirao direktorijum pod tim imenom
            //String optimizationUUID = BinderUtil.readString(in);
            //String optimizationDirectory = System.getProperty("user.dir") + File.separator + optimizationUUID;

			// Worker prima niz
			double[] parameters = BinderUtil.readDoubles(in);

			//String[] commandArray = { "MojExe.exe" };
			//String[] commandArray = {"./run_exe.sh"};
            //String[] commandArray = { optimizationDirectory + File.separator + "run_exe.sh" };
			String[] commandArray = { "./run_exe.sh" };
            //new File(optimizationDirectory + File.separator + "run_exe.sh").setExecutable(true);

			ProcessBuilder pb = new ProcessBuilder(commandArray);
			//pb.directory(new File(optimizationDirectory));
			pb.redirectErrorStream(true);

			try {
				final Process pr = pb.start();
				workerConnector.log("Started exe");
				// slanje niza doublova .exe-u preko std input-a
				// prvo se salje broj parametara
				// a onda jedan po jedan parametar
				ProcessOutput = new BufferedWriter(new OutputStreamWriter(pr.getOutputStream()));
				ProcessOutput.write(Integer.toString(parameters.length));
				ProcessOutput.newLine();
				for (int i = 0; i < parameters.length; i++) {
					ProcessOutput.write(Double.toString(parameters[i]));
					ProcessOutput.newLine();
				}

				ProcessOutput.flush();
				ProcessOutput.close();

				// preuzimanje izlaza iz .exe sa std out i prosledjivanje
				// klijentu
				ProcessInput = new BufferedReader(new InputStreamReader(
						pr.getInputStream()));
				String s = ProcessInput.readLine();
				workerConnector.log("Primio sam " + s);
				// ako .exe nije digao exception
				if (s.equalsIgnoreCase("SUCCESS")) {

					BinderUtil.writeString(out, "SUCCESS");
					// prvo primi broj rezultata koje ce ocitati
					int len = Integer.parseInt(ProcessInput.readLine());
					double[] results = new double[len];
					// prima jedan po jedan rezultat
					for (int i = 0; i < len; i++) {
						results[i] = Double.parseDouble(ProcessInput.readLine());
					}
					BinderUtil.writeDoubles(out, results);
				} else {
					workerConnector.log("Greska je " + s);
					BinderUtil.writeString(out, s);
				}

				// BinderUtil.writeString(out,ProcessInput.readLine());

				pr.waitFor();
				ProcessInput.close();
				pr.destroy();
				workerConnector.log("Finished exe");
				BinderUtil.writeString(out, "-finished-");
			} catch (IOException e) {
				workerConnector.log("MojExe nije startovao kako treba>>>> "
						+ e.getMessage());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		} catch (FileNotFoundException e) {
			workerConnector.log(
					"ServerDispatcherThread:   *** ERROR *** File not found ",
					e);
		} catch (IOException e) {
			workerConnector
					.log("ServerDispatcherThread:   *** ERROR *** IO Error occured while binder communicated with the client!!!",
							e);
		} catch (Exception e) {
			workerConnector.log("*** ERROR ***   Unknown error occured.", e);
		}
		workerConnector.log("EXTERNAL worker handler end.");
	}
}
