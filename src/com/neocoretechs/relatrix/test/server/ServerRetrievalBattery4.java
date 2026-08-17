package com.neocoretechs.relatrix.test.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.DomainRangeMap;
import com.neocoretechs.relatrix.MapDomainRange;
import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.RangeDomainMap;
import com.neocoretechs.relatrix.RangeMapDomain;
import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.Result1;
import com.neocoretechs.relatrix.client.RelatrixClient;



/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findSubSet retrieval. We will let our samplesize be dictated by hi and low range values.
 * Provides a persistent collection iterator of keys 'from' element inclusive, 'to' element exclusive of the keys specified.
 * NOTES:
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021,2024
 *
 */
public class ServerRetrievalBattery4 {
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
	public static int div = 10;
	static long lorange = (max/20L);
	static long hirange = (max/10L);
	static Long lo = (long) min;
	static Long hi = (long) max/10;
	static Long increment = 10L;
	static String key = "This is a test"; 
	static String uniqKeyFmt = "%0100d";
	private static RelatrixClient rkvc ;

	/**
	*/
	public static void main(String[] argv) throws Exception {
		System.out.println("Subset Provides a persistent collection iterator of keys 'from' element inclusive, 'to' element exclusive of the keys specified");
		if(argv.length < 2) {
			System.out.println("Usage: <remoteNode> <remotePort> [init]");
		}
		rkvc = new RelatrixClient(argv[0], Integer.parseInt(argv[1]) );
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		if(argv.length == 3 && argv[2].equals("init")) {
			battery1AR17();
		}
		if(rkvc.size() == 0) {
			battery0();
		}
		battery1();

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
			dmr = rkvc.store(fkey, "Has unit", Long.valueOf(i));
			++recs;
		}
		 System.out.println("BATTERY0 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}

	/**
	 * @throws Exception
	 */
	public static void battery1() throws Exception {
		System.out.println("Iterator Battery1 ");
		long tims = System.currentTimeMillis();
		// this list will store an object used to test subsequent queries where a named object is needed
		// it will be extracted from the wildcard queries
		ArrayList<Result> ar = new ArrayList<Result>(); // range
		ArrayList<Result> am = new ArrayList<Result>(); // map
		ArrayList<Result> ad = new ArrayList<Result>(); // domain
		ArrayList<Result> ar2dr = new ArrayList<Result>(); // domain,range
		Iterator<?> it = null;
		Iterator<?> it2 = null;
		System.out.println("Wildcard queries:");
		displayLine = 0;

		System.out.println("1.) findSubSet(*,*,*,String.class, String.class,"+lo+","+hi+");");
		it =  rkvc.findSubSet('*', '*', '*',String.class, String.class, lo,hi);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			//ar.add(c[0]);
		}
		displayLine = 0;
		System.out.println("2.) findSubSet(*,*,*,String.class, String.class, "+lo+","+hi+");");	
		it = rkvc.findSubSet('*', '*', '*',String.class, String.class, lo, hi);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			// samplesize is dictated by hi and low range
			ar.add(c);
		}
		displayLine = 0;
		String slo = key + String.format(uniqKeyFmt, lo);
		String shi = key  + String.format(uniqKeyFmt, hi);
		System.out.println("3.) findSubset(*,*,*,"+slo+","+shi+", String.class, Long.class);");		
		it = rkvc.findSubSet('*', '*', '*',slo,shi, String.class, Long.class);
		while(it.hasNext()) {
			Object o = it.next();
			Result  c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			am.add(c);
		}
		displayLine = 0;
		System.out.println("4.) findSubSet(*,*,*.String.class, String.class, "+lo+","+hi+");");			
		it = rkvc.findSubSet('*', '*', '*',String.class, String.class, lo, hi);
		while(it.hasNext()) {
			Object o = it.next();
			Result  c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			ad.add(c);
		}

		displayLine = 0;
		System.out.println("6.) findSubSet(*,*,*,"+slo+","+shi+",String.class, "+lo+","+hi+")...");		
		it = rkvc.findSubSet('*', '*', '*',slo,shi, String.class, lo,hi);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			ar2dr.add(c);
		}
	
		it = null;
		System.out.println("----------");
		System.out.println("Above are all the wildcard permutations. Now retrieve those with object references using the wildcard results.");
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			Comparable[] ar3c = ((Result)(ar.get(j))).toArray();
			System.out.println("9."+j+") findSubSet(<obj>,<obj>,<obj>) using ="+
					ar3c[0]+",("+ar3c[0].getClass().getName()+"),"+
					ar3c[1]+",("+ar3c[1].getClass().getName()+"),"+
					ar3c[2]+",("+ar3c[2].getClass().getName());
			if(it != null)
				rkvc.setIterator(it);
			it = rkvc.findSubSet(ar3c[0],ar3c[1],ar3c[2]);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("10."+j+") findSubSet(*,*,<obj>,String.class, String.class) using range="+((Relation)(((Result)ar.get(j)).get())).getRange());
			if(it2 != null)
				rkvc.setIterator(it2);
			it2 = rkvc.findSubSet('*', '*', ((Relation)(((Result)ar.get(j)).get())).getRange(), String.class, String.class);
			while(it2.hasNext()) {
				Object o = it2.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("----------");
		System.out.println("Begin 1 instance match 2 wildcard testing");
		it = null;
		it2 = null;
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("11."+j+") findSubSet(*,<obj>,*, String.class, Long.class) using map="+((Relation)(((Result)ar.get(j)).get())).getMap());
			if(it != null)
				rkvc.setIterator(it);
			it = rkvc.findSubSet('*', ((Relation)(((Result)ar.get(j)).get())).getMap(), '*',String.class, Long.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("12."+j+") FindSubset(<obj>,*,*,String.class, Long.class) using domain="+((Relation)(((Result)ar.get(j)).get())).getDomain());
			if(it2 != null)
				rkvc.setIterator(it2);
			it2 = rkvc.findSubSet(((Relation)(((Result)ar.get(j)).get())).getDomain(), '*', '*', String.class, Long.class);
			while(it2.hasNext()) {
				Object o = it2.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("----------");
		System.out.println("Begin 2 instance match 1 wildcard testing");
		it = null;
		it2 = null;
		for(int j = 0; j < ar.size(); j++) {
			// From a Result1 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			Comparable[] arc = ((Result)ar.get(j)).toArray();
			System.out.println("13."+j+") findSubSet(*,<obj>,<obj>,String.class) using map="+arc[1]+" range="+arc[2]);
			if(it != null)
				rkvc.setIterator(it);
			it = rkvc.findSubSet('*', arc[1], arc[2], String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine = 0;
			System.out.println("14."+j+") findSubSet(<obj>,*,<obj>,String.class) using ="+arc[0]+", "+arc[2]);
			if(it2 != null)
				rkvc.setIterator(it2);
			it2 = rkvc.findSubSet(arc[0], '*', arc[2], String.class);
			while(it2.hasNext()) {
				Object o = it2.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("15."+j+") findSubSet(<obj>,<obj>,*, Long.class) using domain="+arc[0]+", map="+arc[1]);		
			it = rkvc.findSubSet(arc[0], arc[1], '*',Long.class);
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
		it = null;
		it2 = null;
		Iterator<?> it3 = null;
		for(int j = 0; j < ar.size(); j++) {
			displayLine =0;
			Comparable[] arc = ((Result)ar.get(j)).toArray();
			System.out.println("16."+j+") findSubSet(*,*,<obj>, String.class, String.class) using range="+arc[2]);
			if(it != null)
				rkvc.setIterator(it);
			it = rkvc.findSubSet('*', '*', arc[2], String.class, String.class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("17."+j+") findSubSet(*,<obj>,*, String.class, Long.class) using map="+arc[1]);
			if(it2 != null)
				rkvc.setIterator(it2);
			it2 = rkvc.findSubSet('*', arc[1], '*', String.class, Long.class);
			while(it2.hasNext()) {
				Object o = it2.next();
				Result1 c = (Result1)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("18."+j+") findSubSet(<obj>,*,*, String.class, Long.class) using domain="+arc[0]);
			if(it3 != null)
				rkvc.setIterator(it3);
			it3 = rkvc.findSubSet(arc[0], '*', '*', String.class, Long.class);
			while(it3.hasNext()) {
				Object o = it3.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("----------");

		//
		// proceed with hi/lo range tests
		//
		System.out.println("----------");
		System.out.println("Begin hi/lo range testing");
		it = null;
		it2 = null;
		it3 = null;
		for(int j = 0; j < ar2dr.size(); j++) {
			lo = lorange;
			hi = hirange;
			displayLine =0;
			Comparable[] arc = ((Result)ar2dr.get(j)).toArray();
			System.out.println("22."+j+") findSubSet(*,*,*,<class>,<class>,<obj>,<obj>) using domain="+arc[0].getClass()+" map="+arc[1].getClass()+" range="+lo+" to "+hi);	
			if(it != null)
				rkvc.setIterator(it);
			it = rkvc.findSubSet('*','*','*',arc[0].getClass(), arc[1].getClass(),lo,hi);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			lo+=increment;
			hi+=increment;
			System.out.println("23."+j+") findSubSet(*,*,*,<class>,<class>,<obj>,<obj>) using domain="+arc[0].getClass()+" map="+arc[1].getClass()+" range="+lo+" to "+hi);
			if(it2 != null)
				rkvc.setIterator(it2);
			it2 = rkvc.findSubSet('*','*','*',arc[0].getClass(), arc[1].getClass(),lo,hi);
			while(it2.hasNext()) {
				Object o = it2.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			lo+=increment;
			hi+=increment;
			System.out.println("24."+j+") findSubSet(*,*,*,<class>,<class>,<obj>,<obj>) using domain="+arc[0].getClass()+" map="+arc[1].getClass()+" range="+lo+" to "+hi);
			if(it3 != null)
				rkvc.setIterator(it3);
			it3 = rkvc.findSubSet('*','*','*',arc[0].getClass(), arc[1].getClass(),lo,hi);
			while(it3.hasNext()) {
				Object o = it3.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims));
	}
	/**
	 * remove entries
	 * @throws Exception
	 */
	public static void battery1AR17() throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("CleanDB");
		Iterator<?> it = rkvc.findSet('*','*','*');
		long timx = System.currentTimeMillis();
		int i = 0;
		while(it.hasNext()) {
			Object fkey = it.next();
			Relation dmr = (Relation)((Result)fkey).get(0);
			rkvc.remove(dmr.getDomain(), dmr.getMap());
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		Iterator<?> its = rkvc.findSet('*','*','*');
		while(its.hasNext()) {
			Result nex = (Result) its.next();
			//System.out.println(i+"="+nex);
			System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
		}
		long siz = rkvc.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = rkvc.entrySet(Relation.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("Relation:"+nex);
		}
		siz = rkvc.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = rkvc.entrySet(DomainRangeMap.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("DomainRangeMap:"+nex);
		}
		siz = rkvc.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = rkvc.entrySet(MapDomainRange.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("MapDomainRange:"+nex);
		}
		siz = rkvc.size(MapDomainRange.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = rkvc.entrySet(MapRangeDomain.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("MapRangeDomain:"+nex);
		}
		siz = rkvc.size(MapRangeDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = rkvc.entrySet(RangeDomainMap.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("RangeDomainMap:"+nex);
		}
		siz = rkvc.size(RangeDomainMap.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = rkvc.entrySet(RangeMapDomain.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("RangeMapDomain:"+nex);
		}
		siz = rkvc.size(RangeMapDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}/*
		it = rkvc.entrySet(DBKey.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("DBKey:"+nex);
		}
		siz = rkvc.size(DBKey.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = rkvc.entrySet(Long.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("Long:"+nex);
		}
		siz = rkvc.size(Long.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = rkvc.entrySet(String.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("String:"+nex);
		}
		siz = rkvc.size(String.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 String MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 String MISMATCH:"+siz+" > 0 after delete/commit");
		}
		*/
		System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}


}
