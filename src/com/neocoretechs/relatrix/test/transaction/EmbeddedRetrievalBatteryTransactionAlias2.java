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

import com.neocoretechs.rocksack.TransactionId;
import com.neocoretechs.relatrix.AbstractRelation.displayLevels;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;
import com.neocoretechs.rocksack.Alias;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findHeadSet retrieval for alias functionality in a transaction context.
 * NOTES:
 * optional arguments are [ [init] [max nnn] ]
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021,2025
 *
 */
public class EmbeddedRetrievalBatteryTransactionAlias2 {
	public static boolean DEBUG = false;
	public static boolean DISPLAY = false;
	public static boolean DISPLAYALL = true;
	public static int displayLinesOn[]= {0,1000,99900};
	public static int displayLinesOff[]= {100,1100,99999};
	public static int displayLine = 0;
	public static int displayLineCtr = 0;
	public static long displayTimer = 0;
	public static int min = 0;
	public static int max = 100;
	static String key = "This is a test"; 
	static String uniqKeyFmt = "%0100d";
	private static int SAMPLESIZE = 5;
	static Alias alias1 = new Alias("ALIAS1");
	static Alias alias2 = new Alias("ALIAS2");
	static Alias alias3 = new Alias("ALIAS3");
	private static TransactionId xid;
	private static long timx;
	private static int i;
	/**
	*/
	public static void main(String[] argv) throws Exception {
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.VERBOSE;
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				RelatrixTransaction.getInstance();	
				RelatrixTransaction.setAlias(alias1,RelatrixTransaction.getTableSpace()+alias1);
				RelatrixTransaction.setAlias(alias2,RelatrixTransaction.getTableSpace()+alias2);
				RelatrixTransaction.setAlias(alias3,RelatrixTransaction.getTableSpace()+alias3);
				AbstractRelation.displayLevel = displayLevels.VERBOSE;
				xid = RelatrixTransaction.getTransactionId();
				if(argv.length > 0 && argv[0].equals("max")) {
					System.out.println("Setting max items to "+argv[1]);
					max = Integer.parseInt(argv[1]);
				} else {
					if(argv.length > 0 && argv[0].equals("init")) {
						System.out.println("Initialize database to zero items, then terminate...");
						battery1AR17(alias1, xid);
						battery1AR17(alias2, xid);
						battery1AR17(alias3, xid);
						System.exit(0);
					}
				}
				if(RelatrixTransaction.size(alias1, xid) == 0) {
					if(DEBUG)
						System.out.println("Zero items, Begin insertion from "+min+" to "+max);
					battery0(alias1, xid);
					battery0(alias2, xid);
					battery0(alias3, xid);
				}
				battery1(alias1, xid);
				battery1(alias2, xid);
				battery1(alias3, xid);
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
	 * @param alias12 
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery0(Alias alias12, TransactionId xid2) throws Exception {
		System.out.println(xid2+" Battery0 "+alias12);
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		Relation dmr = null;
		for(int i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				dmr = RelatrixTransaction.store(alias12, xid2, fkey, "Has unit "+alias12, Long.valueOf(i));
				++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		RelatrixTransaction.commit(alias12, xid2);
		System.out.println("BATTERY0 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}

	/**
	 * @param argv
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery1(Alias alias12, TransactionId xid2) throws Exception {
		System.out.println(xid2+" Iterator Battery1 "+alias12);
		long tims = System.currentTimeMillis();
		// this list will store an object used to test subsequent queries where a named object is needed
		// it will be extracted from the wildcard queries
		ArrayList<Result> ar = new ArrayList<Result>();

		Iterator<?> it = null;
		System.out.println("Wildcard queries:");
		displayLine = 0;
		System.out.println("1.) FindHeadset("+alias12+",xid,*,*,*,String.class, String.class, Long.class)...");
		it =  RelatrixTransaction.findHeadSet(alias12,xid2, '*', '*', '*',String.class, String.class, Long.class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
		}
		displayLine = 0;
		System.out.println("2.) FindHeadset("+alias12+",xid,*,*,?,String.class, String.class, Long.class)...");		
		it = RelatrixTransaction.findHeadSet(alias12,xid2, '*', '*', '*',String.class, String.class, Long.class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			if(ar.size() < SAMPLESIZE ) {
				ar.add(c);
			}
		}

		System.out.println("----------\r\nAbove are wildcard permutations. Now retrieve those with object references using the");
		System.out.println("wildcard results. Recall headset is strictly less than 'to' element...");
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			System.out.println("9."+j+") FindHeadSet("+alias12+",xid2,<obj>,<obj>,<obj>) using domain="+ar.get(j).getDomain()+",map="+ar.get(j).getMap()+",range="+ar.get(j).getRange());
			it = RelatrixTransaction.findHeadSet(alias12,xid2, ar.get(j).getDomain(), ar.get(j).getMap(), ar.get(j).getRange());
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("10."+j+") FindHeadSet("+alias12+",xid,*,*,<obj>,String.class, String.class) using range="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, '*', '*', ar.get(j).getDomain(), String.class, String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("11."+j+") FindHeadSet("+alias12+",xid,*,<obj>,*, String.class, Long.class) using map="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, '*', ar.get(j).getDomain(), '*',String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("12."+j+") FindHeadSet("+alias12+",xid,<obj>,*,*,String.class, Long.class) using domain="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, ar.get(j).getDomain(), '*', '*',String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			System.out.println("13."+j+") FindHeadSet("+alias12+",xid,*,<obj>,<obj>,String.class) using map="+ar.get(j).getDomain()+" range="+ar.get(j).getMap());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, '*', ar.get(j).getDomain(), ar.get(j).getMap(), String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine = 0;
			System.out.println("14."+j+") FindHeadSet("+alias12+",xid,<obj>,*,<obj>,String.class) using domain="+ar.get(j).getDomain()+", range="+ar.get(j).getMap());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, ar.get(j).getDomain(), '*', ar.get(j).getMap(), String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("15."+j+") FindHeadSet("+alias12+",xid,<obj>,<obj>,*, Long.class) using domain="+ar.get(j).getDomain()+", map="+ar.get(j).getMap());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, ar.get(j).getDomain(), ar.get(j).getMap(), '*', Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("16."+j+") FindHeadSet("+alias12+",xid2,?,?,<obj>, String.class, String.class) using range="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, '*', '*', ar.get(j).getDomain(), String.class, String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			System.out.println("17."+j+") FindHeadSet("+alias12+",xid,?,<obj>,?, String.class, Long.class) using map="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, '*', ar.get(j).getDomain(), '*', String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			System.out.println("18."+j+") FindHeadSet("+alias12+",xid,<obj>,?,?, String.class, Long.class) using domain="+ar.get(j).getDomain());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, ar.get(j).getDomain(), '*', '*', String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("19."+j+") FindHeadSet("+alias12+",xid,?,<obj>,<obj>, String.class) using map="+ar.get(j).getDomain()+" range="+ar.get(j).getMap());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, '*', ar.get(j).getDomain(), ar.get(j).getMap(), String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("20."+j+") FindHeadSet("+alias12+",xid,<obj>,?,<obj>,String.class) using domain="+ar.get(j).getDomain()+" range="+ ar.get(j).getMap());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, ar.get(j).getDomain(), '*', ar.get(j).getMap(), String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("21."+j+") FindHeadSet("+alias12+",xid,<obj>,<obj>,?,Long.class) using domain="+ar.get(j).getDomain()+" map="+ar.get(j).getMap());		
			it = RelatrixTransaction.findHeadSet(alias12,xid2, ar.get(j).getDomain(), ar.get(j).getMap(), '*',Long.class);
			//ar = new ArrayList<Comparable>();
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
	 * @param xid2 
	 * @throws Exception
	 */
	public static void battery1AR17(Alias alias12, TransactionId xid2) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println(alias12+" CleanDB DMR size="+RelatrixTransaction.size(alias12,xid2,Relation.class)+" xid:"+xid2);
		System.out.println("CleanDB DRM size="+RelatrixTransaction.size(alias12,xid2,DomainRangeMap.class));
		System.out.println("CleanDB MDR size="+RelatrixTransaction.size(alias12,xid2,MapDomainRange.class));
		System.out.println("CleanDB MDR size="+RelatrixTransaction.size(alias12,xid2,MapRangeDomain.class));
		System.out.println("CleanDB RDM size="+RelatrixTransaction.size(alias12,xid2,RangeDomainMap.class));
		System.out.println("CleanDB RMD size="+RelatrixTransaction.size(alias12,xid2,RangeMapDomain.class));
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		Iterator<?> it = RelatrixTransaction.findSet(alias12,xid2,'*','*','*');
		timx = System.currentTimeMillis();
		it.forEachRemaining(fkey-> {
			Relation dmr = (Relation)((Result)fkey).get(0);
			try {
				RelatrixTransaction.remove(alias12,xid2,dmr);
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
