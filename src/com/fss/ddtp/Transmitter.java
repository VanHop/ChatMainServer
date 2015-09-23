package com.fss.ddtp;

/**
 * <p>Title: Transmitter</p>
 * <p>Description: Send request to and receive response from ddtp server</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: FPT</p>
 * @author Thai Hoang Hiep
 * @version 1.0
 */

public interface Transmitter
{
	/////////////////////////////////////////////////////////////////
	/**
	 * Send request to server
	 * @param strClass class contain method to call
	 * @param strFunctionName method to call
	 * @return DDTP contain response data
	 * @throws Exception
	 * @author Thai Hoang Hiep - Date: 3/3/2004
	 */
	/////////////////////////////////////////////////////////////////
	DDTP sendRequest(String strClass,String strFunctionName) throws Exception;
	/////////////////////////////////////////////////////////////////
	/**
	 * Send request to server
	 * @param strClass class contain method to call
	 * @param strFunctionName method to call
	 * @param request DDTP contain request data
	 * @return DDTP contain response data
	 * @throws Exception
	 * @author Thai Hoang Hiep - Date: 3/3/2004
	 */
	/////////////////////////////////////////////////////////////////
	DDTP sendRequest(String strClass,String strFunctionName,DDTP request) throws Exception;
}
