package test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import proxy.IProxyCli;
import server.IFileServerCli;
import util.ComponentFactory;
import util.Config;
import util.Util;
import cli.Shell;
import cli.TestInputStream;
import cli.TestOutputStream;
import client.IClientCli;

public class LoadTest {

	static ComponentFactory componentFactory = new ComponentFactory();
	IProxyCli proxy;
	IFileServerCli server1;
	IFileServerCli server2;
	ArrayList<IClientCli> clientlist = new ArrayList<IClientCli>();
	Config loadtestcfg = new Config("loadtest");
	Config clientcfg = new Config("client");
	Config fs1cfg = new Config("fs1");
	Config fs2cfg = new Config("fs2");
	String userpropertiesBackup;
	long startTime;
	long prevTime;
	long currTime;
	float frameTime;
	String downnewfile = "downFile.txt";
	String upsamefile = "sameFile.txt";
	String upnewfile = "newFile.txt";
	int size = 0;
	int c = 0; //newfile counter

	@Before
	public void before() throws Exception {
		proxy = componentFactory.startProxy(new Config("proxy"), new Shell("proxy", new TestOutputStream(System.out), new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);

		server1 = componentFactory.startFileServer(new Config("fs1"), new Shell("fs1", new TestOutputStream(System.out), new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);
		server2 = componentFactory.startFileServer(new Config("fs2"), new Shell("fs2", new TestOutputStream(System.out), new TestInputStream()));
		Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);
		//user.properties lesen
		File userfile = new File("src/main/resources/user.properties");
		BufferedReader bufferedreader = new BufferedReader(new FileReader(userfile));
		String line = null;
		StringBuilder stringbuilder = new StringBuilder();
		String lineseperator = System.getProperty("line.separator");
		while((line = bufferedreader.readLine()) != null) {
			stringbuilder.append(line);
			stringbuilder.append(lineseperator);
		}
		bufferedreader.close();
		userpropertiesBackup=stringbuilder.toString();
		for(int i=0;i<loadtestcfg.getInt("clients");i++){
			//editing user.properties
			stringbuilder.append("client"+i+".credits = "+"1000");
			stringbuilder.append(lineseperator);
			stringbuilder.append("client"+i+".password = "+"12345");
			stringbuilder.append(lineseperator);
			generateKeys("client"+i);
		}
		//overwriting user.properties
				Writer writer = null;
				if(userfile.exists()){
					userfile.delete();
				}
				try {
					writer = new FileWriter(userfile);
					writer.write(stringbuilder.toString());
				} catch (IOException e) {
					System.out.println("Could not write File");
				} finally {
					if (writer != null)
						try {
							writer.close();
						} catch (IOException e) {
						}
				}
				//Generating File with given size
				size = loadtestcfg.getInt("fileSizeKB");
				generateRandomFile(size, fs1cfg.getString("fileserver.dir")+"/"+downnewfile);
				generateRandomFile(size, fs2cfg.getString("fileserver.dir")+"/"+downnewfile);
				
				generateRandomFile(size, clientcfg.getString("download.dir")+"/"+upsamefile);
				generateRandomFile(size, fs1cfg.getString("fileserver.dir")+"/"+upsamefile);
				generateRandomFile(size, fs2cfg.getString("fileserver.dir")+"/"+upsamefile);
		//---------------------
		for(int i=0;i<loadtestcfg.getInt("clients");i++){
			clientlist.add(componentFactory.startClient(new Config("client"), new Shell("client", new TestOutputStream(System.out), new TestInputStream())));
			Thread.sleep(Util.WAIT_FOR_COMPONENT_STARTUP);
		}
		
		
	}

	@After
	public void after() throws Exception {
		try {
			proxy.exit();
		} catch (Exception e) {
			// This should not happen. In case it does, output the stack trace for easier trouble shooting.
			e.printStackTrace();
		}
		try {
			server1.exit();
		} catch (IOException e) {
			// This should not happen. In case it does, output the stack trace for easier trouble shooting.
			e.printStackTrace();
		}
		try {
			server2.exit();
		} catch (IOException e) {
			// This should not happen. In case it does, output the stack trace for easier trouble shooting.
			e.printStackTrace();
		}
		try {
			for(IClientCli client : clientlist)client.exit();
		} catch (IOException e) {
			// This should not happen. In case it does, output the stack trace for easier trouble shooting.
			e.printStackTrace();
		}
		//resetting user.properties
		File userfile = new File("src/main/resources/user.properties");
		Writer writer = null;
		if(userfile.exists()){
			userfile.delete();
		}
		try {
			writer = new FileWriter(userfile);
			writer.write(userpropertiesBackup);
		} catch (IOException e) {
			System.out.println("Could not write File");
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
				}
		}
		//deleting key copies
		for(int i=0;i<loadtestcfg.getInt("clients");i++){
			File keyfile = new File(clientcfg.getString("keys.dir")+"/client"+i+".pub.pem");
			if(keyfile.exists())keyfile.delete();
			File keyfile2 = new File(clientcfg.getString("keys.dir")+"/client"+i+".pem");
			if(keyfile2.exists())keyfile2.delete();
		}
		//deleting all new upload files
		for(int i=1;i<c;i++){
			File upfile = new File(fs1cfg.getString("fileserver.dir")+"/"+c+upnewfile);
			if(upfile.exists())upfile.delete();
			File upfile2 = new File(fs2cfg.getString("fileserver.dir")+"/"+c+upnewfile);
			if(upfile2.exists())upfile2.delete();
		}
		//deleting new download file
		File temp = new File(fs1cfg.getString("fileserver.dir")+"/"+downnewfile);
		if(temp.exists())temp.delete();
		File temp2 = new File(fs2cfg.getString("fileserver.dir")+"/"+downnewfile);
		if(temp2.exists())temp2.delete();
		File temp3 = new File(clientcfg.getString("download.dir")+"/"+downnewfile);
		if(temp3.exists())temp3.delete();
		//deleting overwrite upload file
		File temp4 = new File(fs1cfg.getString("fileserver.dir")+"/"+upsamefile);
		if(temp4.exists())temp4.delete();
		File temp5 = new File(fs2cfg.getString("fileserver.dir")+"/"+upsamefile);
		if(temp5.exists())temp5.delete();
		File temp6 = new File(clientcfg.getString("download.dir")+"/"+upsamefile);
		if(temp6.exists())temp6.delete();

	}


	@Test
	public void test() throws Exception {
		//LOGIN ALL CLIENTS
		int i = 0;
		for(IClientCli client : clientlist){
			String actual = client.login("client"+i,"12345").toString();
			String expected = "success";
			//assertTrue(String.format("Response must contain '%s' but was '%s'", expected, actual), actual.contains(expected));
			i++;
		}
		//INIT TIME
		startTime = Calendar.getInstance().getTimeInMillis();
		prevTime = startTime;
		currTime = startTime;
		float downRate = 60/(loadtestcfg.getInt("downloadsPerMin"));
		float upRate = 60/(loadtestcfg.getInt("uploadsPerMin"));
		float overwriteRatio = loadtestcfg.getInt("overwriteRatio");
		float uploadCountOverwrite = 0;
		float uploadCountNew = 0;
		//DOWNLOADS AND UPLOADS
		int min = 1; //test duration in minutes
		while((currTime-startTime)<(10000)){ //
			//compute time between frames
			currTime = Calendar.getInstance().getTimeInMillis();
			frameTime = ((currTime-prevTime)/1000.0f);
			prevTime = currTime;
			//DOWNLOADS
			if(frameTime>=downRate){
				for(IClientCli client : clientlist){
					client.download(downnewfile); //download generated file TODO: enter filename
				}
			}
			//UPLOADS
			if(frameTime>=upRate){
				for(IClientCli client : clientlist){
					if((Math.floor((uploadCountOverwrite+1)*(1-overwriteRatio)))<uploadCountNew){
						client.upload(upsamefile); //upload existing file TODO: enter filename
						uploadCountOverwrite++;
					}
					else{
						c++;
						generateRandomFile(size, clientcfg.getString("download.dir")+"/"+c+upnewfile);
						client.upload(upnewfile); //upload new file TODO: enter filename
						uploadCountNew++;
					}
				}
			}
		}
		assertTrue(true);
	}

	private void generateKeys(String username) throws Exception{
		//public
		File clientfile = new File(clientcfg.getString("keys.dir")+"/alice.pub.pem");
		BufferedReader bufferedreader = new BufferedReader(new FileReader(clientfile));
		String line = null;
		StringBuilder stringbuilder = new StringBuilder();
		String lineseperator = System.getProperty("line.separator");
		while((line = bufferedreader.readLine()) != null) {
			stringbuilder.append(line);
			stringbuilder.append(lineseperator);
		}
		bufferedreader.close();

		Writer writer = null;
		try {
			writer = new FileWriter(clientcfg.getString("keys.dir")+"/"+username+".pub.pem");
			writer.write(stringbuilder.toString());
		} catch (IOException e) {
			System.out.println("Could not write File");
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
				}
		}

		//private
		File clientfile2 = new File(clientcfg.getString("keys.dir")+"/alice.pem");
		bufferedreader = new BufferedReader(new FileReader(clientfile2));
		line = null;
		StringBuilder stringbuilder2 = new StringBuilder();
		lineseperator = System.getProperty("line.separator");
		while((line = bufferedreader.readLine()) != null) {
			stringbuilder2.append(line);
			stringbuilder2.append(lineseperator);
		}
		bufferedreader.close();

		writer = null;
		try {
			writer = new FileWriter(clientcfg.getString("keys.dir")+"/"+username+".pem");
			writer.write(stringbuilder2.toString());
		} catch (IOException e) {
			System.out.println("Could not write File");
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
				}
		}
	}

	private void generateRandomFile(int size, String path) throws Exception{
		//Generating File with given size
		File downloadFile = new File(path);
		RandomAccessFile raf = new RandomAccessFile(downloadFile, "rw");  
		try {  
			raf.setLength(size);
		}  
		finally {  
			raf.close();  
		}
		/*//writing file to directory
	    writer = null;
		if(downloadFile.exists()){
			downloadFile.delete();
		}
		try {
			writer = new FileWriter(downloadFile);
			writer.write(downloadFile.toString());
		} catch (IOException e) {
			System.out.println("Could not write File");
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
				}
		}*/
	}

}
