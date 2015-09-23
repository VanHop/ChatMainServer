package com.fis.chat.server.thread;

import java.io.File;
import java.io.PrintStream;
import java.sql.Connection;

import com.fss.dictionary.Dictionary;
import com.fss.sql.OracleConnectionFactory;
import com.fss.thread.ProcessorListener;
import com.fss.thread.ThreadManager;
import com.fss.thread.ThreadProcessor;
import com.fss.util.FileUtil;
import com.fss.util.LogOutputStream;
/**
 * <p>
 * Title: Balance Transfer
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2015
 * </p>
 * 
 * <p>
 * Company: FIS FTU
 * </p>
 * 
 * @author 
 * @version 1.0
 */
public class ChatManager implements ProcessorListener {

	public static final String CONFIG_FILE_NAME = "conf/ServerConfig.txt";
	private static Dictionary mdc;

	private OracleConnectionFactory mPool = null;

	/**
	 * getConnection
	 * 
	 * @return Connection
	 * @throws Exception
	 * @todo Implement this com.fss.thread.ProcessorListener method
	 */
	public Connection getConnection() throws Exception {
		return mPool.getConnection();
	}

	/**
	 * onCreate
	 * 
	 * @param threadProcessor
	 *            ThreadProcessor
	 * @throws Exception
	 * @todo Implement this com.fss.thread.ProcessorListener method
	 */
	public void onCreate(ThreadProcessor threadProcessor) throws Exception {
	}

	/**
	 * onOpen
	 * 
	 * @param threadProcessor
	 *            ThreadProcessor
	 * @throws Exception
	 * @todo Implement this com.fss.thread.ProcessorListener method
	 */
	public void onOpen(ThreadProcessor threadProcessor) throws Exception {
		threadProcessor.mcnMain = getConnection();
	}

	public void setPool(OracleConnectionFactory pool) {
		mPool = pool;
	}

	public static void main(String[] argv) throws Exception {
		try {
			// Change system output to file
			ChatManager lsn = new ChatManager();
			String strWorkingDir = System.getProperty("user.dir");
			if (strWorkingDir == null || strWorkingDir.equals("")) {
				strWorkingDir = System.getProperty("user.dir");
			}
			if (!strWorkingDir.endsWith("/") || !strWorkingDir.endsWith("\\")) {
				strWorkingDir += "/";
			}

			// loadServerConfig(strConfigFilePath);
			mdc = new Dictionary(CONFIG_FILE_NAME);

			String strLogFile = mdc.getString("LogFile");
			String strOutputFile = mdc.getString("OutputFile");
			String strLogDir = strWorkingDir + mdc.getString("LogDir");

			if (!strLogDir.endsWith("/") || !strLogDir.endsWith("\\")) {
				strLogDir += "/";
			}
			File f = new File(strLogDir + strLogFile);
			FileUtil.forceFolderExist(f.getParent());

			int iLogKeepDay = Integer.parseInt(mdc.getString("KeepLogFileOnDay"));
			int iMaxFileToSave = Integer.parseInt(mdc.getString("MaxFileLogSave"));

			PrintStream ps = new PrintStream(new LogOutputStream(strLogDir + strLogFile));
			PrintStream psOutput = new PrintStream(new LogOutputStream(strLogDir + strOutputFile));
			int iConnectPoolSize = Integer.parseInt(mdc.getString("ConnectionPoolSize"));
			OracleConnectionFactory pool = new OracleConnectionFactory(mdc.getString("Url"), mdc.getString("UserName"),
					mdc.getString("Password"), iConnectPoolSize);
			lsn.setPool(pool);
			System.setOut(psOutput);
			System.setErr(ps);

			// Start manager
			ServerManager cs = new ServerManager(Integer.parseInt(mdc.getString("PortID")), lsn);

			cs.setLoadingMethod(ServerManager.LOAD_FROM_FILE);
			cs.start();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	public String getParameter(String string) {
		return "";
	}
}
