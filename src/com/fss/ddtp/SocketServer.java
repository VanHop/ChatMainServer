package com.fss.ddtp;

import com.fss.util.AppException;

/**
 * <p>Title: A Thread which listen from request queue of SocketTransmitterThread</p>
 * <p>Description:
 *    -   Always read from request queue of SocketTransmitterThread
 *    -   After that, it will call a class to handle request
 * </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: FSS-FPT-BU5</p>
 * @author Dang Dinh Trung
 * @version 2.0
 */

public class SocketServer implements Runnable
{
	private Thread mthrMain;
	private SocketTransmitter channel = null;
	///////////////////////////////////////////////////////////
	/**
	 * Create processorthread for transmitter thread
	 * @param channel transmitter thread
	 */
	///////////////////////////////////////////////////////////
	public SocketServer(SocketTransmitter channel)
	{
		this.channel = channel;
	}
	///////////////////////////////////////////////////////////
	/**
	 * Start processor thread
	 */
	///////////////////////////////////////////////////////////
	public void start()
	{
		if(mthrMain != null)
			mthrMain.stop();
		mthrMain = new Thread(this);
		mthrMain.start();
	}
	///////////////////////////////////////////////////////////
	/**
	 * always listen from request queue and process request
	 * @author
	 * - Thai Hoang Hiep
	 * - Dang Dinh Trung
	 */
	///////////////////////////////////////////////////////////
	public void run()
	{
		while(isConnected())
		{
			// Get request from queue
			DDTP request = channel.getFirstRequest();
			DDTP response = null;

			try
			{
				// Process request
				if(request == null)
					continue;
				response = Processor.processRequest(channel,request);
			}
			catch(Exception e)
			{
				response = new DDTP();
				if(e instanceof AppException)
					response.setException((AppException)e);
				else
					response.setException(new AppException(e.getMessage(),"SocketServer.run",""));
				e.printStackTrace();
			}
			finally
			{
				try
				{
					// Return response
					if(request != null)
					{
						String strRequestID = request.getRequestID();
						if(strRequestID.length() > 0 && channel != null)
						{
							response.setResponseID(strRequestID);
							channel.sendResponse(response);
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	///////////////////////////////////////////////////////////
	/**
	 * Check connection
	 * @return true if connected, otherwise false
	 * @author HiepTH
	 */
	///////////////////////////////////////////////////////////
	public boolean isConnected()
	{
		return (channel != null && channel.isOpen());
	}
}
