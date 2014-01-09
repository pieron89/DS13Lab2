package your;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import your.Channel;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import proxy.IProxy;
import proxy.IProxyCli;
import message.response.BuyResponse;
import message.response.CreditsResponse;
import message.response.DownloadTicketResponse;
import message.response.FileServerInfoResponse;
import message.response.InfoResponse;
import message.response.ListResponse;
import message.response.LoginResponse.Type;
import message.response.OkResponse;
import message.response.UserInfoResponse;
import message.response.VersionResponse;
import message.Request;
import message.Response;
import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.DownloadTicketRequest;
import message.request.InfoRequest;
import message.request.ListRequest;
import message.request.LoginChallengeRequest;
import message.request.LoginRequest;
import message.request.LogoutRequest;
import message.request.UploadRequest;
import message.request.VersionRequest;
import message.response.LoginResponse;
import message.response.MessageResponse;
import model.DownloadTicket;
import model.FileServerInfo;
import model.UserInfo;
import util.ChecksumUtils;
import util.Config;
import cli.Command;
import cli.Shell;

public class Proxy implements IProxyCli, Runnable {
	Config proxyConfig;
	Config userConfig;
	Config mcConfig;
	Shell proxyShell;
	HashMap<String, UserInfo> userInfoList;
	HashMap<String, FileServerInfo> fileServerInfoList;
	HashMap<String, Long> isAliveAriveTimes;
	//stage4 part------------------
	HashMap<String, Integer> downloadlist;
	HashMap<String, ClientConnection> clientlist;
	//-----------------------------
	List<Socket> clientSocketList;
	List<Channel> clientChannelList;

	Thread shellThread;
	Thread isAliveThread;

	ServerSocket serverSocket;
	DatagramSocket datagramSocket;
	ResourceBundle userResource;
	
	private int qw = 0;
	
	Key secretSharedKey;

	public Proxy(Config proxyConfig, Shell proxyShell){
		this.proxyConfig = proxyConfig;
		this.proxyShell = proxyShell;
		this.proxyShell.register(this);
		mcConfig = new Config("mc");
	}


	@Override
	public void run() {
		shellThread = new Thread(proxyShell);
		shellThread.start();

		fileServerInfoList = new HashMap<String,FileServerInfo>();
		clientSocketList = new ArrayList<Socket>();
		clientChannelList = new ArrayList<Channel>();
		//stage4 part------------------
		downloadlist = new HashMap<String, Integer>();
		clientlist = new HashMap<String, ClientConnection>();
		
		byte[] keyBytes = new byte[1024];
		FileInputStream fis;
		try {
			fis = new FileInputStream(proxyConfig.getString("hmac.key"));
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
			Registry reg = LocateRegistry.createRegistry(mcConfig.getInt("proxy.rmi.port"));
			Remote bla = new ProxyRemote(this);
			reg.bind("proxyremote", bla);
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (AlreadyBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//-----------------------------
		isAliveThread = new Thread(new AliveReceiver());
		isAliveThread.start();

		userConfig = new Config("user");
		userResource = ResourceBundle.getBundle("user");
		userInfoList = new HashMap<String, UserInfo>();
		Set<String> userSet = userResource.keySet();
		for(String k : userSet){
			UserInfo userinfo = new UserInfo(k.substring(0,k.indexOf('.')),(long) Integer.parseInt(userResource.getString(k.substring(0,k.indexOf('.'))+".credits")),false);
			if(!userInfoList.containsKey(userinfo.getName())){
				userInfoList.put(userinfo.getName(), userinfo);
			}
		}
		try {
			serverSocket = new ServerSocket(proxyConfig.getInt("tcp.port"));
			while(true){
				synchronized(clientSocketList){
					Socket clientSocket = serverSocket.accept();

					System.out.println("ListenerService: client connected...");
					//clientSocketList.add(clientSocket);
					TCPChannel clientChannel = new TCPChannel(clientSocket);
					clientChannelList.add(clientChannel);
					//ClientConnection clientconnection = new ClientConnection(clientSocket);
					ClientConnection clientconnection = new ClientConnection(clientChannel);

					System.out.println("ClientThread erstellt");
					Thread newclithread = new Thread(clientconnection);

					newclithread.start();
				}
			} 

		} catch (SocketException se){
			System.out.println("serverSocket closed.");
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
	

	private Object requestToFileserver(FileServerInfo fsi, HashPlusObjectRequest request){
		//kurze tcp verbindung zum fileserver aufbauen um request zu verschicken und response zu erhalten
		Socket proxyclientSocket;
		Object response = null;
		System.out.println(fsi.toString());
		try {
			proxyclientSocket = new Socket(fsi.getAddress(), fsi.getPort());
			ObjectOutputStream outstreamPCS = new ObjectOutputStream(proxyclientSocket.getOutputStream());
			ObjectInputStream instreamPCS = new ObjectInputStream(proxyclientSocket.getInputStream());
			outstreamPCS.writeObject(request);
			response = instreamPCS.readObject();
			proxyclientSocket.close();
		} catch (ConnectException e){
			return new MessageResponse("Could not connect to Fileserver.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return response;
	}
	/**
	 * @see proxy.IProxyCli#fileservers()
	 */
	@Override
	@Command
	public Response fileservers() throws IOException {

		Set<String> keyset = fileServerInfoList.keySet();
		List<FileServerInfo> fsi = new ArrayList<FileServerInfo>();
		synchronized(fsi){
			for(String s: keyset){
				fsi.add(fileServerInfoList.get(s));
			}
			return new FileServerInfoResponse(fsi);
		}
	}
	/**
	 * @see proxy.IProxyCli#users()
	 */
	@Override
	@Command
	public Response users() throws IOException {

		Set<String> userSet = userInfoList.keySet();
		List<UserInfo> users = new ArrayList<UserInfo>();
		synchronized(users){
			for(String k : userSet)
				users.add(userInfoList.get(k));
			return new UserInfoResponse(users);
		}
	}
	/**
	 * @see proxy.IProxyCli#exit()
	 */
	@Override
	@Command
	public MessageResponse exit() throws IOException {

		try{

			System.in.close();
			datagramSocket.close();
			serverSocket.close();
			for(Socket socket : clientSocketList){
				socket.close();
			}

			return new MessageResponse("Proxy existed successfully.");
		}catch(Exception e){
			return new MessageResponse("Proxy existed successfully.");
		}
	}

	public class AliveReceiver implements Runnable{
		//isAlive packets erhalten und auf alter ueberpruefen
		Set<String> keyset;
		@Override
		public void run() {
			isAliveAriveTimes = new HashMap<String, Long>();
			//synchronized(fileServerInfoList){
				try {
					datagramSocket = new DatagramSocket(proxyConfig.getInt("udp.port"));
					datagramSocket.setSoTimeout(1200);
					byte[] buf = new byte[256];
					while(true){
						long curTime = Calendar.getInstance().getTimeInMillis();
						DatagramPacket packet = new DatagramPacket(buf, buf.length);
						try{
							datagramSocket.receive(packet);
							long packetrecieved = Calendar.getInstance().getTimeInMillis();
							String s = new String(packet.getData());
							//!isAlive 12345
							String port = s.substring(7,12);
							isAliveAriveTimes.put(port, packetrecieved);

							if(!fileServerInfoList.containsKey(port)){
								fileServerInfoList.put(port, new FileServerInfo(packet.getAddress(), Integer.parseInt(port), 0, true));
							}else if(fileServerInfoList.containsKey(port)&&fileServerInfoList.get(port).isOnline()==false){
								FileServerInfo temp;
								temp = new FileServerInfo(fileServerInfoList.get(port).getAddress(),
										fileServerInfoList.get(port).getPort(),
										fileServerInfoList.get(port).getUsage(),
										true);
								fileServerInfoList.put(port, temp);
							}

						}catch (SocketTimeoutException e) {
						}
						keyset = isAliveAriveTimes.keySet();

						for(String g:keyset){
							if(Math.abs((isAliveAriveTimes.get(g)- curTime )) > (proxyConfig.getInt("fileserver.checkPeriod")+100)){
								if(fileServerInfoList.get(g).isOnline()){
									FileServerInfo temp = new FileServerInfo(fileServerInfoList.get(g).getAddress(),
											fileServerInfoList.get(g).getPort(),
											fileServerInfoList.get(g).getUsage(),
											false);
									fileServerInfoList.put(g, temp);
								}
							} 
						}
					} 
				} catch (SocketException e) {
					System.out.println("datagramSocket closed.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			//}
		}		
	}

	public class ClientConnection implements IProxy, Runnable{
		//private final Socket clientSocket;
		//private ObjectInputStream inputs;
		//private ObjectOutputStream outputs;
		Object request;
		private String currentUser = null;
		private TCPChannel clientChannel;
		private RSAChannel RSA64TCPChannel;
		private AESChannel AES64TCPChannel;
		//stage4 part------------------
		private ICallback callback;
		private HashMap<String, Integer> subscribtions = new HashMap<String, Integer>();
		//-----------------------------
		
		//variables for gilford versioning, N = number of file servers
		//private int qw = 0;		//write quorum, (N/2 + 1)
		// private int qr = 0;		//read quorum, qr = qw because lazy
		
		public ClientConnection(TCPChannel clientChannel) {
			//this.clientSocket = clientSocket;
			this.clientChannel = clientChannel;
			RSA64TCPChannel = new RSAChannel(new Base64Channel(clientChannel));
			AES64TCPChannel = new AESChannel(new Base64Channel(clientChannel));
			RSA64TCPChannel.setPrivateKey(proxyConfig.getString("key"), "12345");
			//try {
			//	outputs = new ObjectOutputStream(clientSocket.getOutputStream());
			//	inputs = new ObjectInputStream(clientSocket.getInputStream());
			//} catch (IOException e) {
			//	e.printStackTrace();
			//}

		}

		@Override
		public void run() {
			try {    

				while(true){
					if(currentUser==null){
						request = deserialize(RSA64TCPChannel.receive());
						if(request.getClass()==LoginChallengeRequest.class){
							if(!userInfoList.get(((LoginChallengeRequest) request).getUsername()).isOnline()){
								AES64TCPChannel.send(serialize(login((LoginChallengeRequest) request)));
							}
						}
					}else{
						request = deserialize(AES64TCPChannel.receive());
						if(request.getClass()==BuyRequest.class){
							AES64TCPChannel.send(serialize(buy((BuyRequest) request)));
						}else if(request.getClass()==CreditsRequest.class){
							AES64TCPChannel.send(serialize(credits()));
						}else if(request.getClass()==ListRequest.class){
							AES64TCPChannel.send(serialize(list()));
						}else if(request.getClass()==DownloadTicketRequest.class){
							AES64TCPChannel.send(serialize(download((DownloadTicketRequest) request)));
						}else if(request.getClass()==UploadRequest.class){
							AES64TCPChannel.send(serialize(upload((UploadRequest) request)));
						}else if(request.getClass()==LogoutRequest.class){
							AES64TCPChannel.send(serialize(logout()));
						}else{
							AES64TCPChannel.send(serialize(new MessageResponse("You are already logged in.")));
						}
					}
					/*response = inputs.readObject();

					if(response.getClass()==LoginRequest.class){
						if(userInfoList.get(((LoginRequest) response).getUsername())==null){
							outputs.writeObject(new MessageResponse("User does not exist."));
						}else if(!userInfoList.get(((LoginRequest) response).getUsername()).isOnline()&&currentUser==null){
							outputs.writeObject(login((LoginRequest) response));
							//currentUser = ((LoginRequest) response).getUsername();
							//UserInfo userinfo = userInfoList.get(currentUser);
							//userInfoList.put(currentUser, new UserInfo(userinfo.getName(), userinfo.getCredits(), true));
						}else if(currentUser!=null){
							outputs.writeObject(new MessageResponse("You are already logged in."));
						}else if(userInfoList.get(((LoginRequest) response).getUsername()).isOnline()){
							outputs.writeObject(new MessageResponse("User already logged in on another Client."));
						}
					}else if(response.getClass()==BuyRequest.class){
						if(currentUser != null) outputs.writeObject(buy((BuyRequest) response));
						else outputs.writeObject(new MessageResponse("Please log in first."));
					}else if(response.getClass()==CreditsRequest.class){
						if(currentUser != null) outputs.writeObject(credits());
						else outputs.writeObject(new MessageResponse("Please log in first."));
					}else if(response.getClass()==ListRequest.class){
						if(currentUser != null) outputs.writeObject(list());
						else outputs.writeObject(new MessageResponse("Please log in first."));
					}else if(response.getClass()==DownloadTicketRequest.class){
						if(currentUser != null) outputs.writeObject(download((DownloadTicketRequest) response));
						else outputs.writeObject(new MessageResponse("Please log in first."));
					}else if(response.getClass()==UploadRequest.class){
						if(currentUser != null) outputs.writeObject(upload((UploadRequest) response));
						else outputs.writeObject(new MessageResponse("Please log in first."));
					}else if(response.getClass()==LogoutRequest.class){
						if(currentUser != null) outputs.writeObject(logout());
						else outputs.writeObject(new MessageResponse("Please log in first."));
					}*/
				}
//			}
//			catch (EOFException e){
//						
			}catch (IOException e) {
				try {
					if(currentUser!=null){
						logout();
					}
					clientChannel.close();
//					Thread.currentThread().interrupt();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				//e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.out.println("Message Object-Class not recognized.");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		/**
		 * @throws NoSuchAlgorithmException 
		 * @throws ClassNotFoundException 
		 * @see proxy.IProxy#login(message.request.LoginRequest)
		 */
		public LoginResponse login(LoginChallengeRequest request) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
			synchronized(userInfoList){
				RSA64TCPChannel.setPublicKey(proxyConfig.getString("keys.dir")+"/"+request.getUsername()+".pub.pem");
				// generates a 32 byte secure random number 
				SecureRandom secureRandom = new SecureRandom(); 
				final byte[] proxyChallenge = new byte[32]; 
				secureRandom.nextBytes(proxyChallenge);
				// generates a 16 byte secure random number
				SecureRandom secureRandom2 = new SecureRandom(); 
				final byte[] iv = new byte[16]; 
				secureRandom2.nextBytes(iv);
				byte[] ivBase64 = Base64.encode(iv);
				// generating a 256 bits secretkey
				KeyGenerator generator = KeyGenerator.getInstance("AES"); 
				generator.init(256); 
				SecretKey secretKey = generator.generateKey();
				byte[] secretKeyBase64 = Base64.encode(secretKey.getEncoded());
				//secretKey = new SecretKeySpec(secretKeyBase64, 0, secretKeyBase64.length, "AES");
				//sending 2nd message
				RSA64TCPChannel.send(serialize(new OkResponse(Base64.encode(request.getclientChallenge()), Base64.encode(proxyChallenge), secretKeyBase64, ivBase64)));
				AES64TCPChannel.setAESSecretKey(secretKey);
				AES64TCPChannel.setAESiv(iv);
				byte[] response = AES64TCPChannel.receive();
				System.out.println(new String(Base64.decode(response)));
				System.out.println(new String(proxyChallenge));
				if(Arrays.equals(Base64.decode(response), (proxyChallenge))){
					System.out.println("client challenge passed.");
					currentUser = request.getUsername();
					UserInfo userinfo = userInfoList.get(currentUser);
					userInfoList.put(currentUser, new UserInfo(userinfo.getName(), userinfo.getCredits(), true));
					//stage4 part------------------
					synchronized(clientlist){
						clientlist.put(currentUser, this);
					}
					//-----------------------------
					return new LoginResponse(Type.SUCCESS);
				}
			return new LoginResponse(Type.WRONG_CREDENTIALS);
			}
		}
		@Override
		public LoginResponse login(LoginRequest request) throws IOException {
			/*synchronized(userInfoList){
				System.out.println("Received Loginrequest: "+request.getUsername()+", "+request.getPassword());
				if(userConfig.getString(request.getUsername()+".password").equals(request.getPassword())){
					currentUser = ((LoginRequest) response).getUsername();
					UserInfo userinfo = userInfoList.get(currentUser);
					userInfoList.put(currentUser, new UserInfo(userinfo.getName(), userinfo.getCredits(), true));
					return new LoginResponse(Type.SUCCESS);
				}
				return new LoginResponse(Type.WRONG_CREDENTIALS);
			}*/
			return null;
		}
		/**
		 * @see proxy.IProxy#credits()
		 */
		@Override
		public Response credits() throws IOException {
			System.out.println("Received Creditsrequest from "+currentUser);
			return new CreditsResponse(userInfoList.get(currentUser).getCredits());
		}
		/**
		 * @see proxy.IProxy#buy(message.request.BuyRequest)
		 */
		@Override
		public Response buy(BuyRequest credits) throws IOException {
			synchronized(userInfoList){
				System.out.println("Received Buyrequest from "+currentUser);
				UserInfo userinfo = userInfoList.get(currentUser);
				userInfoList.put(currentUser, new UserInfo(userinfo.getName(), userinfo.getCredits()+credits.getCredits(), userinfo.isOnline()));
				return new BuyResponse(userinfo.getCredits()+credits.getCredits());
			}
		}
		/**
		 * @see proxy.IProxy#list()
		 */
		@Override
		public Response list() throws IOException {
			System.out.println("Listrequest received from "+currentUser);
			for(String s : fileServerInfoList.keySet()){
				if(fileServerInfoList.get(s).isOnline()){
					//ListResponse response = (ListResponse) requestToFileserver(fileServerInfoList.get(s),request);
					ListRequest request = new ListRequest();
					byte[] temphash = computeHash(request.toString());
					HashPlusObjectRequest sendrequest = new HashPlusObjectRequest(temphash, request);
					HashPlusObjectResponse sendresponse = (HashPlusObjectResponse) requestToFileserver(fileServerInfoList.get(s),sendrequest);
					if(validateHash(sendresponse.getHash(), computeHash(sendresponse.getResponse().toString()))
							||sendresponse.getResponse() instanceof MessageResponse){
						return sendresponse.getResponse();
					}else{
						System.out.println("Hashfehler: "+sendresponse.getResponse().toString());
						list();
					}
					
					//return (Response) requestToFileserver(fileServerInfoList.get(s),new ListRequest());
				}
			}
			return new MessageResponse("No Fileservers online.");
		}
		/**
		 * @see proxy.IProxy#download(message.request.DownloadTicketRequest)
		 */
		@Override
		public Response download(DownloadTicketRequest request) throws IOException {
			System.out.println("Received Downloadticketrequest from "+currentUser);
			Set<String> keyset = fileServerInfoList.keySet(); 
			synchronized(fileServerInfoList){
				synchronized(userInfoList){
					//wenn keine fileserver online
					if(keyset.isEmpty()){
						System.out.println("No fileservers online at the time.");
						return new DownloadTicketResponse(null);
					}
					//wenn die file nicht vorhanden ist
					if(!((ListResponse) list()).getFileNames().contains(request.getFilename())){
						System.out.println("File not found.");
						return new DownloadTicketResponse(null);
					}
					//fileserver mit min usage suchen
					long minUsage = Integer.MAX_VALUE;
					FileServerInfo si = null;
					String fileservername = null;
					InfoResponse fileinfo = null;
					for(String s: keyset){
						if(fileServerInfoList.get(s).getUsage()<minUsage && fileServerInfoList.get(s).isOnline()){
							si = fileServerInfoList.get(s);
							fileservername = s;
							minUsage = si.getUsage();
							System.out.println("Das hier sollte zumindest einmal erscheinen");
						}
					}
					//credits abziehen
					UserInfo userinfo = userInfoList.get(currentUser);
					//wenn user zu wenige credits hat
					System.out.println("Checking users credits...");
					InfoRequest irequest = new InfoRequest(request.getFilename());
					byte[] temphash = computeHash(irequest.toString());
					HashPlusObjectRequest sendrequest = new HashPlusObjectRequest(temphash, irequest);
					System.out.println(si+"sdkfnldkfnsldkfnsdlkfsdlkfnsdlkfnlsdkfnlskdnf");
					HashPlusObjectResponse sendresponse = (HashPlusObjectResponse) requestToFileserver(si,sendrequest);
					if(validateHash(sendresponse.getHash(), computeHash(sendresponse.getResponse().toString()))
							||sendresponse.getResponse() instanceof MessageResponse){
						fileinfo = (InfoResponse) sendresponse.getResponse();
						if(userinfo.getCredits()<fileinfo.getSize()){
							return new MessageResponse("Not enough credits.");
						}
					}else{
						System.out.println("Hashfehler: "+sendresponse.getResponse().toString());
						download(request);
					}
//					fileinfo = (InfoResponse) requestToFileserver(si, new InfoRequest(request.getFilename()));
//					if(userinfo.getCredits()<fileinfo.getSize()){
//						return new MessageResponse("Not enough credits.");
//					}
					//credits abziehen && usage erhehen
					//-------------------
//					InfoRequest irequest = new InfoResponse(request.getFilename());
//					byte[] temphash = computeHash(irequest.toString());
					//HashPlusObjectRequest sendrequest = new HashPlusObjectRequest(temphash, irequest);
					//HashPlusObjectResponse sendresponse = (HashPlusObjectResponse) requestToFileserver(fileServerInfoList.get(si),sendrequest);
					if(validateHash(sendresponse.getHash(), computeHash(sendresponse.getResponse().toString()))){
						fileinfo = (InfoResponse) sendresponse.getResponse();
						fileinfo.getSize();
						userInfoList.put(currentUser, new UserInfo(userinfo.getName(), userinfo.getCredits()-(fileinfo).getSize(), userinfo.isOnline()));
						fileServerInfoList.put(fileservername, new FileServerInfo(si.getAddress(), si.getPort(), (si.getUsage()+fileinfo.getSize()), si.isOnline()));
						System.out.println("Credits removed.");
						if(userinfo.getCredits()<fileinfo.getSize()){
							return new MessageResponse("Not enough credits.");
						}
					
					//---------------------
//					userInfoList.put(currentUser, new UserInfo(userinfo.getName(), userinfo.getCredits()-((InfoResponse) requestToFileserver(si, new InfoRequest(request.getFilename()))).getSize(), userinfo.isOnline()));
//					fileServerInfoList.put(fileservername, new FileServerInfo(si.getAddress(), si.getPort(), (si.getUsage()+fileinfo.getSize()), si.isOnline()));
//					System.out.println("Credits removed.");
					//stage4 part------------------
					synchronized(downloadlist){
						if(downloadlist.containsKey(fileinfo.getFilename()))downloadlist.put(fileinfo.getFilename(), downloadlist.get(fileinfo.getFilename())+1);
						else downloadlist.put(fileinfo.getFilename(), 1);
					}
					Set<String> clients = clientlist.keySet();
					for(String s : clients){
						ClientConnection c = clientlist.get(s);
						synchronized(c.subscribtions){
							if(c.subscribtions.containsKey(fileinfo.getFilename())){
								if(c.subscribtions.get(fileinfo.getFilename())<=downloadlist.get(fileinfo.getFilename())){
									String notify = "Notification: "+fileinfo.getFilename()+" got downloaded "+c.subscribtions.get(fileinfo.getFilename())+" times!.";
									c.notifyClient(notify);
									c.subscribtions.remove(fileinfo.getFilename());
								}
							}
						}
					}
					//-----------------------------

					//downloadticketresponse erstellen
					
					
					VersionRequest vrequest = new VersionRequest(request.getFilename());
					byte[] versiontemphash = computeHash(vrequest.toString());
					HashPlusObjectRequest versionsendrequest = new HashPlusObjectRequest(versiontemphash, vrequest);
					HashPlusObjectResponse versionsendresponse = (HashPlusObjectResponse) requestToFileserver(si,versionsendrequest);
					if(validateHash(versionsendresponse.getHash(), computeHash(versionsendresponse.getResponse().toString()))){
						if(versionsendresponse.getResponse() instanceof MessageResponse){
							System.out.println("Hashfehler: "+sendresponse.getResponse().toString());
							download(request);
						}
						return new DownloadTicketResponse(new DownloadTicket(
								currentUser,
								request.getFilename(),

								ChecksumUtils.generateChecksum(
										currentUser,
										request.getFilename(),
										((VersionResponse) versionsendresponse.getResponse()).getVersion(),
										(fileinfo).getSize()
										),
										si.getAddress(),
										si.getPort()
								)
								);
					}else{
						download(request);
					}
					
					
				}
			}
			}
			return null;
		}
		/**
		 * @see proxy.IProxy#upload(message.request.UploadRequest)
		 */
		@Override
		public MessageResponse upload(UploadRequest request) throws IOException {
			System.out.println("Received Uploadrequest from "+currentUser);
			boolean uploaded = false;
			MessageResponse messageresponse = new MessageResponse("Could not upload file.");
			
			
			//find number of servers and set read/write quorum
			if(qw == 0){ //should only be done at the first upload according to the assignment
				int n = 0;
				for (String s : fileServerInfoList.keySet()){
					if(fileServerInfoList.get(s).isOnline()){
						n++;
					}
				}
				qw = n/2 + 1;
				//qr = n - n/2 + 1;  lazy gifford qw = qr
			}
			//find the <qr> servers with lowest load
			HashMap<Long, FileServerInfo> lowestUsageServers = new HashMap<Long, FileServerInfo>();
			long highest = 0;
			for(String s : fileServerInfoList.keySet()){
				long usage = fileServerInfoList.get(s).getUsage();
				FileServerInfo fs = fileServerInfoList.get(s);
				if(fs.isOnline()){
					if(lowestUsageServers.size() < qw){
						lowestUsageServers.put(usage, fs);
						if(usage > highest){
							highest = usage;
						}
					}else if(usage < highest){
						lowestUsageServers.remove(highest);
						lowestUsageServers.put(usage, fs);
						highest = 0;
						for(long usg : lowestUsageServers.keySet()){
							if(usg > highest){
								highest = usg;
							}
						}
					}
				}
			}
			//find the highest version on lowest load servers
			int version = 0; 
			for(FileServerInfo fs : lowestUsageServers.values()){
				VersionRequest vrequest = new VersionRequest(request.getFilename());
				byte[] temphash = computeHash(vrequest.toString());
				HashPlusObjectRequest sendrequest = new HashPlusObjectRequest(temphash, vrequest);
				HashPlusObjectResponse sendresponse = (HashPlusObjectResponse) requestToFileserver(fs,sendrequest);
				if(!validateHash(sendresponse.getHash(), computeHash(sendresponse.getResponse().toString()))
						||sendresponse.getResponse() instanceof MessageResponse){
					System.out.println("Hashfehler: "+sendresponse.getResponse().toString());
					upload(request);
				}
				//Response response = (Response) requestToFileserver(fs, new VersionRequest(request.getFilename()));
				if(sendresponse.getResponse() instanceof VersionResponse){
					VersionResponse vr = (VersionResponse) sendresponse.getResponse(); 
					if(vr.getVersion() > version){
						version = vr.getVersion();
					}
				} else {
					//nothing, file does not exist on the server which is expected behavior.
				}
			}
			UploadRequest versionfixrequest = new UploadRequest(request.getFilename(), version + 1, request.getContent());
		
			//lazy gifford scheme: qw = gr
			//upload to the <gw> lowest usage servers
			for(FileServerInfo fs : lowestUsageServers.values()){
				System.out.println("Uploading "+versionfixrequest.getFilename()+" to fileserver: "+fs.getPort());
				uploaded = true;
				
				byte[] temphash = computeHash(versionfixrequest.toString());
				HashPlusObjectRequest sendrequest = new HashPlusObjectRequest(temphash, versionfixrequest);
				HashPlusObjectResponse sendresponse = (HashPlusObjectResponse) requestToFileserver(fs,sendrequest);
				MessageResponse mesrp = new MessageResponse("Fehler beim verifizieren des Hashes");
				if(validateHash(sendresponse.getHash(), computeHash(sendresponse.getResponse().toString()))
						&&!(sendresponse.getResponse().equals(mesrp))){
					messageresponse = (MessageResponse) sendresponse.getResponse();
				}else{
					System.out.println("Hashfehler: "+sendresponse.getResponse().toString());
					upload(request);
				}
				
//				messageresponse = (MessageResponse) requestToFileserver(fs, request);
			}
			
			
			
			
			//credits hinzufuegen
			synchronized(userInfoList){
				if(uploaded){
					UserInfo userinfo = userInfoList.get(currentUser);
					userInfoList.put(currentUser, new UserInfo(userinfo.getName(), userinfo.getCredits()+(2*request.getContent().length), userinfo.isOnline()));
					System.out.println("Credits successfully added.");
				}
			}
			return messageresponse;
		}
		/**
		 * @see proxy.IProxy#logout()
		 */
		@Override
		public MessageResponse logout() throws IOException {
			synchronized(userInfoList){
				System.out.println("loging out "+currentUser);
				UserInfo userinfo = userInfoList.get(currentUser);
				userInfoList.put(currentUser, new UserInfo(userinfo.getName(), userinfo.getCredits(), false));
				//stage4 part------------------
				synchronized(clientlist){
					clientlist.remove(currentUser);
				}
				//-----------------------------
				currentUser = null;
				return new MessageResponse(userinfo.getName()+" successfully logged out.");
			}
		}
		//stage4 part------------------
		public void subscribe(String filename, int count, ICallback callback){
			subscribtions.put(filename, count);
			this.callback=callback;
		}
		private void notifyClient(String notify){
			try {
				callback.notifyMe(notify);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//-----------------------------
	}
	public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}
	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}
	//REMOTE-METHODS----------------------------------------------------------------------------------------------
		public int getReadQuorum(){
			//return somelist.size();???
			return qw;
		}

		public int getWriteQuorum(){
			//return someotherlist.size();???
			return qw;
		}

		public String getTopThreeDownloads(){
			//return downloadlist;
			ArrayList<String> top3list = new ArrayList<String>();
			ArrayList<String> keyset = new ArrayList<String>(downloadlist.keySet());
			String filename;
			String returnstring = "Top Three Downloads:";
			int count;
			int stringcounter = 1;
			for(int i=0;i<3;i++){
				count = 0;
				filename = null;
				for(String s : keyset){
					if(downloadlist.get(s)>count){
						count=downloadlist.get(s);
						filename = s;
					}
				}
				keyset.remove(filename);
				if(filename!=null&&!top3list.contains(filename+" "+count)){
					top3list.add(filename+" "+count);
					//returnstring += stringcounter+". "+filename+" "+count;
				}
			}
			for(String s: top3list){
				System.out.println(top3list.size());
				returnstring += "\n"+stringcounter+". "+s;
				stringcounter++;
			}
			
			return returnstring;
		}

		public void subscribe(String username, String filename, int count, ICallback callback){
			//create new Subscribtion with filename,count and save callback
			clientlist.get(username).subscribe(filename, count, callback);
		}

		public String getProxyPublicKey(){
			BufferedReader in;
			FileReader fr;
			String publicKey = null;
			File proxypublickey = new File(proxyConfig.getString("keys.dir")+"/proxy.pub.pem");
			
			System.out.println(proxypublickey.getAbsolutePath());
			System.out.println(proxypublickey.exists());
			
			try {		
				fr = new FileReader(proxypublickey);
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
				publicKey = (String) stringbuilder.toString();
				System.out.println(publicKey);
			} catch (FileNotFoundException e) {
				System.out.println("Could not find file: proxy.pub.pem");
				//e.printStackTrace();
			} catch (IOException e) {
				System.out.println("gugugugCould not access file: proxy.pub.pem");
				//e.printStackTrace();
			} 

			return publicKey;
		}

		public void setUserPublicKey(String username, String key){
			Writer writer = null;
			File userpublickey = new File(proxyConfig.getString("keys.dir")+"/"+username+".pub.pem");
			if(userpublickey.exists()){
				System.out.println("Deleting "+username+".pub.pem"+" because it already exists.");
				userpublickey.delete();
			}
			try {
				writer = new FileWriter(userpublickey);
				writer.write(new String(key));
			} catch (IOException e) {
				System.out.println("Could not write File: "+username+".pub.pem");
			} finally {
				if (writer != null)
					try {
						writer.close();
					} catch (IOException e) {
					}
			}
		}

}