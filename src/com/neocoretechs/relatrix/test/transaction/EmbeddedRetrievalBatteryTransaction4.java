package com.neocoretechs.relatrix.test.transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.DomainRangeMap;
import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.MapDomainRange;
import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.RangeDomainMap;
import com.neocoretechs.relatrix.RangeMapDomain;
import com.neocoretechs.relatrix.RelatrixTransaction;
import com.neocoretechs.relatrix.Result;

import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;
import com.neocoretechs.rocksack.TransactionId;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findSubSet retrieval in a transaction context. We will let our samplesize be dictated by hi and low range values
 * Provides a persistent collection iterator of keys 'from' element inclusive, 'to' element exclusive of the keys specified
 * NOTES:
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021,2024
 *
 */
public class EmbeddedRetrievalBatteryTransaction4 {
	public static boolean DEBUG = false;
	public static boolean DISPLAY = false;
	private static boolean DISPLAYALL = true;
	public static int displayLinesOn[]= {0,1000,5000,9990,15000,20000,30000,40000,50000,60000,70000,80000,90000,99000};
	public static int displayLinesOff[]= {100,1100,5100,9999,15999,20999,30999,40999,50999,60999,70999,80999,90999,100000};
	public static int displayLine = 0;
	public static int displayLineCtr = 0;
	public static long displayTimer = 0;
	public static int min = 0;
	public static int max = 100;
	static long lorange = (max/20L);
	static long hirange = (max/10L);
	static Long lo = (long) min;
	static Long hi = (long) max/10;
	static Long increment = 10L;
	static String key = "This is a test"; 
	static String uniqKeyFmt = "%0100d";
	private static long timx;
	private static int i;
	private static TransactionId xid;

	/**
	*/
	public static void main(String[] argv) throws Exception {
		System.out.println("Subset Provides a persistent collection iterator of keys 'from' element inclusive, 'to' element exclusive of the keys specified");
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				RelatrixTransaction.getInstance();
				AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
				xid = RelatrixTransaction.getTransactionId();
				if(argv.length == 1 && argv[0].equals("init")) {
					battery1AR17(xid);
				}
				if(RelatrixTransaction.size(xid) == 0) {
					battery0(xid);
				}
				battery1(xid);
				RelatrixTransaction.commit(xid);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		System.out.println("TEST BATTERY COMPLETE.");	
		System.exit(0);
	}
	
	public static void displayCtrl() {
		if(displayLine == 0)
			displayLineCtr = 0;
		if(displayLine >= displayLinesOn[displayLineCtr] && displayLine <= displayLinesOff[displayLineCtr]) {
			if(!DISPLAY)
				displayTimer = System.currentTimeMillis();
			DISPLAY = true;
		} else {
			if(DISPLAY)
				System.out.println("Time between lines:"+displayLinesOn[displayLineCtr]+" and "+displayLinesOff[displayLineCtr]+" is "+(System.currentTimeMillis()-displayTimer)+" ms.");
			DISPLAY = false;
			if(displayLine > displayLinesOff[displayLineCtr] && displayLineCtr < displayLinesOff.length-1)
				++displayLineCtr;
		}
		++displayLine;
	}
	/**
	 * Loads up on keys
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery0(TransactionId xid2) throws Exception {
		System.out.println("Battery0 "+xid2);
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		Relation dmr = null;
		for(int i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				dmr = RelatrixTransaction.store(xid2, fkey, "Has unit", Long.valueOf(i));
				++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		RelatrixTransaction.commit(xid2);
		System.out.println("BATTERY0 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}

	/**
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery1(TransactionId xid2) throws Exception {
		System.out.println("Iterator Battery1 "+xid2);
		long tims = System.currentTimeMillis();
		// this list will store an object used to test subsequent queries where a named object is needed
		// it will be extracted from the wildcard queries
		ArrayList<Result> ar = new ArrayList<Result>(); // range

		Iterator<?> it = null;
		System.out.println("Wildcard queries:");
		displayLine = 0;

		System.out.println("1.) findSubSet(xid,*,*,*,String.class, String.class,"+lo+","+hi+");");
		it =  RelatrixTransaction.findSubSet(xid2,'*', '*', '*',String.class, String.class, lo,hi);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			//ar.add(c[0]);
		}
		displayLine = 0;
		System.out.println("2.) findSubSet(xid,*,*,?,String.class, String.class, "+lo+","+hi+");");	
		it = RelatrixTransaction.findSubSet(xid2,'*', '*', '*',String.class, String.class, lo, hi);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			// samplesize is dictated by hi and low range
			ar.add(c);
		}
	
		System.out.println("----------");
		System.out.println("Begin 1 instance match 2 wildcard testing");
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("11."+j+") findSubSet(xid,*,<obj>,*, String.class, Long.class) using map="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findSubSet(xid2,'*', ar.get(j).getDomain(), '*',String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("12."+j+") FindSubset(xid,<obj>,*,*,String.class, Long.class) using domain="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findSubSet(xid2,ar.get(j).getDomain(), '*', '*', String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("----------");
		System.out.println("Begin 2 instance match 1 wildcard testing");
		for(int j = 0; j < ar.size(); j++) {
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			System.out.println("13."+j+") findSubSet(xid,*,<obj>,<obj>,String.class) using map="+ar.get(j).getDomain()+" range="+ar.get(j).getMap());		
			it = RelatrixTransaction.findSubSet(xid2,'*', ar.get(j).getDomain(), ar.get(j).getMap(), String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine = 0;
			System.out.println("14."+j+") findSubSet(xid,<obj>,*,<obj>,String.class) using ="+ar.get(j).getDomain()+", "+ar.get(j).getMap());		
			it = RelatrixTransaction.findSubSet(xid2,ar.get(j).getDomain(), '*', ar.get(j).getMap(), String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("15."+j+") findSubSet(xid,<obj>,<obj>,*, Long.class) using domain="+ar.get(j).getDomain()+", map="+ar.get(j).getMap());		
			it = RelatrixTransaction.findSubSet(xid2,ar.get(j).getDomain(), ar.get(j).getMap(), '*',Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("----------");
		System.out.println("Begin 1 instance match 2 element return testing");
		for(int j = 0; j < ar.size(); j++) {
			displayLine =0;
			System.out.println("16."+j+") findSubSet(xid,?,?,<obj>, String.class, String.class) using range="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findSubSet(xid2,'*', '*', ar.get(j).getDomain(), String.class, String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("17."+j+") findSubSet(xid,?,<obj>,?, String.class, Long.class) using map="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findSubSet(xid2,'*', ar.get(j).getDomain(), '*', String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("18."+j+") findSubSet(xid,<obj>,?,?, String.class, Long.class) using domain="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findSubSet(xid2,ar.get(j).getDomain(), '*', '*', String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("----------");
		System.out.println("Begin 2 instance match 1 element return testing");
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("19."+j+") findSubSet(xid,?,<obj>,<obj>, String.class) using map="+ar.get(j).getDomain()+" range="+ar.get(j).getMap());		
			it = RelatrixTransaction.findSubSet(xid2,'*', ar.get(j).getDomain(), ar.get(j).getMap(), String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			System.out.println("20."+j+") findSubSet(xid,<obj>,?,<obj>,String.class) using domain="+ar.get(j).getDomain()+" range="+ ar.get(j).getMap());		
			it = RelatrixTransaction.findSubSet(xid2,ar.get(j).getDomain(), '*', ar.get(j).getMap(), String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			System.out.println("21."+j+") findSubSet(xid,<obj>,<obj>,?,Long.class) using domain="+ar.get(j).getDomain()+" map="+ar.get(j).getMap());		
			it = RelatrixTransaction.findSubSet(xid2,ar.get(j).getDomain(), ar.get(j).getMap(), '*',Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		//
		// proceed with hi/lo range tests
		//
		System.out.println("----------");
		System.out.println("Begin hi/lo range testing");
		for(int j = 0; j < ar.size(); j++) {
			lo = lorange;
			hi = hirange;
			displayLine =0;
			System.out.println("22."+j+") findSubSet(xid,*,*,?,<class>,<class>,<obj>,<obj>) using domain="+ar.get(j).getDomain().getClass()+" map="+ar.get(j).getMap().getClass()+" range="+lo+" to "+hi);		
			it = RelatrixTransaction.findSubSet(xid2,'*','*','*',ar.get(j).getDomain().getClass(), ar.get(j).getMap().getClass(),lo,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			lo+=increment;
			hi+=increment;
			System.out.println("23."+j+") findSubSet(xid,?,?,?,<class>,<class>,<obj>,<obj>) using domain="+ar.get(j).getDomain().getClass()+" map="+ar.get(j).getMap().getClass()+" range="+lo+" to "+hi);		
			it = RelatrixTransaction.findSubSet(xid2,'*','*','*',ar.get(j).getDomain().getClass(), ar.get(j).getMap().getClass(),lo,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			lo+=increment;
			hi+=increment;
			System.out.println("24."+j+") findSubSet(xid,?,*,?,<class>,<class>,<obj>,<obj>) using domain="+ar.get(j).getDomain().getClass()+" map="+ar.get(j).getMap().getClass()+" range="+lo+" to "+hi);		
			it = RelatrixTransaction.findSubSet(xid2,'*','*','*',ar.get(j).getDomain().getClass(), ar.get(j).getMap().getClass(),lo,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims));
	}
	/**
	 * remove entries, all relationships should be recursively deleted
	 * @param alias12 
	 * @throws Exception
	 */
	public static void battery1AR17(TransactionId xid) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println(xid+" CleanDB DMR size="+RelatrixTransaction.size(xid,Relation.class));
		System.out.println("CleanDB DRM size="+RelatrixTransaction.size(xid,DomainRangeMap.class));
		System.out.println("CleanDB MDR size="+RelatrixTransaction.size(xid,MapDomainRange.class));
		System.out.println("CleanDB MDR size="+RelatrixTransaction.size(xid,MapRangeDomain.class));
		System.out.println("CleanDB RDM size="+RelatrixTransaction.size(xid,RangeDomainMap.class));
		System.out.println("CleanDB RMD size="+RelatrixTransaction.size(xid,RangeMapDomain.class));
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		Iterator<?> it = RelatrixTransaction.findSet(xid,'*','*','*');
		timx = System.currentTimeMillis();
		it.forEachRemaining(fkey-> {
			Relation dmr = (Relation)((Result)fkey).get(0);
			try {
				RelatrixTransaction.remove(xid,dmr);
			} catch (IllegalArgumentException | ClassNotFoundException | IllegalAccessException | IOException e) {
				throw new RuntimeException(e);
			}
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" total, current="+fkey);
				timx = System.currentTimeMillis();
			}
		});
		System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
}
