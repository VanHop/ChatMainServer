package com.fis.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fss.ddtp.DDTP;

public class HashMapResponse {

	private Map<Long, DDTP> responses;
	private AtomicInteger miMapSize = new AtomicInteger();
	private Object mutex;
	
	public HashMapResponse(){
		responses = new LinkedHashMap<Long, DDTP>();
		mutex = this;
	}
	
	public void put(long id, DDTP response){
		synchronized (responses) {
			responses.put(id, response);
		}
		miMapSize.incrementAndGet();
		synchronized (mutex) {
			mutex.notify();
		}
	}
	public DDTP getResponse(int index){
		if(responses==null)
			return null;
		else if(index < 0 || index >= miMapSize.intValue())
			return null;
		DDTP ddtpReturn = null;
		synchronized (responses) {
			ddtpReturn = (new ArrayList<DDTP>(responses.values())).get(index);
		}
		return ddtpReturn;
	}
	public DDTP getResponse(String idResponse){
		DDTP response = null;
		Long id = Long.parseLong(idResponse);
		synchronized (responses) {
			response = responses.get(id);
		}
		if(response != null){
			miMapSize.decrementAndGet();
			synchronized (responses) {
				responses.remove(id);
			}
			return response;
		} else {
			for(int i = 0 ; i < 30 ; i++){
				synchronized (mutex) {
					try {
						mutex.wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				synchronized (responses) {
					response = responses.get(id);
				}
				if(response != null){
					miMapSize.decrementAndGet();
					synchronized (responses) {
						responses.remove(id);
					}
					return response;
				}
			}
		}
		return null;
	}
}
