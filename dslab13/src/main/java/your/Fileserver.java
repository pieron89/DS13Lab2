package your;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import message.Response;
import message.request.DownloadFileRequest;
import message.request.InfoRequest;
import message.request.ListRequest;
import message.request.UploadRequest;
import message.request.VersionRequest;
import message.response.DownloadFileResponse;
import message.response.InfoResponse;
import message.response.ListResponse;
import message.response.MessageResponse;
import message.response.VersionResponse;
import server.IFileServer;
import server.IFileServerCli;
import util.ChecksumUtils;
import util.Config;
import cli.Command;
import cli.Shell;

public class Fileserver implements Runnable,IFileServerCli{

	Config filesConfig;
	Shell filesShell;
	Thread shellThread;
	Thread isAliveSending;
	DatagramSocket datagramSocket;
	ServerSocket serverSocket;
	HashMap<String, Integer> fileList;
	AliveSender packetSender;
	Key secretSharedKey;

	public Fileserver(Config filesConfig, Shell filesShell){
		this.filesConfig = filesConfig;
		this.filesShell = filesShell;
	}

	@Override
	public void run() {
		filesShell.register(this);
		shellThread = new Thread(filesShell);
		shellThread.start();
		packetSender = new AliveSender();
		isAliveSending = new Thread(packetSender);
		isAliveSending.start();
		fileList = new HashMap<String, Integer>();

		byte[] keyBytes = new byte[1024];
		FileInputStream fis;
		try {
			fis = new FileInputStream(filesConfig.getString("hmac.key"));
			fis.read(keyBytes);
			fis.close();
			byte[] input = Hex.decode(keyBytes);
			// make sure to use the right ALGORITHM for what you want to do 
			// (see text) 
			secretSharedKey = new SecretKeySpec(input,"HmacSHA256");
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			serverSocket = new ServerSocket(filesConfig.getInt("tcp.port"));
			while(true){
				Socket clientSocket = serverSocket.accept();
				System.out.println("ListenerService: client connected...");
				clientDownloader climanage = new clientDownloader(clientSocket);
				Thread newclithread = new Thread(climanage);
				newclithread.start();
				System.out.println("ProxyThread erstellt");
			}
		} catch (SocketException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private byte[] computeHash(String tostring){
		// make sure to use the right ALGORITHM for what you want to do 
		// (see text) 
		Mac hMac;
		byte[] hash = null;
		try {
			hMac = Mac.getInstance("HmacSHA256");
			hMac.init(secretSharedKey);
			// MESSAGE is the message to sign in bytes 
			hMac.update(tostring.getBytes());
			hash = Base64.encode(hMac.doFinal());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		return hash;
	}

	private boolean validateHash(byte[] computedHash, byte[] receivedHash){
		return MessageDigest.isEqual(computedHash,receivedHash);
	}

	public class clientDownloader implements Runnable,IFileServer{
		private final Socket clientSocket;
		private ObjectInputStream inputs;
		private ObjectOutputStream outputs;
		Object request;

		public clientDownloader(Socket clientSocket){
			this.clientSocket = clientSocket;
			try {
				outputs = new ObjectOutputStream(clientSocket.getOutputStream());
				inputs = new ObjectInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}


		}
		@Override
		public void run() {
			while(true){
				try {
					request = inputs.readObject();

					if(request.getClass()==DownloadFileRequest.class){
						DownloadFileRequest dfr = (DownloadFileRequest) request;
						if(ChecksumUtils.verifyChecksum(dfr.getTicket().getUsername(), new File(filesConfig.getString("fileserver.dir")+"/"+dfr.getTicket().getFilename()), 1, dfr.getTicket().getChecksum())){
							outputs.writeObject(download(dfr));
						}else{
							outputs.writeObject(new MessageResponse("Invalid Ticket."));
						}
						clientSocket.close();
						break;
					}else if(request.getClass()==HashPlusObjectRequest.class){
						HashPlusObjectRequest hrequest = (HashPlusObjectRequest) request;
						if(hrequest.getRequest().getClass()==InfoRequest.class){
							InfoRequest info = (InfoRequest) hrequest.getRequest();
							HashPlusObjectResponse sendresponse;
							InfoResponse inforp = (InfoResponse) info(info);
							byte[] temphash = computeHash(inforp.toString());
							sendresponse = new HashPlusObjectResponse(temphash, inforp);
							outputs.writeObject(sendresponse);
							clientSocket.close();
							break;
						}else if(hrequest.getRequest().getClass()==VersionRequest.class){
							VersionRequest version = (VersionRequest) hrequest.getRequest();
							HashPlusObjectResponse sendresponse;
							VersionResponse versionrp = (VersionResponse) version(version);
							byte[] temphash = computeHash(versionrp.toString());
							sendresponse = new HashPlusObjectResponse(temphash, versionrp);
							outputs.writeObject(sendresponse);
							clientSocket.close();
							break;
						}else if(hrequest.getRequest().getClass()==UploadRequest.class){
							UploadRequest upload = (UploadRequest) hrequest.getRequest();
							HashPlusObjectResponse sendresponse;
							MessageResponse uploadrp = (MessageResponse) upload(upload);
							byte[] temphash = computeHash(uploadrp.toString());
							sendresponse = new HashPlusObjectResponse(temphash, uploadrp);
							outputs.writeObject(sendresponse);
							clientSocket.close();
							break;
						}else if(hrequest.getRequest().getClass()==ListRequest.class){
							HashPlusObjectResponse sendresponse;
							ListResponse listrp = (ListResponse) list();
							byte[] temphash = computeHash(listrp.toString());
							sendresponse = new HashPlusObjectResponse(temphash, listrp);
							outputs.writeObject(sendresponse);
							clientSocket.close();
							break;
						}
					}else{
						HashPlusObjectResponse sendresponse;
						MessageResponse mesrp = new MessageResponse("Fehler beim verifizieren des Hashes");
						byte[] temphash = computeHash(mesrp.toString());
						sendresponse = new HashPlusObjectResponse(temphash, mesrp);
						outputs.writeObject(sendresponse);
						clientSocket.close();
					}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	/**
	 * @see server.IFileServer#list()
	 */
	@Override
	public Response list() throws IOException {
		System.out.println("Received ListRequest from Proxy.");

		synchronized(fileList){
			File[] filearray = new File(filesConfig.getString("fileserver.dir")).listFiles();
			for(File listfile : filearray){
				fileList.put(listfile.getName(), 0);
			}
			return new ListResponse(fileList.keySet());
		}
	}
	/**
	 * @see server.IFileServer#download(message.request.DownloadFileRequest)
	 */
	@Override
	public Response download(DownloadFileRequest request) throws IOException {
		System.out.println("Received DownloadFileRequest from Client: "+request.getTicket().getUsername());
		BufferedReader bufferedreader = null;
		File downloadfile = new File(filesConfig.getString("fileserver.dir")+"/"+request.getTicket().getFilename());
		bufferedreader = new BufferedReader(new FileReader(downloadfile));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String lineseperator = System.getProperty("line.separator");

		while((line = bufferedreader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(lineseperator);
		}
		bufferedreader.close();
		return new DownloadFileResponse(request.getTicket(), (stringBuilder.toString()).getBytes());
	}
	/**
	 * @see server.IFileServer#info(message.request.InfoRequest)
	 */
	@Override
	public Response info(InfoRequest request) throws IOException {
		System.out.println("Received InfoRequest from Proxy.");
		File infofile = new File(filesConfig.getString("fileserver.dir")+"/"+request.getFilename());
		return new InfoResponse(request.getFilename(),infofile.length());
	}
	/**
	 * @see server.IFileServer#version(message.request.VersionRequest)
	 */
	@Override
	public Response version(VersionRequest request) throws IOException {
		System.out.println("Received VersionRequest from Proxy.");
		if(fileList.isEmpty()){
			list();
		}
		if(fileList.containsKey(request.getFilename())){
			return new VersionResponse(request.getFilename(), fileList.get(request.getFilename()));	
		}
		return new MessageResponse("File does not exist on the server.");

	}
	/**
	 * @see server.IFileServer#upload(message.request.UploadRequest)
	 */
	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		System.out.println("Received UploadRequest from Proxy.");
		System.out.println("Uploading the following File: "+request.getFilename());

		if(fileList.containsKey(request.getFilename())){
			int version;
			String name;
			name = request.getFilename();
			version = fileList.get(name) + 1;
			fileList.remove(name);
			fileList.put(name, version);					
		} else {
			fileList.put(request.getFilename(), request.getVersion());
		}

		Writer writer = null;
		File uploadfile = new File(filesConfig.getString("fileserver.dir")+"/"+request.getFilename());
		if(uploadfile.exists()){
			System.out.println("Deleting "+request.getFilename()+" because it already exists.");
			uploadfile.delete();
		}
		try {
			writer = new FileWriter(uploadfile);
			writer.write(new String(request.getContent()));
		} catch (IOException e) {
			System.out.println("Could not write file.");
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
				}
		}
		return new MessageResponse("File successfully uploaded.");
	}

}

public class AliveSender implements Runnable{
	//sendet isAlive packets an proxy
	@Override
	public void run() {
		try {
			datagramSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		while(!datagramSocket.isClosed()){
			try {
				Thread.currentThread();
				Thread.sleep(filesConfig.getInt("fileserver.alive"));
				InetAddress address = InetAddress.getByName(filesConfig.getString("proxy.host"));
				byte[] buf = new byte[256];
				//buf = filesConfig.getString("tcp.port").getBytes();
				buf = new String("!alive "+filesConfig.getString("tcp.port")).getBytes();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, address, filesConfig.getInt("proxy.udp.port"));
				datagramSocket.send(packet);
			} catch (SocketException e) {
				//e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
/**
 * @see server.IFileServerCli#exit()
 */
@Override
@Command
public MessageResponse exit() throws IOException {

	datagramSocket.close();
	System.in.close();
	serverSocket.close();

	return new MessageResponse("Exit successful");
}
}

