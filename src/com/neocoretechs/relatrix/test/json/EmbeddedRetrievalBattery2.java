package com.neocoretechs.relatrix.test.json;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.DomainRangeMap;
import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.MapDomainRange;
import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.RangeDomainMap;
import com.neocoretechs.relatrix.RangeMapDomain;
import com.neocoretechs.relatrix.RelatrixJson;
import com.neocoretechs.relatrix.RelatrixKVJson;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.Result2;
import com.neocoretechs.relatrix.Result3;
import com.neocoretechs.relatrix.AbstractRelation.displayLevels;


/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findHeadSet retrieval.
 * NOTES:
 * program arguments are _database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021
 *
 */
public class EmbeddedRetrievalBattery2 {
	public static boolean DEBUG = true;
	public static boolean DISPLAY = true;
	public static boolean DISPLAYALL = true;
	static String x =     "{\"timestamp\":1779166000301,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String x50k =  "{\"timestamp\":1779166050000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String x75k =  "{\"timestamp\":1779166075000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	static String xfull = "{\"timestamp\":1779749659999,\"LeftImage\":[{ \"count\":1, \"detections\":[ { \"name\":\"toilet\", \"probability\":0.35266665,  \"bbox\":{\"xmin\":288,\"ymin\":289,\"xmax\":320,\"ymax\":390} } ] } ], \"RightImage\":[{ \"count\":1, \"detections\":[ { \"name\":\"toilet\", \"probability\":0.29021525, \"bbox\":{\"xmin\":282,\"ymin\":289,\"xmax\":315,\"ymax\":391} } ] } ]}";

	static int numDelete = 100; // for delete test
	static int i = 0;
	private static long timx;
	public static int displayLinesOn[]= {0,1000,99900};
	public static int displayLinesOff[]= {100,1100,99999};
	public static int min = 0;
	public static int max = 100;
	static String key = "This is a test"; 
	static String uniqKeyFmt = "%0100d";
	private static int SAMPLESIZE = 5;
	static JSONObject xf = new JSONObject(xfull);
	static JSONObject jo2 = new JSONObject(x50k);
	static JSONObject jo = new JSONObject(x);
	static Class<?> xfClass, joClass, jo2Class;
	private static int displayLine;
	private static int displayLineCtr;
	private static long displayTimer;
	
	/**
	*/
	public static void main(String[] argv) throws Exception {
		 //System.out.println("Analysis of all");
		RelatrixJson.setTablespace(argv[0]);
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.VERBOSE;
		xfClass = RelatrixKVJson.getClassType(xf);
		jo2Class = RelatrixKVJson.getClassType(jo2);
		joClass = RelatrixKVJson.getClassType(jo);
		if(argv.length > 2 && argv[1].equals("max")) {
			System.out.println("Setting max items to "+argv[2]);
			max = Integer.parseInt(argv[2]);
		} else {
			if(argv.length > 1 && argv[1].equals("init")) {
				System.out.println("Initialize database to zero items, then terminate...");
				battery1AR17(argv);
				System.exit(0);
			}
		}
		long siz = RelatrixJson.size();
		if(siz == 0) {
			if(DEBUG)
				System.out.println("Zero items, Begin insertion from "+min+" to "+max);
			battery0(argv);
		} else
			System.out.println("size="+siz);
		battery1(argv);
		System.out.println("TEST BATTERY COMPLETE.");	
		System.exit(1);
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
	 * @param argv
	 * @throws Exception
	 */
	public static void battery0(String[] argv) throws Exception {
		System.out.println("Battery0 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		Relation dmr = null;
		JSONObject jox = new JSONObject(x);
		for(; i < max; i++) {
			try {
				long tim = jox.getLong("timestamp");
				++tim;
				jox.put("timestamp",tim);
				tim = jo2.getLong("timestamp");
				++tim;
				jo2.put("timestamp",tim);
				RelatrixJson.store(jox, xf, jo2);
				++recs;
				if((System.currentTimeMillis()-tims) > 1000) {
					System.out.println("storing "+recs+" "+jox);
					tims = System.currentTimeMillis();
				}
			} catch(DuplicateKeyException dke) { 
				++dupes; 
			}
		}
		 System.out.println("BATTERY0 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}

	/**
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1(String[] argv) throws Exception {
		System.out.println("Iterator Battery1 ");
		String fmap;
		long tims = System.currentTimeMillis();
		int recs = 0;
		// this list will store an object used to test subsequent queries where a named object is needed
		// it will be extracted from the wildcard queries
		ArrayList<Comparable> ar = new ArrayList<Comparable>();
		ArrayList<Comparable> ad = new ArrayList<Comparable>();
		ArrayList<Comparable> am = new ArrayList<Comparable>();
		ArrayList<Comparable> ar2 = new ArrayList<Comparable>(); // will store 2 element result sets map range
		ArrayList<Comparable> ar2dm = new ArrayList<Comparable>(); // will store 2 element result sets domain map
		ArrayList<Comparable> ar2dr = new ArrayList<Comparable>(); // will store 2 element result sets domain range
		ArrayList<Comparable> ar3 = new ArrayList<Comparable>(); // will store 3 element result sets
		Iterator<?> itx = null;
		System.out.println("Wildcard queries:");
		displayLine = 0;
		System.out.println("1.) FindHeadset(*,*,*,"+joClass+","+xfClass+","+jo2Class+")...");
		itx =  RelatrixJson.findHeadSet('*', '*', '*',joClass, xfClass, jo2Class);
		Iterator<?> it = RelatrixKVJson.getStringIterator(itx);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
		}
		displayLine = 0;
		System.out.println("2.) FindHeadset(*,*,?,"+joClass+","+xfClass+","+jo2Class+")...");		
		itx = RelatrixJson.findHeadSet('*', '*', '?',joClass, xfClass, jo2Class);
		it = RelatrixKVJson.getStringIterator(itx);
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
		displayLine = 0;
		System.out.println("3.) FindHeadSet(*,?,*,"+joClass+","+xfClass+","+jo2Class+")...");		
		itx = RelatrixJson.findHeadSet('*', '?', '*',joClass, xfClass, jo2Class);
		it = RelatrixKVJson.getStringIterator(itx);
		while(it.hasNext()) {
			Object o = it.next();
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			if(am.size() < SAMPLESIZE ) {
				am.add(c);
			}
		}
		displayLine = 0;
		System.out.println("4.) FindHeadSet(?,*,*."+joClass+","+xfClass+","+jo2Class+")...");		
		itx = RelatrixJson.findHeadSet('?', '*', '*',joClass, xfClass, jo2Class);
		it = RelatrixKVJson.getStringIterator(itx);
		while(it.hasNext()) {
			Object o = it.next();
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			if(ad.size() < SAMPLESIZE) {
				ad.add(c);
			}
		}
		displayLine=0;
		System.out.println("5.) FindHeadSet(*,?,?,"+joClass+","+xfClass+","+jo2Class+")...");		
		itx = RelatrixJson.findHeadSet('*', '?', '?',joClass, xfClass, jo2Class);
		it = RelatrixKVJson.getStringIterator(itx);
		while(it.hasNext()) {
			Object o = it.next();
			Result2 c = (Result2)o; // result2
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			if(ar2.size() < SAMPLESIZE) {
				ar2.add(c);
			}
		}
		displayLine = 0;
		System.out.println("6.) FindHeadSet(?,*,?,"+joClass+","+xfClass+","+jo2Class+")...");		
		itx = RelatrixJson.findHeadSet('?', '*', '?',joClass, xfClass, jo2Class);
		it = RelatrixKVJson.getStringIterator(itx);
		while(it.hasNext()) {
			Object o = it.next();
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			if(ar2dr.size() < SAMPLESIZE) {
				ar2dr.add(c);
			}
		}
		displayLine = 0;
		System.out.println("7.) FindHeadSet(?,?,*,"+joClass+","+xfClass+","+jo2Class+")...");		
		itx = RelatrixJson.findHeadSet('?', '?', '*',joClass, xfClass, jo2Class);
		it = RelatrixKVJson.getStringIterator(itx);
		while(it.hasNext()) {
			Object o = it.next();
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			if(ar2dm.size() < SAMPLESIZE) {
				ar2dm.add(c);
			}

		}
		displayLine = 0;
		System.out.println("8.) FindHeadSet(?,?,?,"+joClass+","+xfClass+","+jo2Class+")...");		
		itx = RelatrixJson.findHeadSet('?', '?', '?',joClass, xfClass, jo2Class);
		it = RelatrixKVJson.getStringIterator(itx);
		while(it.hasNext()) {
			Object o = it.next();
			Result3 c = (Result3)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+c);
			if(ar3.size() < SAMPLESIZE) {
				ar3.add(c);
			}
		}
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("8."+j+") FindHeadSet(?,?,?,<obj>,<obj>,<obj>) using domain="+((Result)ar3.get(j)).get(0)+",map="+((Result)ar3.get(j)).get(1)+",range="+((Result)ar3.get(j)).get(2));
			itx = RelatrixJson.findHeadSet('?','?','?',((Result)ar3.get(j)).get(0), ((Result)ar3.get(j)).get(1), ((Result)ar3.get(j)).get(2));
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("Should retrieve none, since range is specified as String and we only stored Long...");
			System.out.println("8A."+j+") FindHeadSet(?,*,*,<obj>,"+joClass+","+xfClass+","+") using domain="+((Result)ar3.get(j)).get(0));		
			itx = RelatrixJson.findHeadSet('?','*', '*', ((Result)ar3.get(j)).get(0), xfClass, jo2Class);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		System.out.println("----------\r\nAbove are wildcard permutations. Now retrieve those with object references using the");
		System.out.println("wildcard results. Recall headset is strictly less than 'to' element...");
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("9."+j+") FindHeadSet(<obj>,<obj>,<obj>) using domain="+((Result)ar3.get(j)).get(0)+",map="+((Result)ar3.get(j)).get(1)+",range="+((Result)ar3.get(j)).get(2));
			itx = RelatrixJson.findHeadSet(((Result)ar3.get(j)).get(0), ((Result)ar3.get(j)).get(1), ((Result)ar3.get(j)).get(2));
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("10."+j+") FindHeadSet(*,*,<obj>,"+joClass+","+xfClass+","+") using range="+((Result)ar.get(j)).get(0));		
			itx = RelatrixJson.findHeadSet('*', '*', ((Result)ar.get(j)).get(0), joClass, xfClass);
			it = RelatrixKVJson.getStringIterator(itx);
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
			System.out.println("11."+j+") FindHeadSet(*,<obj>,*, "+xfClass+","+jo2Class+","+") using map="+((Result)am.get(j)).get(0));		
			itx = RelatrixJson.findHeadSet('*', ((Result)am.get(j)).get(0), '*',joClass, jo2Class);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("12."+j+") FindHeadSet(<obj>,*,*,"+xfClass+","+jo2Class+","+") using domain="+((Result)ad.get(j)).get(0));		
			itx = RelatrixJson.findHeadSet(((Result)ad.get(j)).get(0), '*', '*', xfClass, jo2Class);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		for(int j = 0; j < ar2.size(); j++) {
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			System.out.println("13."+j+") FindHeadSet(*,<obj>,<obj>,String.class) using map="+((Result)ar2.get(j)).toArray()[0]+" range="+((Result)ar2.get(j)).toArray()[1]);		
			itx = RelatrixJson.findHeadSet('*', ((Result)ar2.get(j)).toArray()[0], ((Result)ar2.get(j)).toArray()[1], joClass);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine = 0;
			System.out.println("14."+j+") FindHeadSet(<obj>,*,<obj>,String.class) using domain="+((Result)ar2dr.get(j)).toArray()[0]+", range="+((Result)ar2dr.get(j)).toArray()[1]);		
			itx = RelatrixJson.findHeadSet(((Result)ar2dr.get(j)).toArray()[0], '*', ((Result)ar2dr.get(j)).toArray()[1], xfClass);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		for(int j = 0; j < ar2.size(); j++) {
			displayLine=0;
			System.out.println("15."+j+") FindHeadSet(<obj>,<obj>,*, Long.class) using domain="+((Result)ar2dm.get(j)).toArray()[0]+", map="+((Result)ar2dm.get(j)).toArray()[1]);		
			itx = RelatrixJson.findHeadSet(((Result)ar2dm.get(j)).toArray()[0], ((Result)ar2dm.get(j)).toArray()[1], '*', jo2Class);
			it = RelatrixKVJson.getStringIterator(itx);
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
			System.out.println("16."+j+") FindHeadSet(?,?,<obj>, "+joClass+","+xfClass+","+") using range="+((Result)ar.get(j)).get(0));		
			itx = RelatrixJson.findHeadSet('?', '?', ((Result)ar.get(j)).get(0), joClass, xfClass);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			System.out.println("17."+j+") FindHeadSet(?,<obj>,?, "+xfClass+","+jo2Class+","+") using map="+((Result)am.get(j)).get(0));		
			itx = RelatrixJson.findHeadSet('?', ((Result)am.get(j)).get(0), '?', xfClass, jo2Class);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result2 c = (Result2)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine=0;
			System.out.println("18."+j+") FindHeadSet(<obj>,?,?,"+xfClass+","+jo2Class+","+") using domain="+((Result)ad.get(j)).get(0));		
			itx = RelatrixJson.findHeadSet(((Result)ad.get(j)).get(0), '?', '?', xfClass, jo2Class);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
		}
		for(int j = 0; j < ar2.size(); j++) {
			displayLine=0;
			System.out.println("19."+j+") FindHeadSet(?,<obj>,<obj>, String.class) using map="+((Result)ar2.get(j)).get(0)+" range="+((Result)ar2.get(j)).get(1));		
			itx = RelatrixJson.findHeadSet('?', ((Result)ar2.get(j)).get(0), ((Result)ar2.get(j)).get(1), joClass);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("20."+j+") FindHeadSet(<obj>,?,<obj>,String.class) using domain="+((Result)ar2dr.get(j)).get(0)+" range="+ ((Result)ar2dr.get(j)).get(1));		
			itx = RelatrixJson.findHeadSet(((Result)ar2dr.get(j)).get(0), '?', ((Result)ar2dr.get(j)).get(1), jo2Class);
			it = RelatrixKVJson.getStringIterator(itx);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+c);
			}
			displayLine =0;
			System.out.println("21."+j+") FindHeadSet(<obj>,<obj>,?,Long.class) using domain="+((Result)ar2dm.get(j)).get(0)+" map="+((Result)ar2dm.get(j)).get(1));		
			itx = RelatrixJson.findHeadSet(((Result)ar2dm.get(j)).get(0), ((Result)ar2dm.get(j)).get(1), '?',jo2Class);
			it = RelatrixKVJson.getStringIterator(itx);
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
	 * remove entries
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR17(String[] argv) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("CleanDB");
		Iterator it = RelatrixJson.findSet('*','*','*');
		long timx = System.currentTimeMillis();
		int i = 0;
		while(it.hasNext()) {
			Object fkey = it.next();
			Relation dmr = (Relation)((Result)fkey).get(0);
			RelatrixJson.remove(dmr.getDomain(), dmr.getMap());
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" "+fkey);
				timx = System.currentTimeMillis();
			}
		}
		Iterator<?> its = RelatrixJson.findSet('*','*','*');
		while(its.hasNext()) {
			Result nex = (Result) its.next();
			//System.out.println(i+"="+nex);
			System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
		}
		long siz = RelatrixJson.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKVJson.entrySet(Relation.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("Relation:"+nex);
		}
		siz = RelatrixJson.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKVJson.entrySet(DomainRangeMap.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("DomainRangeMap:"+nex);
		}
		siz = RelatrixJson.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = RelatrixKVJson.entrySet(MapDomainRange.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("MapDomainRange:"+nex);
		}
		siz = RelatrixKVJson.size(MapDomainRange.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = RelatrixKVJson.entrySet(MapRangeDomain.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("MapRangeDomain:"+nex);
		}
		siz = RelatrixKVJson.size(MapRangeDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKVJson.entrySet(RangeDomainMap.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("RangeDomainMap:"+nex);
		}
		siz = RelatrixKVJson.size(RangeDomainMap.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKVJson.entrySet(RangeMapDomain.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("RangeMapDomain:"+nex);
		}
		siz = RelatrixKVJson.size(RangeMapDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}/*
		it = RelatrixKV.entrySet(DBKey.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("DBKey:"+nex);
		}
		siz = RelatrixKV.size(DBKey.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(Long.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("Long:"+nex);
		}
		siz = RelatrixKV.size(Long.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(String.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("String:"+nex);
		}
		siz = RelatrixKV.size(String.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 String MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 String MISMATCH:"+siz+" > 0 after delete/commit");
		}
		*/
		System.out.println("BATTERY1AR17 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms.");
	}


}
