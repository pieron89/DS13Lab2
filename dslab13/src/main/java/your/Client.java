package your;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.Base64;

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
import message.request.LoginChallengeRequest;
import message.request.LogoutRequest;
import message.request.UploadRequest;
import message.response.BuyResponse;
import message.response.CreditsResponse;
import message.response.DownloadFileResponse;
import message.response.DownloadTicketResponse;
import message.response.ListResponse;
import message.response.LoginResponse;
import message.response.MessageResponse;
import message.response.OkResponse;
import message.response.LoginResponse.Type;

public class Client implements Runnable,IClientCli{

	//config and shell for client
	Config clientConfig;
	Config userConfig;
	Config mcConfig;
	Shell clientShell;
	Thread shellThread;
	Thread cliproxThread;
	//client-proxy socket and streams	
	//	Socket proxySocket;
	//	ObjectInputStream proxyInstream;
	//	ObjectOutputStream proxyOutstream;

	TCPChannel proxyChannel;
	RSAChannel RSA64ProxyChannel;
	AESChannel AES64ProxyChannel;
	//client-fileserver socket and streams
	Socket fileserverSocket;
	ObjectInputStream fileserverInstream;
	ObjectOutputStream fileserverOutstream;

	//current UserPrivateKeyPath
	String userPrivateKeyPath=null;
	String username;

	//Registry
	Registry registry;
	IRemote proxyremote;


	public Client(Config clientConfig, Shell clientShell) {
		this.clientConfig=clientConfig;
		this.clientShell=clientShell;
		userConfig = new Config("user");
		mcConfig = new Config("mc");
		this.clientShell.register(this); //register methodes marked with Command
		shellThread = new Thread(clientShell);

	}

	@Override
	public void run() {
		//shellThread.start();
		try {
			//			this.proxySocket = new Socket(clientConfig.getString("proxy.host"), clientConfig.getInt("proxy.tcp.port"));
			//			this.proxyOutstream = new ObjectOutputStream(proxySocket.getOutputStream());
			//			this.proxyInstream = new ObjectInputStream(proxySocket.getInputStream());
			//			shellThread.start();
			//
			//			System.out.println("Connection to Proxy established.");


			registry = LocateRegistry.getRegistry(mcConfig.getString("proxy.host"), mcConfig.getInt("proxy.rmi.port"));
			proxyremote = (IRemote) registry.lookup("proxyremote");
			proxyChannel = new TCPChannel(new Socket(clientConfig.getString("proxy.host"), clientConfig.getInt("proxy.tcp.port")));
			RSA64ProxyChannel = new RSAChannel(new Base64Channel(proxyChannel));
			AES64ProxyChannel = new AESChannel(new Base64Channel(proxyChannel));
			shellThread.start();

		} catch(ConnectException e){
			System.out.println("Proxy offline, restart Client.");
		}catch (IOException e) {
			//System.out.println("Lost Connection to Proxy");
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @see client.IClientCli#login(java.lang.String, java.lang.String)
	 */
	@Override
	public LoginResponse login(String username, String password) throws IOException {
		return null;
	}

	@Command
	public LoginResponse login(String username) throws IOException {

		Object responseob = null;
		this.username = username;

		SecureRandom random = new SecureRandom();
		byte[] clientChallenge = new byte[32];
		random.nextBytes(clientChallenge);
		clientChallenge = Base64.encode(clientChallenge);
		OkResponse okres = null;
		File privateKeyPathFile = new File(clientConfig.getString("keys.dir")+"/"+username+".pem");

		try {
			//proxyOutstream.writeObject(new LoginRequest(username, password));
			//RSA64ProxyChannel.send(serialize(new LoginRequest(username, password)));
			if(userPrivateKeyPath!=null){
				System.out.println("Please log out first!");
				return new LoginResponse(Type.WRONG_CREDENTIALS);
			}
			if(privateKeyPathFile.exists()){
				System.out.println("Userkeyfile exists!");
				RSA64ProxyChannel.setPrivateKey(privateKeyPathFile.getAbsolutePath(), userConfig.getString(username+".password"));
				RSA64ProxyChannel.setPublicKey(clientConfig.getString("proxy.key"));	
				RSA64ProxyChannel.send(serialize(new LoginChallengeRequest(username, clientChallenge)));
				responseob = deserialize(RSA64ProxyChannel.receive());
				if(responseob instanceof OkResponse){
					System.out.println("Answer is Instance of OkResponse!");
					okres = (OkResponse) responseob;
					System.out.println(Base64.decode(okres.getClientChallenge()));
					System.out.println(clientChallenge);
					if(Arrays.equals(Base64.decode(okres.getClientChallenge()),(clientChallenge))){
						System.out.println("ClientChallenge check passed!");
						//						byte[] proxyChallenge = new byte[32];
						//						proxyChallenge = okres.getProxyChallenge();
						//						proxyChallenge = 
						AES64ProxyChannel.setAESSecretKey(new SecretKeySpec(Base64.decode(okres.getSecretKey()), 0, 
								Base64.decode(okres.getSecretKey()).length, "AES"));
						AES64ProxyChannel.setAESiv(Base64.decode(okres.getIv()));
						System.out.println("sending");
						AES64ProxyChannel.send(okres.getProxyChallenge());
						System.out.println("receiving");
						Object successResponse = deserialize(AES64ProxyChannel.receive());
						System.out.println("received");
						if(successResponse instanceof LoginResponse){
							LoginResponse loginResponse = (LoginResponse) successResponse;
							System.out.println("SuccessResponse received!");
							System.out.println(loginResponse.getType());
							if(loginResponse.getType()==Type.SUCCESS){
								System.out.println("Secure connection established!");
								userPrivateKeyPath = privateKeyPathFile.getAbsolutePath();
								return (LoginResponse) successResponse;
							}
							System.out.println("Failure to establish secure Connection!");
						}
						System.out.println("SuccessResponse failure!");
					}
					System.out.println("ClientChallenge check failed!");
				}
				System.out.println("Answer is no Instance of OkResponse!");
			}
			System.out.println("Userkeyfile doesnt exist!");
			//			if(loginresponse.getClass()==MessageResponse.class){
			//				System.out.println(loginresponse);
			//				return null;
			//			}



		} catch (NullPointerException e) {
			e.printStackTrace();
			System.out.println("Proxy offline, restart Client.");
			exit();
		} catch (SocketException e){
			System.out.println("Proxy offline, restart Client.");
			exit();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		userPrivateKeyPath=null;
		System.out.println("Error 37: Something went wrong!");
		return new LoginResponse(Type.WRONG_CREDENTIALS);
	}
	/**
	 * @see client.IClientCli#credits()
	 */
	@Override
	@Command
	public Response credits() throws IOException {

		if(userLoggedIn()){
			CreditsResponse creditsresponse = null;
			try {
				AES64ProxyChannel.send(serialize(new CreditsRequest()));
				//	proxyOutstream.writeObject(new CreditsRequest());
				//	creditsresponse = (CreditsResponse) proxyInstream.readObject();

				creditsresponse = (CreditsResponse) deserialize(AES64ProxyChannel.receive());
				System.out.println(creditsresponse);
			} catch (NullPointerException e) {
				System.out.println("Proxy offline, restart Client.");
				exit();
			} catch (SocketException e){
				System.out.println("Proxy offline, restart Client.");
				exit();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return creditsresponse;
		}else{
			MessageResponse response = new MessageResponse("Failure");
			return response;
		}

	}
	/**
	 * @see client.IClientCli#buy(long)
	 */
	@Override
	@Command
	public Response buy(long credits) throws IOException {

		if(userLoggedIn()){
			BuyResponse buyresponse = null;
			try {
				//			proxyOutstream.writeObject(new BuyRequest(credits));
				//			buyresponse = (BuyResponse) proxyInstream.readObject();
				AES64ProxyChannel.send(serialize(new BuyRequest(credits)));
				buyresponse = (BuyResponse) deserialize(AES64ProxyChannel.receive());			
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
		}else{
			MessageResponse response = new MessageResponse("Failure");
			return response;
		}

	}
	/**
	 * @see client.IClientCli#list()
	 */
	@Override
	@Command
	public Response list() throws IOException {

		if(userLoggedIn()){
			ListResponse listresponse = null;
			try {
				//			proxyOutstream.writeObject(new ListRequest());
				//			Object response =  proxyInstream.readObject();

				AES64ProxyChannel.send(serialize(new ListRequest()));
				Object response = deserialize(AES64ProxyChannel.receive());
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
		}else{
			MessageResponse response = new MessageResponse("Failure");
			return response;
		}

	}
	/**
	 * @see client.IClientCli#download(java.lang.String)
	 */
	@Override
	@Command
	public Response download(String filename) throws IOException {

		if(userLoggedIn()){
			DownloadTicketResponse dtr = null;
			DownloadFileResponse dfr = null;
			try {
				//			proxyOutstream.writeObject(new DownloadTicketRequest(filename));
				//			Object response = proxyInstream.readObject();

				AES64ProxyChannel.send(serialize(new DownloadTicketRequest(filename)));
				Object response = deserialize(AES64ProxyChannel.receive());
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
				System.out.println("File not Found.");
//				exit();
			} catch (SocketException e){
				System.out.println("Proxy offline, restart Client.");
				exit();
			}
			//return new MessageResponse("File successfully downloaded.");
			return dfr;
		}else{
			MessageResponse response = new MessageResponse("Failure");
			return response;
		}

	}
	/**
	 * @see client.IClientCli#upload(java.lang.String)
	 */
	@Override
	@Command
	public MessageResponse upload(String filename) throws IOException {

		if(userLoggedIn()){
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
					//			proxyOutstream.writeObject(new UploadRequest(filename, 1, (stringbuilder.toString()).getBytes()));
					//			uploadresponse = (MessageResponse) proxyInstream.readObject();

					AES64ProxyChannel.send(serialize(new UploadRequest(filename, 1, (stringbuilder.toString()).getBytes())));
					uploadresponse = (MessageResponse) deserialize(AES64ProxyChannel.receive());
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
		}else{
			MessageResponse response = new MessageResponse("Failure");
			return response;
		}
	}
	/**
	 * @see client.IClientCli#logout()
	 */
	@Override
	@Command
	public MessageResponse logout() throws IOException {

		if(userLoggedIn()){
			MessageResponse logoutresponse = null;
			try {
				//			proxyOutstream.writeObject(new LogoutRequest());
				//			logoutresponse = (MessageResponse) proxyInstream.readObject();

				AES64ProxyChannel.send(serialize(new LogoutRequest()));
				logoutresponse = (MessageResponse) deserialize(AES64ProxyChannel.receive());
				userPrivateKeyPath = null;
				username = null;

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
		}else{
			MessageResponse response = new MessageResponse("Failure");
			return response;
		}
	}

	@Command
	public void readQuorum(){

		try {
			System.out.println("Read-Quorum is set to "+proxyremote.readQuorum()+".");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Command
	public void writeQuorum(){

		try {
			System.out.println("Write-Quorum is set to "+proxyremote.writeQuorum()+".");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Command
	public void topThreeDownloads(){

		try {
			System.out.println(proxyremote.topThreeDownloads());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Command
	public void subscribe(String filename, int numberOfDownloads){

		try {
			proxyremote.subscribe(username, filename, numberOfDownloads, new Callback());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Command
	public void getProxyPublicKey(){
		String key = null;
		Writer writer = null;
		File proxypublickey = new File(clientConfig.getString("keys.dir")+"/proxy.pub.pem");
		try {
			key = proxyremote.getProxyPublicKey();
			if(proxypublickey.exists()){
				System.out.println("Deleting proxy.pub.pem because it already exists.");
				proxypublickey.delete();
			}
			writer = new FileWriter(proxypublickey);
			writer.write(new String(key));
		} catch (IOException e) {
			System.out.println("Could not write File: proxy.pub.pem");
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
				}
		}

	}

	@Command
	public void setUserPublicKey(String username){
		BufferedReader in;
		FileReader fr;
		try {
			fr = new FileReader(clientConfig.getString("keys.dir")+"/"+username+".pub.pem");
			in = new BufferedReader(fr);
			String line = null;
			StringBuilder stringbuilder = new StringBuilder();
			String lineseperator = System.getProperty("line.separator");
			while((line = in.readLine()) != null) {
				stringbuilder.append(line);
				stringbuilder.append(lineseperator);
			}
			in.close();
			fr.close();
			String publicKey = (String) stringbuilder.toString();
			proxyremote.setUserPublicKey(username, publicKey);
		} catch (FileNotFoundException e) {
			System.out.println("Could not find file: "+username+".pub.pem");
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not access file: "+username+".pub.pem");
			//e.printStackTrace();
		} 
	}

	/**
	 * @see client.IClientCli#exit()
	 */
	@Override
	@Command
	public MessageResponse exit() throws IOException {
		proxyChannel.close();
		System.in.close();
		return new MessageResponse("Client exited successfully.");
	}

	public byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}

	public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}

	public boolean userLoggedIn(){
		if(userPrivateKeyPath!=null){
			return true;
		}
		return false;
	}

}