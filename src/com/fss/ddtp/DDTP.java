package com.fss.ddtp;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.fss.util.*;

/**
 * <p>Title: DDTP</p>
 * <p>Description: Package of parameter can be transfer on net</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: FPT</p>
 * @author Thai Hoang Hiep
 * @version 1.0
 */

public class DDTP
{
	////////////////////////////////////////////////////////
	// static variables
	////////////////////////////////////////////////////////
	public static final String ENCODE = "utf-8";
	public static final String SIGN_SYMBOL = "DDTP";
	////////////////////////////////////////////////////////
	// static variables
	////////////////////////////////////////////////////////
	private static boolean AUTO_COMPRESS = true;
	private static final byte PLAIN_BUFFER = 0;
	private static final byte COMPRESSED_BUFFER = 1;
	private static final byte STRING_SYMBOL = 0;
	private static final byte VECTOR_SYMBOL = 1;
	private static final byte BINARY_SYMBOL = 2;
	public static final String REQUEST_ID_FIELD_NAME = "$reqid$";
	public static final String RESPONSE_ID_FIELD_NAME = "$resid$";
	public static final String RETURN_FIELD_NAME = "$rtn$";
	public static final String ERROR_DESCRIPTION_FIELD_NAME = "$err.dsc$";
	public static final String ERROR_CONTEXT_FIELD_NAME = "$err.ctx$";
	public static final String ERROR_INFO_FIELD_NAME = "$err.inf$";
	public static final String FUNCTION_FIELD_NAME = "$fnc$";
	public static final String CLASS_FIELD_NAME = "$cls$";
	////////////////////////////////////////////////////////
	// Variables
	////////////////////////////////////////////////////////
	private Hashtable mprtParameter = new Hashtable();
	////////////////////////////////////////////////////////
	/**
	 * Create empty ddtp
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public DDTP()
	{
	}
	////////////////////////////////////////////////////////
	/**
	 * Create ddtp from inputstream
	 * @param is inputstream contain ddtp data
	 * @throws IOException
	 */
	public DDTP(InputStream is) throws IOException
	{
		load(is);
	}
	////////////////////////////////////////////////////////
	/**
	 * Load parameter from input stream
	 * @param is InputStream
	 * @throws IOException
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public void load(InputStream is) throws IOException
	{
		byte[] btBuff = null;
		int iParameterCount = 0;
		int iType = 0;
		synchronized(is)
		{
			// Validate ddtp
			if(!SIGN_SYMBOL.equals(new String(StreamUtil.getFixedSizeBuffer(is,SIGN_SYMBOL.length()))))
				throw new IOException("Not a ddtp package");

			// Get buffer type
			iType = is.read();
			if(iType < 0)
				throw new IOException("No data to read");

			// Get parameter count
			iParameterCount = readBufferSize(is);

			// Get buffer size
			int iBufferSize = readBufferSize(is);
			btBuff = new byte[iBufferSize];

			int iByteRemain = iBufferSize;
			while(iByteRemain > 0)
			{
				int iByteRead = is.read(btBuff,iBufferSize - iByteRemain,iByteRemain);
				if(iByteRead < 0)
					throw new IOException("No data to read");
				iByteRemain -= iByteRead;
			}
		}

		// Decompress
		InputStream isUsed = new ByteArrayInputStream(btBuff);
		if(iType == COMPRESSED_BUFFER)
			isUsed = new InflaterInputStream(isUsed);
		else if(iType != PLAIN_BUFFER)
			throw new IOException("Unsupported buffer type " + iType);

		// Parse data
		while(iParameterCount > 0)
		{
			// Get name
			int iNameSize = readBufferSize(isUsed);
			String strName = new String(StreamUtil.getFixedSizeBuffer(isUsed,iNameSize),ENCODE);

			// Put value into storage
			getData().put(strName,loadValue(isUsed));
			iParameterCount--;
		}
	}
	////////////////////////////////////////////////////////
	/**
	 * Load ddtp from buffer
	 * @param is InputStream
	 * @return Object
	 * @throws IOException
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	private Object loadValue(InputStream is) throws IOException
	{
		// Get value type
		int iType = is.read();
		if(iType < 0)
			throw new IOException("No data to read");

		// Get value length
		int iSize = readBufferSize(is);

		// Get value
		Object objValue = null;
		if(iType == VECTOR_SYMBOL) // Vector
		{
			Vector vt = new Vector();
			for(int iIndex = 0;iIndex < iSize;iIndex++)
				vt.addElement(loadValue(is));
			objValue = vt;
		}
		else if(iType == STRING_SYMBOL) // String
			objValue = new String(StreamUtil.getFixedSizeBuffer(is,iSize),ENCODE);
		else if(iType == BINARY_SYMBOL) // Binary
			objValue = StreamUtil.getFixedSizeBuffer(is,iSize);
		else
			throw new IOException("Data type not supported");
		return objValue;
	}
	////////////////////////////////////////////////////////
	/**
	 *
	 * @param bCompress boolean
	 */
	////////////////////////////////////////////////////////
	public static void setAutoCompress(boolean bCompress)
	{
		AUTO_COMPRESS = bCompress;
	}
	////////////////////////////////////////////////////////
	/**
	 *
	 * @return boolean
	 */
	////////////////////////////////////////////////////////
	public static boolean getAutoCompress()
	{
		return AUTO_COMPRESS;
	}
	////////////////////////////////////////////////////////
	/**
	 *
	 * @param os OutputStream
	 * @param bCompress boolean
	 * @throws IOException
	 */
	////////////////////////////////////////////////////////
	public void store(OutputStream os,boolean bCompress) throws IOException
	{
		// Store content to buffer
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStream osUsed = null;
		if(bCompress)
			osUsed = new DeflaterOutputStream(bos);
		else
			osUsed = bos;
		Object[] objParameterName = getData().keySet().toArray();
		int iParameterCount = objParameterName.length;
		for(int iParameterIndex = 0;iParameterIndex < iParameterCount;iParameterIndex++)
		{
			// Write parameter name
			byte[] btName = objParameterName[iParameterIndex].toString().getBytes(ENCODE);
			osUsed.write(intToByteArray(btName.length));
			osUsed.write(btName);

			// Write parameter value
			Object objValue = getData().get(objParameterName[iParameterIndex]);
			storeValue(osUsed,objValue);
		}
		osUsed.close();
		bos.close();

		// Write to stream
		synchronized(os)
		{
			os.write(SIGN_SYMBOL.getBytes());
			if(bCompress)
				os.write(COMPRESSED_BUFFER);
			else
				os.write(PLAIN_BUFFER);
			os.write(intToByteArray(iParameterCount));
			os.write(intToByteArray(bos.size()));
			os.write(bos.toByteArray());
		}
	}
	////////////////////////////////////////////////////////
	/**
	 * Store parameter to ouput stream
	 * @param os OutputStream
	 * @throws IOException
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public void store(OutputStream os) throws IOException
	{
		store(os,AUTO_COMPRESS);
	}
	////////////////////////////////////////////////////////
	/**
	 * Store parameter value to ouput stream
	 * @param os OutputStream
	 * @param objValue Object
	 * @throws IOException
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	private void storeValue(OutputStream os,Object objValue) throws IOException
	{
		// Store value to buffer
		if(objValue instanceof Vector)
		{
			// Write signature
			os.write(VECTOR_SYMBOL);

			// Write content
			if(objValue == null)
				os.write(0x80);
			else
			{
				Vector vtValue = (Vector)objValue;
				os.write(intToByteArray(vtValue.size()));
				for(int iIndex = 0;iIndex < vtValue.size();iIndex++)
					storeValue(os,vtValue.elementAt(iIndex));
			}
		}
		else if(objValue instanceof byte[])
		{
			// Write signature
			os.write(BINARY_SYMBOL);

			// Write content
			if(objValue == null)
				os.write(0x80);
			else
			{
				// Create content buffer
				byte[] bt = (byte[])objValue;

				// Write content to stream
				os.write(intToByteArray(bt.length));
				os.write(bt);
			}
		}
		else
		{
			// Write signature
			os.write(STRING_SYMBOL);

			// Write content
			if(objValue == null)
				os.write(0x80);
			else
			{
				// Create content buffer
				byte[] bt = objValue.toString().getBytes(ENCODE);

				// Write content to stream
				os.write(intToByteArray(bt.length));
				os.write(bt);
			}
		}
	}
	/////////////////////////////////////////////////////////////////
	/**
	 *
	 * @param is InputStream
	 * @return int
	 * @throws IOException
	 */
	/////////////////////////////////////////////////////////////////
	private static int readBufferSize(InputStream is) throws IOException
	{
		int iResult = 0;
		int iCount = 0;
		int iByteRead = is.read();
		while(iByteRead >= 0)
		{
			if(iCount > 3 && ((iByteRead & 0x7F) > 0xF))
				throw new IOException("Value out of range");
			else
				iCount++;
			iResult = (iResult << 7) | (iByteRead & 0x7F);
			if(iByteRead > 127)
				break;
			iByteRead = is.read();
		}
		if(iByteRead < 0)
			throw new IOException("No data to read");
		return iResult;
	}
	/////////////////////////////////////////////////////////////////
	/**
	 *
	 * @param i int
	 * @return byte[]
	 */
	/////////////////////////////////////////////////////////////////
	private static byte[] intToByteArray(int i)
	{
		byte[] bt = null;
		if(i > 0xFE00000)
		{
			bt = new byte[5];
			bt[4] = (byte)(0x80 | (0x7F & i));
			i >>>= 7;
			bt[3] = (byte)(0x7F & i);
			i >>>= 7;
			bt[2] = (byte)(0x7F & i);
			i >>>= 7;
			bt[1] = (byte)(0x7F & i);
			i >>>= 7;
			bt[0] = (byte)(0x7F & i);
			i >>>= 7;
		}
		else if(i > 0x1FC000)
		{
			bt = new byte[4];
			bt[3] = (byte)(0x80 | (0x7F & i));
			i >>>= 7;
			bt[2] = (byte)(0x7F & i);
			i >>>= 7;
			bt[1] = (byte)(0x7F & i);
			i >>>= 7;
			bt[0] = (byte)(0x7F & i);
		}
		else if(i > 0x3F80)
		{
			bt = new byte[3];
			bt[2] = (byte)(0x80 | (0x7F & i));
			i >>>= 7;
			bt[1] = (byte)(0x7F & i);
			i >>>= 7;
			bt[0] = (byte)(0x7F & i);
		}
		else if(i > 0x7F)
		{
			bt = new byte[2];
			bt[1] = (byte)(0x80 | (0x7F & i));
			i >>>= 7;
			bt[0] = (byte)(0x7F & i);
		}
		else
		{
			bt = new byte[1];
			bt[0] = (byte)(0x80 | (0x7F & i));
		}
		return bt;
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Remove parameter from parameter list
	 * @param paramName Name of parameter
	 * @author Nguyen Truong Giang - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void removeValue(String paramName)
	{
		getData().remove(paramName);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Clear all objects and data
	 * @author Thai Hoang Hiep - Date: 27/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void clear()
	{
		getData().clear();
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Return parameter list
	 * @return Hashtable
	 * @author Thai Hoang Hiep - Date: 27/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public Hashtable getData()
	{
		return mprtParameter;
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Get object value
	 * @param paramName Name of parameter
	 * @return Value of parameter
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public Object getObject(String paramName)
	{
		return getData().get(paramName);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Add or update new object
	 * @param paramName Name of parameter
	 * @param obj Value of parameter
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setObject(String paramName,Object obj)
	{
		getData().remove(paramName);
		if(obj != null)
			getData().put(paramName,obj);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Get parameter value
	 * @param paramName Name of parameter
	 * @return String value
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public byte[] getByteArray(String paramName)
	{
		Object obj = getObject(paramName);
		if(obj == null)
			return null;
		if(obj instanceof byte[])
			return (byte[])obj;
		return obj.toString().getBytes();
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Add or update new string value into object
	 * @param paramName Name of parameter
	 * @param btValue byte[] Value of parameter
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setByteArray(String paramName,byte[] btValue)
	{
		getData().remove(paramName);
		if(btValue != null)
			getData().put(paramName,btValue);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Get parameter value
	 * @param paramName Name of parameter
	 * @return String value
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public String getString(String paramName)
	{
		Object obj = getObject(paramName);
		if(obj == null)
			return null;
		if(obj instanceof byte[])
			return new String((byte[])obj);
		return obj.toString();
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Add or update new string value into object
	 * @param paramName Name of parameter
	 * @param strValue Value of parameter
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setString(String paramName,String strValue)
	{
		getData().remove(paramName);
		if(strValue != null)
			getData().put(paramName,strValue);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Get vector parameter
	 * @param paramName Name of parameter
	 * @return Vector of value
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public Vector getVector(String paramName)
	{
		Object obj = getObject(paramName);
		if(obj == null || !(obj instanceof Vector))
			return null;
		return (Vector)obj;
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Add or update vector parameter
	 * @param paramName Name of parameter
	 * @param vtValue Vector of value
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setVector(String paramName,Vector vtValue)
	{
		getData().remove(paramName);
		if(vtValue != null)
			getData().put(paramName,vtValue);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Get request id
	 * @return String
	 * @author Dang Dinh Trung - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public String getRequestID()
	{
		return StringUtil.nvl(getString(REQUEST_ID_FIELD_NAME),"");
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Set request id
	 * @param strRequestID String
	 * @author Dang Dinh Trung - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setRequestID(String strRequestID)
	{
		setString(REQUEST_ID_FIELD_NAME,strRequestID);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Get reponse id
	 * @return String
	 * @author Dang Dinh Trung - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public String getResponseID()
	{
		return StringUtil.nvl(getString(RESPONSE_ID_FIELD_NAME),"");
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * set reponse id
	 * @param strResponseID String
	 * @author Dang Dinh Trung - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setResponseID(String strResponseID)
	{
		setString(RESPONSE_ID_FIELD_NAME,strResponseID);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Get return
	 * @return Object
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public Object getReturn()
	{
		return getObject(RETURN_FIELD_NAME);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * set return
	 * @param objValue Object
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setReturn(Object objValue)
	{
		setObject(RETURN_FIELD_NAME,objValue);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Get return
	 * @return AppException
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public AppException getException()
	{
		String strDescription = getString(ERROR_DESCRIPTION_FIELD_NAME);
		if(strDescription == null)
			return null;
		String strContext = getString(ERROR_CONTEXT_FIELD_NAME);
		String strInfo = getString(ERROR_INFO_FIELD_NAME);
		return new AppException(strDescription,strContext,strInfo);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * set return
	 * @param e Exception
	 * @author Thai Hoang Hiep - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setException(Exception e)
	{
		if(e instanceof AppException)
		{
			setString(ERROR_DESCRIPTION_FIELD_NAME,((AppException)e).getReason());
			setString(ERROR_CONTEXT_FIELD_NAME,((AppException)e).getContext());
			setString(ERROR_INFO_FIELD_NAME,((AppException)e).getInfo());
		}
		else
			setString(ERROR_DESCRIPTION_FIELD_NAME,e.getMessage());
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns function name which is sent by client
	 * @return String
	 * @author Nguyen Truong Giang - Date: 13/05/2003
	 */
	/////////////////////////////////////////////////////////////////
	public String getFunctionName()
	{
		return StringUtil.nvl(getString(FUNCTION_FIELD_NAME),"");
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Set function name
	 * @param strFunctionName String
	 * @author Nguyen Truong Giang - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setFunctionName(String strFunctionName)
	{
		setString(FUNCTION_FIELD_NAME,strFunctionName);
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Get class name which was passed by server and client
	 * @return String
	 * @author Dang Dinh Trung - Date: 27/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public String getClassName()
	{
		return StringUtil.nvl(getString(CLASS_FIELD_NAME),"");
	}
	/////////////////////////////////////////////////////////////////
	/**
	 * Set class name
	 * @param strClass String
	 * @author Dang Dinh Trung - Date: 26/09/2003
	 */
	/////////////////////////////////////////////////////////////////
	public void setClassName(String strClass)
	{
		setString(CLASS_FIELD_NAME,strClass);
	}
}
