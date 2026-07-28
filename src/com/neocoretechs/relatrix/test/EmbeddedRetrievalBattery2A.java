package com.neocoretechs.relatrix.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.Relatrix;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findHeadSet retrieval.
 * NOTES:
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021
 *
 */
public class EmbeddedRetrievalBattery2A {
	public static boolean DEBUG = false;
	public static boolean DISPLAY = false;
	public static boolean DISPLAYALL = true;
	public static int displayLinesOn[]= {0,1000,4500,9900};
	public static int displayLinesOff[]= {100,1100,5100,9999};
	public static int displayLine = 0;
	public static int displayLineCtr = 0;
	public static long displayTimer = 0;
	public static int min = 0;
	public static int max = 100;
	static String key = "This is a test"; 
	static String uniqKeyFmt = "%0100d";
	private static int SAMPLESIZE = 50;

	/**
	*/
	public static void main(String[] argv) throws Exception {
		//System.out.println("Analysis of all");
		IndexResolver indexResolver = new IndexResolver();
		indexResolver.setLocal();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				Relatrix.getInstance();
				AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
				if(argv.length == 1 && argv[0].equals("init")) {
					battery1AR17();
				}
				if(Relatrix.size() == 0) {
					battery0();
				}
				battery1();
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
	 * @throws Exception
	 */
	public static void battery0() throws Exception {
		System.out.println("Battery0 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		Relation dmr = null;
		for(int i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			try {
				dmr = Relatrix.store(fkey, "Has unit", Long.valueOf(i));
				++recs;
			} catch(DuplicateKeyException dke) { ++dupes; }
		}
		 System.out.println("BATTERY0 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}

	/**
	 * @throws Exception
	 */
	public static void battery1() throws Exception {
		System.out.println("Iterator Battery1 ");
		String fmap;
		long tims = System.currentTimeMillis();
		int recs = 0;
		// this list will store an object used to test subsequent queries where a named object is needed
		// it will be extracted from the wildcard queries
		ArrayList<Result> ar3 = new ArrayList<Result>(); // will store 3 element result sets
		Iterator<?> it = null;
		System.out.println("Mixed Headset queries:");
		displayLine = 0;
		System.out.println("1.) Load test array with FindHeadset(*,*,*,String.class,String.class,Long.class) for "+SAMPLESIZE+" elements.");
		it =  Relatrix.findHeadSet('*', '*', '*',String.class, String.class, Long.class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			ar3.add(c);
			if(ar3.size() >= SAMPLESIZE)
				break;
		}
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("2."+j+") FindHeadSet(*,*,*,<obj>,<obj>,<obj>) using ="+((Relation)ar3.get(j).get()).getDomain()+" Has unit12345 "+((Relation)ar3.get(j).get()).getRange());
			it = Relatrix.findHeadSet('*','*','*',((Relation)ar3.get(j).get()).getDomain(),"Has unit12345",((Relation)ar3.get(j).get()).getRange());
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine = 0;
			System.out.println("3."+j+") FindHeadSet(*,*,*,<obj>,<obj>,<obj>) using =String.class,"+((Relation)ar3.get(j).get()).getMap()+","+((Relation)ar3.get(j).get()).getRange());
			it = Relatrix.findHeadSet('*','*','*',String.class,((Relation)ar3.get(j).get()).getMap(),((Relation)ar3.get(j).get()).getRange());
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
		}
		//------------------------------------------------------------------
		// same but with regions
		//
		System.out.println("----------");
		System.out.println("Repeat tests with region search range "+(max/2));
		displayLine = 0;
		System.out.println("4.) FindHeadset(*,*,*,String.class, String.class,"+(Long.valueOf(max/2))+");");
		it =  Relatrix.findHeadSet('*', '*', '*',String.class, String.class, Long.valueOf(max/2));
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			ar3.add(c);
		}
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("5."+j+") FindHeadSet(*,*,*,<obj>,<obj>,<obj>) using ="+((Relation)ar3.get(j).get()).getDomain()+","+((Relation)ar3.get(j).get()).getMap()+","+ Long.valueOf(max/2));
			it = Relatrix.findHeadSet('*','*','*',((Relation)ar3.get(j).get()).getDomain(),((Relation)ar3.get(j).get()).getMap(), Long.valueOf(max/2));
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}
	public static void battery1AR17() throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("CleanDB");
		Iterator it = Relatrix.findSet('*','*','*');
		long timx = System.currentTimeMillis();
		int i = 0;
		while(it.hasNext()) {
			Object fkey = it.next();
			Relation dmr = (Relation)((Result)fkey).get(0);
			Relatrix.remove(dmr.getDomain(), dmr.getMap());
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		Iterator<?> its = Relatrix.findSet('*','*','*');
		while(its.hasNext()) {
			Result nex = (Result) its.next();
			//System.out.println(i+"="+nex);
			System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
		}
		long siz = Relatrix.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
	}

}
