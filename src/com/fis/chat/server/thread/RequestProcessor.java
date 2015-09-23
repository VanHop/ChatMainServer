package com.fis.chat.server.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.fss.ddtp.ChatSocketTransmitter;
import com.fss.ddtp.DDTP;
import com.fss.ddtp.SocketProcessor;


/**
 * <p>Title: </p>
 * <p>Description:
 *    DDTPServer only process request from client
 *    and pass response for it
 * </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: FSS-FPT</p>
 * @author
 * - Thai Hoang Hiep
 * - Dang Dinh Trung
 * @version 2.0
 */

public class RequestProcessor extends SocketProcessor
{

	private ThreadServer threadServer;

	public RequestProcessor() throws Exception{
	}

	public RequestProcessor(ThreadServer mgr) throws Exception{
		threadServer = mgr;
	}

	public void setCaller(Object objCaller){
		super.setCaller(objCaller);
		if(channel != null && channel.mobjParent instanceof ThreadServer){
			threadServer = (ThreadServer)channel.mobjParent;
		}
	}

	public void notifyUserConnected() {
		try{
			String username = request.getString("username");
			int idSubServer = Integer.parseInt(request.getString("subServer"));
			threadServer.getListAllClient().put(username, idSubServer);
			DDTP requestTo = new DDTP();
			requestTo.setString("username",username);
			requestTo.setString("message",username + ": đã online!");
			sendRequestToAll(requestTo,"notifyUserConnected","RequestProcessor",idSubServer);
			sendListUserOnline(username);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	public void sendRequestToAll(DDTP request,String strFunctionName,String strClassName,int idSubServer) throws Exception
	{
		for(Integer port : threadServer.getListSubServer().keySet()){
			if(port.equals(idSubServer))
				continue;
			else{
				ChatSocketTransmitter channelSub = threadServer.getListSubServer().get(port);
				if(channelSub.isOpen())
					channelSub.sendRequest(strClassName,strFunctionName,request);
			}
		}
		
	}
	public Vector<String> getAllUserSubServer(){
		Vector<String> listAllUser = new Vector<String>();
		for(String username : threadServer.getListAllClient().keySet()){
			listAllUser.add(username);
		}

		return listAllUser;
	}
	public String sendMessage(){
		String idSubServer = request.getString("idSubServer");
		String username = request.getString("me");
		int idSubServerUsername = threadServer.getListAllClient().get(username);
		
		threadServer.logMonitor("Đã nhận được 1 request từ Subserver " + idSubServer);
		
		ChatSocketTransmitter channelSub = threadServer.getListSubServer().get(idSubServerUsername);
		if(channelSub != null && channelSub.isOpen())
			try {
				DDTP response = channelSub.sendRequest("MessageProcessor", "receiverMessageServer", request);
				if((String)response.getReturn() != null){
					threadServer.logMonitor("Đã gửi tin nhắn đến " + username + " trong subserver có cổng " + idSubServerUsername);
					return "OK";
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		return null;
	}
	public void sendFile(){
		String idSubServer = request.getString("idSubServer");
		String username = request.getString("friend");
		int idSubServerUsername = threadServer.getListAllClient().get(username);
		
		threadServer.logMonitor("Đã nhận được 1 request từ Subserver " + idSubServer);

		ChatSocketTransmitter channelSub = threadServer.getListSubServer().get(idSubServerUsername);
		if(channelSub != null && channelSub.isOpen())
			try {
				channelSub.sendRequest("MessageProcessor", "receiverFileServer", request);
				threadServer.logMonitor("Đã gửi file cho " + username + " trong subserver " + idSubServerUsername);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	public void makeGroup(){
		String idSubServer = request.getString("idSubServer");
		Vector<String> userInGroup =  request.getVector("listUserInGroup");
		Map<Integer, String> idSubserverOfUser = new HashMap<Integer,String>();
		for(String username : userInGroup){
			idSubserverOfUser.put(threadServer.getListAllClient().get(username),"id");
		}
		threadServer.logMonitor("Đã nhận yêu cầu tạo group của subserver " + idSubServer);
		for(int port : threadServer.getListSubServer().keySet()){
			if(port == Integer.parseInt(idSubServer) || idSubserverOfUser.get(port) == null)
				continue;
			ChatSocketTransmitter channelSub = threadServer.getListSubServer().get(port);
			try {
				channelSub.sendRequest("RequestProcessor", "receiverRequestMakeGroup", request);
				threadServer.logMonitor("Đã gửi yêu cầu tạo group cho subserver " + port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void sendMessageIntoGroup(){
		int idSubServer = Integer.parseInt(request.getString("idSubServer"));
		Vector<String> userInGroup =  request.getVector("listUserInGroup");
		Map<Integer, String> idSubserverOfUser = new HashMap<Integer,String>();
		for(String username : userInGroup){
			idSubserverOfUser.put(threadServer.getListAllClient().get(username),"id");
		}
		threadServer.logMonitor("Đã nhận yêu cầu của subserver " + idSubServer);
		for(int port : threadServer.getListSubServer().keySet()){
			if(port == idSubServer || idSubserverOfUser.get(port) == null)
				continue;
			ChatSocketTransmitter channelSub = threadServer.getListSubServer().get(port);
			try {
				channelSub.sendRequest("RequestProcessor", "receiverMessageIntoGroup", request);
				threadServer.logMonitor("Đã gửi tin nhắn cho subserver " + port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void notifyUserDisconnected(){
		int idSubServer = Integer.parseInt(request.getString("idSubServer"));
		threadServer.getListAllClient().remove(request.getString("username"));
		for(int port : threadServer.getListSubServer().keySet()){
			if(port == idSubServer)
				continue;
			ChatSocketTransmitter channelSub = threadServer.getListSubServer().get(port);
			try {
				channelSub.sendRequest("RequestProcessor", "notifyUserDisconnected", request);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void logoutGroup(){
		int idSubServer = Integer.parseInt(request.getString("idSubServer"));
		String username = request.getString("username");
		Vector<String> userInGroup =  request.getVector("listUserInGroup");
		Map<Integer, String> idSubserverOfUser = new HashMap<Integer,String>();
		for(String username1 : userInGroup){
			idSubserverOfUser.put(threadServer.getListAllClient().get(username1),"id");
		}
		threadServer.logMonitor("Đã nhận yêu cầu của subserver " + idSubServer);
		for(int port : threadServer.getListSubServer().keySet()){
			if(port == idSubServer || idSubserverOfUser.get(port) == null)
				continue;
			ChatSocketTransmitter channelSub = threadServer.getListSubServer().get(port);
			try {
				channelSub.sendRequest("RequestProcessor", "receiverRequestLogoutGroup", request);
				threadServer.logMonitor("Đã gửi tin logout cho subserver " + port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		threadServer.getListAllClient().remove(username);
	}
	public void updateGroup(){
		int idSubServer = Integer.parseInt(request.getString("idSubServer"));
		Vector<String> userInGroup =  request.getVector("listUserInGroup");
		Map<Integer, String> idSubserverOfUser = new HashMap<Integer,String>();
		for(String username1 : userInGroup){
			idSubserverOfUser.put(threadServer.getListAllClient().get(username1),"id");
		}
		threadServer.logMonitor("Đã nhận yêu cầu của subserver " + idSubServer);
		for(int port : threadServer.getListSubServer().keySet()){
			if(port == idSubServer || idSubserverOfUser.get(port) == null)
				continue;
			ChatSocketTransmitter channelSub = threadServer.getListSubServer().get(port);
			try {
				channelSub.sendRequest("RequestProcessor", "receiverRequestUpdateGroup", request);
				threadServer.logMonitor("Đã gửi update group cho subserver " + port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void receiveListUser(){
		Vector<String> listUser = request.getVector("listUser");
		String idSubServer = request.getString("IdSubServer");
		for(String username : listUser)
			threadServer.getListAllClient().put(username, Integer.parseInt(idSubServer));
		//notifyListUserOnline();
	}
	public void notifyListUserOnline(){
		DDTP request = new DDTP();
		Vector<String> listUser = getAllUserSubServer();
		request.setVector("listUser", listUser);
		for(int port : threadServer.getListSubServer().keySet()){
			try {
				threadServer.getListSubServer().get(port).sendRequest("RequestProcessor", "notifyListUserOnline", request);
				Thread.sleep(200);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void sendListUserOnline(String username){
		DDTP request = new DDTP();
		Vector<String> listUser = getAllUserSubServer();
		int port = threadServer.getListAllClient().get(username);
		request.setVector("listUser", listUser);
		request.setString("username", username);
		
		try {
			threadServer.getListSubServer().get(port).sendRequest("RequestProcessor", "listUserOnline", request);
			threadServer.logMonitor("gửi danh sách người dùng cho " + username);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
