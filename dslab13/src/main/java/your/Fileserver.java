package your;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.HashSet;
import java.util.Set;

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
import cli.Command;
import cli.Shell;
import server.IFileServer;
import server.IFileServerCli;
import util.ChecksumUtils;
import util.Config;

public class Fileserver implements Runnable,IFileServerCli{

	Config filesConfig;
	Shell filesShell;
	Thread shellThread;
	Thread isAliveSending;
	DatagramSocket datagramSocket;
	ServerSocket serverSocket;
	Set<String> fileList;
	AliveSender packetSender;

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
		fileList = new HashSet<String>();

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
					}else if(request.getClass()==InfoRequest.class){
						outputs.writeObject(info((InfoRequest) request));
						clientSocket.close();
						break;
					}else if(request.getClass()==VersionRequest.class){
						outputs.writeObject(version((VersionRequest) request));
						clientSocket.close();
						break;
					}else if(request.getClass()==UploadRequest.class){
						outputs.writeObject(upload((UploadRequest) request));
						clientSocket.close();
						break;
					}else if(request.getClass()==ListRequest.class){
						outputs.writeObject(list());
						clientSocket.close();
						break;
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
				fileList.add(listfile.getName());
			}
			return new ListResponse(fileList);
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
			return new VersionResponse(request.getFilename(),1);
		}
		/**
		 * @see server.IFileServer#upload(message.request.UploadRequest)
		 */
		@Override
		public MessageResponse upload(UploadRequest request) throws IOException {
			System.out.println("Received UploadRequest from Proxy.");
			System.out.println("Uploading the following File: "+request.getFilename());

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
					buf = filesConfig.getString("tcp.port").getBytes();
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

