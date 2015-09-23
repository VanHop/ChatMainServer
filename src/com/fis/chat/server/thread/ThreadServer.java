package com.fis.chat.server.thread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.fss.ddtp.ChatSocketTransmitter;
import com.fss.ddtp.DDTP;
import com.fss.thread.ManageableThread;
import com.fss.thread.ParameterType;
import com.fss.thread.ThreadConstant;
import com.fss.util.AppException;

/**
 * <p>Title: Listen server socket</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: FSS-FPT-BU5</p>
 * @author Dang Dinh Trung
 * @version 1.0
 */

public class ThreadServer extends ManageableThread
{
	protected ServerSocket serverSocket;
	protected Thread mthrMain = null;
	protected int miPort;
	private Map<Integer,ChatSocketTransmitter> listSubServer;
	private Map<String,Integer> listAllClient;
	private long waitTime = 0;

	public ThreadServer(){
		super();
		listSubServer = new HashMap<Integer, ChatSocketTransmitter>();
		listAllClient = new HashMap<String, Integer>();
	}
	
	public Map<Integer, ChatSocketTransmitter> getListSubServer() {
		return listSubServer;
	}

	public void setListSubServer(Map<Integer, ChatSocketTransmitter> listSubServer) {
		this.listSubServer = listSubServer;
	}

	public Map<String, Integer> getListAllClient() {
		return listAllClient;
	}

	public void setListAllClient(Map<String, Integer> listAllClient) {
		this.listAllClient = listAllClient;
	}

	public void start(){
		if(mthrMain != null)
			mthrMain.stop();
		mthrMain = new Thread(this);
		mthrMain.start();
	}
	@Override
	public void destroy(){
		super.destroy();
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(int i = 0 ; i < listSubServer.size() ; i++){
			listSubServer.get(i).close();
		}
		listSubServer.clear();
		this.mthrMain = null; 
	}
	public Vector getParameterDefinition() {
		Vector vtReturn = new Vector();
		vtReturn.addElement(createParameterDefinition("ServerPort", "", ParameterType.PARAM_TEXTBOX_MASK, "99999", "Server port for subserver"));
		vtReturn.addAll(super.getParameterDefinition());
		return vtReturn;
	}

	public void fillParameter() throws AppException {
		miPort = loadInteger("ServerPort");
		super.fillParameter();
	}
	
	protected void beforeSession() throws Exception {
		serverSocket = new ServerSocket(miPort);
		serverSocket.setSoTimeout(3000);
		logMonitor("Server start listening on "+miPort);
		waitTime = 0;
	}
	
	protected void afterSession() throws Exception {
		super.afterSession();
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(Integer port : listSubServer.keySet()){
			listSubServer.get(port).close();
		}
		listSubServer.clear();
		listAllClient.clear();
		this.mthrMain = null; 
	}
	
	@Override
	protected void processSession() throws Exception {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();
		long end = System.currentTimeMillis();
		while(miThreadCommand != ThreadConstant.THREAD_STOP)
		{
			start = System.currentTimeMillis();
			Socket sck = null;
			try
			{
				sck = serverSocket.accept();
				sck.setSoLinger(true,0);
				sck.setKeepAlive(true);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				if(waitTime == 0){
					RequestProcessor pro = new RequestProcessor(this);
					pro.notifyListUserOnline();
				}
				end = System.currentTimeMillis();
				waitTime += end - start;
				continue;
			}
			try
			{
				ChatSocketTransmitter channel = new ChatSocketTransmitter(sck)
				{
					public void close()
					{
						if(msckMain != null)
						{
							logMonitor("Đã tắt kết nối đến Subserver có cổng là "+ this.getIdSubServer());
							
							int idSubServer = this.getIdSubServer();
							Set<String> temps = null;

							temps = new HashSet<String>();
							for(String key : listAllClient.keySet()){
								if(listAllClient.get(key).equals(idSubServer)){
									temps.add(key);
								}
							}
							listAllClient.keySet().removeAll(temps);
							listSubServer.remove(this);
							super.close();
						}
					}
				};
				DDTP request = new DDTP(sck.getInputStream());
				channel.setIdSubServer(Integer.parseInt(request.getString("idSubServer")));
				channel.mobjParent = this;
				channel.setPackage("com.fss.thread.");
				channel.start();
				listSubServer.put(channel.getIdSubServer(), channel);
				logMonitor("Đã có Subserver kết nối đến cổng " + miPort + " và đang lắng nghe ở cổng " + channel.getIdSubServer());
				waitTime = 0;
			}
			catch(Exception e)
			{
				e.printStackTrace();
				continue;
			}
		}
	}


	public boolean isOpen() {
		if(serverSocket.isClosed())
			return false;
		return true;
	}
	
}
