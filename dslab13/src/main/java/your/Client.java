package your;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import util.Config;
import cli.Command;
import cli.Shell;
import client.IClientCli;
import message.Response;
import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.DownloadFileRequest;
import message.request.DownloadTicketRequest;
import message.request.ListRequest;
import message.request.LoginRequest;
import message.request.LogoutRequest;
import message.request.UploadRequest;
import message.response.BuyResponse;
import message.response.CreditsResponse;
import message.response.DownloadFileResponse;
import message.response.DownloadTicketResponse;
import message.response.ListResponse;
import message.response.LoginResponse;
import message.response.MessageResponse;

public class Client implements Runnable,IClientCli{

	//config and shell for client
	//und dis ist ein test
	Config clientConfig;
	Shell clientShell;
	Thread shellThread;
	Thread cliproxThread;
	//client-proxy socket and streams
	Socket proxySocket;
	ObjectInputStream proxyInstream;
	ObjectOutputStream proxyOutstream;
	//client-fileserver socket and streams
	Socket fileserverSocket;
	ObjectInputStream fileserverInstream;
	ObjectOutputStream fileserverOutstream;

	public Client(Config clientConfig, Shell clientShell) {
		this.clientConfig=clientConfig;
		this.clientShell=clientShell;
		this.clientShell.register(this); //register methodes marked with Command
		shellThread = new Thread(clientShell);
		
	}
	
	@Override
	public void run() {
		//shellThread.start();
		try {
			this.proxySocket = new Socket(clientConfig.getString("proxy.host"), clientConfig.getInt("proxy.tcp.port"));
			this.proxyOutstream = new ObjectOutputStream(proxySocket.getOutputStream());
			this.proxyInstream = new ObjectInputStream(proxySocket.getInputStream());
			shellThread.start();

			System.out.println("Connection to Proxy established.");

		} catch(ConnectException e){
			System.out.println("Proxy offline, restart Client.");
		}catch (IOException e) {
			//System.out.println("Lost Connection to Proxy");
		}
	}
	
	/**
	 * @see client.IClientCli#login(java.lang.String, java.lang.String)
	 */
	@Override
	@Command
	public LoginResponse login(String username, String password) throws IOException {
		
		Response loginresponse = null;
		try {
			proxyOutstream.writeObject(new LoginRequest(username, password));
			loginresponse = (Response) proxyInstream.readObject();
			if(loginresponse.getClass()==MessageResponse.class){
				System.out.println(loginresponse);
				return null;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.out.println("Proxy offline, restart Client.");
			exit();
		} catch (SocketException e){
			System.out.println("Proxy offline, restart Client.");
			exit();
		}
		return (LoginResponse) loginresponse;
	}
	/**
	 * @see client.IClientCli#credits()
	 */
	@Override
	@Command
	public Response credits() throws IOException {
		
		CreditsResponse creditsresponse = null;
		try {
			proxyOutstream.writeObject(new CreditsRequest());
			creditsresponse = (CreditsResponse) proxyInstream.readObject();
			System.out.println(creditsresponse);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.out.println("Proxy offline, restart Client.");
			exit();
		} catch (SocketException e){
			System.out.println("Proxy offline, restart Client.");
			exit();
		}
		return creditsresponse;
	}
	/**
	 * @see client.IClientCli#buy(long)
	 */
	@Override
	@Command
	public Response buy(long credits) throws IOException {
		
		BuyResponse buyresponse = null;
		try {
			proxyOutstream.writeObject(new BuyRequest(credits));
			buyresponse = (BuyResponse) proxyInstream.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.out.println("Proxy offline, restart Client.");
			exit();
		} catch (SocketException e){
			System.out.println("Proxy offline, restart Client.");
			exit();
		}
		return buyresponse;
	}
	/**
	 * @see client.IClientCli#list()
	 */
	@Override
	@Command
	public Response list() throws IOException {
		
		ListResponse listresponse = null;
		try {
			proxyOutstream.writeObject(new ListRequest());
			Object response =  proxyInstream.readObject();
			if(response.getClass()==MessageResponse.class){
				return (MessageResponse) response;
			}
			listresponse = (ListResponse) response;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.out.println("Proxy offline, restart Client.");
			exit();
		} catch (SocketException e){
			System.out.println("Proxy offline, restart Client.");
			exit();
		}
		return listresponse;
	}
	/**
	 * @see client.IClientCli#download(java.lang.String)
	 */
	@Override
	@Command
	public Response download(String filename) throws IOException {
		
		DownloadTicketResponse dtr = null;
		DownloadFileResponse dfr = null;
		try {
			proxyOutstream.writeObject(new DownloadTicketRequest(filename));
			Object response = proxyInstream.readObject();
			//Wenn kein downloadticket zurueckkommt
			if(response.getClass()==MessageResponse.class){
				return (MessageResponse) response;
			}
			dtr = (DownloadTicketResponse) response;
			System.out.println("Received Downloadticket from Proxy.");
			//Socket zum Fileserver1
			try {
				fileserverSocket = new Socket(dtr.getTicket().getAddress(), dtr.getTicket().getPort());
				fileserverOutstream = new ObjectOutputStream(fileserverSocket.getOutputStream());
				fileserverInstream = new ObjectInputStream(fileserverSocket.getInputStream());

				System.out.println("Client connected to Fileserver: "+dtr.getTicket().getPort());

				fileserverOutstream.writeObject(new DownloadFileRequest(dtr.getTicket()));
				Object downloadresponse = fileserverInstream.readObject();
				if(downloadresponse.getClass()==MessageResponse.class){
					return (MessageResponse) downloadresponse;
				}
				dfr = (DownloadFileResponse) downloadresponse;

				System.out.println("Received DownloadFileResponse from Fileserver.");

				Writer writer = null;
				File downloadfile = new File(clientConfig.getString("download.dir")+"/"+dfr.getTicket().getFilename());
				if(downloadfile.exists()){
					System.out.println("Deleting "+filename+" because it already exists.");
					downloadfile.delete();
				}
				try {
					writer = new FileWriter(downloadfile);
					writer.write(new String(dfr.getContent()));
				} catch (IOException e) {
					System.out.println("Could not write File: "+filename);
				} finally {
					if (writer != null)
						try {
							writer.close();
						} catch (IOException e) {
						}
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.out.println("Proxy offline, restart Client.");
			exit();
		} catch (SocketException e){
			System.out.println("Proxy offline, restart Client.");
			exit();
		}
		//return new MessageResponse("File successfully downloaded.");
		return dfr;
	}
	/**
	 * @see client.IClientCli#upload(java.lang.String)
	 */
	@Override
	@Command
	public MessageResponse upload(String filename) throws IOException {
		
		File uploadfile = new File(clientConfig.getString("download.dir")+"/"+filename);
		BufferedReader bufferedreader = new BufferedReader(new FileReader(uploadfile));
		String line = null;
		StringBuilder stringbuilder = new StringBuilder();
		String lineseperator = System.getProperty("line.separator");
		
		synchronized(stringbuilder){
		while((line = bufferedreader.readLine()) != null) {
			stringbuilder.append(line);
			stringbuilder.append(lineseperator);
		}
		bufferedreader.close();
		System.out.println("Uploading the following File: "+filename);
		MessageResponse uploadresponse = null;
		try {
			proxyOutstream.writeObject(new UploadRequest(filename, 1, (stringbuilder.toString()).getBytes()));
			uploadresponse = (MessageResponse) proxyInstream.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.out.println("Proxy offline, restart Client.");
			exit();
		} catch (SocketException e){
			System.out.println("Proxy offline, restart Client.");
			exit();
		}
		return uploadresponse;
		}
	}
	/**
	 * @see client.IClientCli#logout()
	 */
	@Override
	@Command
	public MessageResponse logout() throws IOException {
		
		MessageResponse logoutresponse = null;
		try {
			proxyOutstream.writeObject(new LogoutRequest());
			logoutresponse = (MessageResponse) proxyInstream.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.out.println("Proxy offline, restart Client.");
			exit();
		} catch (SocketException e){
			System.out.println("Proxy offline, restart Client.");
			exit();
		}
		return logoutresponse;
	}
	/**
	 * @see client.IClientCli#exit()
	 */
	@Override
	@Command
	public MessageResponse exit() throws IOException {
		
		proxySocket.close();
		System.in.close();
		return new MessageResponse("Client exited successfully.");
	}


}