package com.fss.ddtp;

import java.lang.reflect.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: FPT</p>
 * @author Thai Hoang Hiep
 * @version 1.0
 */

public class Processor
{
	////////////////////////////////////////////////////////
	/**
	 * Call function specified by ddtp request
	 * @param objCaller Object
	 * @param request DDTP contain request data
	 * @return response data
	 * @throws Exception
	 * @author Thai Hoang Hiep
	 */
	////////////////////////////////////////////////////////
	public static DDTP processRequest(Object objCaller,DDTP request) throws Exception
	{
		// Get class name & create class instance
		String strClassName = request.getClassName();
		if(strClassName.length() == 0)
			throw new Exception("Class name was not passed");
		Class cls = Class.forName(strClassName);
		Object obj = cls.newInstance();

		// Check class
		if(!(obj instanceof ProcessorStorage))
			throw new Exception("Class '" + strClassName + "' must be a ProcessorStorage");
		ProcessorStorage storage = (ProcessorStorage)obj;

		// Get function name and method
		String strFunctionName = request.getFunctionName();
		if(strFunctionName.length() == 0)
			throw new Exception("Function name was not passed");

		// Get method from class name and function name
		Method method = cls.getMethod(strFunctionName,null);
		if(method == null)
			throw new Exception("Function '" + strClassName + "." + strFunctionName + "' was not declared");

		// Check function
		if(Modifier.isAbstract(method.getModifiers()))
			throw new Exception("Function '" + strClassName + "." + strFunctionName + "' was not implemented");
		if(!Modifier.isPublic(method.getModifiers()))
			throw new Exception("Function '" + strClassName + "." + strFunctionName + "' is not public");

		// Invoke function
		storage.setCaller(objCaller);
		storage.setRequest(request);
		try
		{
			storage.prepareProcess();
			obj = method.invoke(storage,null);
			storage.processCompleted();
		}
		catch(InvocationTargetException e)
		{
			storage.processFailed();
			if(e.getTargetException() instanceof Exception)
				throw (Exception)e.getTargetException();
			throw new Exception(e.getTargetException());
		}

		// Response
		DDTP response = storage.getResponse();
		response.setReturn(obj);
		return response;
	}
}
