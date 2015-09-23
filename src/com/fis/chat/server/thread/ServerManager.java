package com.fis.chat.server.thread;

import java.util.ArrayList;

import com.fss.ddtp.ChatSocketTransmitter;
import com.fss.thread.ProcessorListener;
import com.fss.thread.ThreadManager;

public class ServerManager extends ThreadManager{

	
	public ServerManager(int port, ProcessorListener lsn) throws Exception {
		super(port, lsn);
	}
	
}
