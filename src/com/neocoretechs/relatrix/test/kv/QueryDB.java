package com.neocoretechs.relatrix.test.kv;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.neocoretechs.relatrix.Relatrix;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021
 *
 */
public class QueryDB {
	public static boolean DEBUG = false;
	static int recs = 1;
	/**
	* Dump key/value store
	*/
	public static void main(String[] argv) throws Exception {
		Relatrix.getInstance();
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				dump1();
			} catch(Exception e) {
				e.printStackTrace();
			}
		});
		System.out.println("Dump COMPLETE.");
		System.exit(0);
	}
	/**
	 * dumps on keys
	 * @param argv
	 * @throws Exception
	 */
	public static void dump1() throws Exception {
		long tims = System.currentTimeMillis();
		/*
		Relatrix.entrySetStream(clazz).forEach(e-> {
			System.out.printf("%d=%s, %s | %s, %s%n",recs++, 
			((Map.Entry<?, ?>)e).getKey().getClass().getName(),
			((Map.Entry<?, ?>)e).getKey(),
			((Map.Entry<?, ?>)e).getValue() == null ? "NULL" : ((Map.Entry<?, ?>)e).getValue().getClass().getName(),
			((Map.Entry<?, ?>)e).getValue() == null ? "NULL" : ((Map.Entry<?, ?>)e).getValue());
		});
		*/
		Iterator<?> it = Relatrix.findSet('*','*','*');
		while(it.hasNext()) {
			Result e = (Result) it.next();
			System.out.printf("%d.) %s%n",recs++, e.get());
		}
		System.out.println("Dump in "+(System.currentTimeMillis()-tims)+" ms. retrieved "+recs+" records");
	}
		
}
