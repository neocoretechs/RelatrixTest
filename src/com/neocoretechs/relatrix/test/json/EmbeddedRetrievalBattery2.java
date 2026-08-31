package com.neocoretechs.relatrix.test.json;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

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

import com.neocoretechs.relatrix.AbstractRelation.displayLevels;
import com.neocoretechs.relatrix.key.IndexResolver;
import com.neocoretechs.relatrix.parallel.ExecutionContextHolder;
import com.neocoretechs.relatrix.parallel.ParallelExecutionContext;


/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findHeadSet Json retrieval.
 * NOTES:
 * program arguments are _database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2026
 *
 */
public class EmbeddedRetrievalBattery2 {
	public static boolean DEBUG = true;
	public static boolean DISPLAY = true;
	public static boolean DISPLAYALL = true;
	//static String x =     "{\"timestamp\":1779166000301,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	//static String x50k =  "{\"timestamp\":1779166050000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	//static String x75k =  "{\"timestamp\":1779166075000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\",\"probability\":0.41232753,\"bbox\":{\"xmin\":104,\"ymin\":12,\"xmax\":223,\"ymax\":561} } ] } ], \"RightImage\":[{\"count\":0, \"detections\":[ ] } ]}";
	//static String xfull = "{\"timestamp\":1779749659999,\"LeftImage\":[{ \"count\":1, \"detections\":[ { \"name\":\"toilet\", \"probability\":0.35266665,  \"bbox\":{\"xmin\":288,\"ymin\":289,\"xmax\":320,\"ymax\":390} } ] } ], \"RightImage\":[{ \"count\":1, \"detections\":[ { \"name\":\"toilet\", \"probability\":0.29021525, \"bbox\":{\"xmin\":282,\"ymin\":289,\"xmax\":315,\"ymax\":391} } ] } ]}";
	
	static String x =     "{\"timestamp\":1779166030000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\"}]}]}";
	static String x50k =  "{\"timestamp\":1779166050000,\"RightImage\":[{\"count\":0, \"affections\":[ {\"name\":\"alligator\"}]}]}";
	static String xfull = "{\"timestamp\":1779166070000,\"LeftImage\":[{ \"count\":1, \"erections\":[ { \"name\":\"toilet\"}]}]}";

	static int numDelete = 100; // for delete test
	static int i = 0;
	private static long timx;
	public static int displayLinesOn[]= {0,1000,99900};
	public static int displayLinesOff[]= {100,1100,99999};
	public static int min = 0;
	public static int max = 100;
	
	private static int SAMPLESIZE = 5;
	static JSONObject xf = new JSONObject(xfull);
	static JSONObject xo50 = new JSONObject(x50k);
	static JSONObject xo = new JSONObject(x);
	static Class<?> xfClass, xoClass, xo50Class;
	private static int displayLine;
	private static int displayLineCtr;
	private static long displayTimer;
	
	/**
	*/
	public static void main(String[] argv) throws Exception {
		//System.out.println("Analysis of all");
		RelatrixJson.getInstance();
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.VERBOSE;
		IndexResolver indexResolver = new IndexResolver();
		ParallelExecutionContext pec = new ParallelExecutionContext(indexResolver, new ConcurrentHashMap<String,Object>());
		ScopedValue.where(ExecutionContextHolder.CONTEXT, pec).run(() -> {
			try {
				xfClass = RelatrixKVJson.getClassType(xf);
				xo50Class = RelatrixKVJson.getClassType(xo50);
				xoClass = RelatrixKVJson.getClassType(xo);
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
				battery2(argv);
			}catch(Exception e) {
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
	 * @param argv
	 * @throws Exception
	 */
	public static void battery0(String[] argv) throws Exception {
		System.out.println("Battery0 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		Relation dmr = null;
		JSONObject xox = new JSONObject(x);
		JSONObject xfx = new JSONObject(xfull);
		JSONObject x50x = new JSONObject(x50k);
		for(; i < max; i++) {
			try {
				long tim = xox.getLong("timestamp");
				++tim;
				xox.put("timestamp",tim);
				tim = xfx.getLong("timestamp");
				++tim;
				xfx.put("timestamp",tim);
				tim = x50x.getLong("timestamp");
				++tim;
				x50x.put("timestamp",tim);
				dmr = RelatrixJson.store(xox, xfx, x50x);
				if(dmr == null)
					throw new RuntimeException("Result of store yielded null relation");
				++recs;
				if((System.currentTimeMillis()-tims) > 1000) {
					System.out.println("storing "+recs+" "+xox);
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
		System.out.println("======================Json Iterator Battery1 =================================");
		long tims = System.currentTimeMillis();
		int recs = 0;
		// this list will store an object used to test subsequent queries where a named object is needed
		// it will be extracted from the wildcard queries
		ArrayList<Result> ar = new ArrayList<Result>();
		ArrayList<Comparable> ad = new ArrayList<Comparable>();
		ArrayList<Comparable> am = new ArrayList<Comparable>();
		ArrayList<Comparable> ar2 = new ArrayList<Comparable>(); // will store 2 element result sets map range
		ArrayList<Comparable> ar2dm = new ArrayList<Comparable>(); // will store 2 element result sets domain map
		ArrayList<Comparable> ar2dr = new ArrayList<Comparable>(); // will store 2 element result sets domain range
		ArrayList<Comparable> ar3 = new ArrayList<Comparable>(); // will store 3 element result sets
		System.out.println("Wildcard queries:");
		displayLine = 0;
		System.out.println("1.) FindHeadset(*,*,*,"+xoClass+","+xfClass+","+xo50Class+")...");
		Iterator<?> it =  RelatrixJson.findHeadSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
		}
		displayLine = 0;
		System.out.println("2.) FindHeadset(*,*,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findHeadSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			if(ar.size() < SAMPLESIZE ) {
				ar.add(c);
			}
		}
		displayLine = 0;
		System.out.println("3.) FindHeadSet(*,?,*,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findHeadSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			if(am.size() < SAMPLESIZE ) {
				am.add(c);
			}
		}
		displayLine = 0;
		System.out.println("4.) FindHeadSet(?,*,*."+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findHeadSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			if(ad.size() < SAMPLESIZE) {
				ad.add(c);
			}
		}
		displayLine=0;
		System.out.println("5.) FindHeadSet(*,?,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findHeadSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o; // result2
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			if(ar.size() < SAMPLESIZE) {
				ar2.add(c);
			}
		}
		displayLine = 0;
		System.out.println("6.) FindHeadSet(?,*,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findHeadSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			if(ar2dr.size() < SAMPLESIZE) {
				ar2dr.add(c);
			}
		}
		displayLine = 0;
		System.out.println("7.) FindHeadSet(?,?,*,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findHeadSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			if(ar2dm.size() < SAMPLESIZE) {
				ar2dm.add(c);
			}

		}
		displayLine = 0;
		System.out.println("8.) FindHeadSet(?,?,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findHeadSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			if(ar3.size() < SAMPLESIZE) {
				ar3.add(c);
			}
		}
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("8."+j+") FindHeadSet(?,?,?,<obj>,<obj>,<obj>) using domain="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					",map="+
					RelatrixKVJson.getData(ar.get(j).getMap())+
					",range="+
					RelatrixKVJson.getData(ar.get(j).getRange()));
			it = RelatrixJson.findHeadSet('*','*','*',ar.get(j).getDomain(), ar.get(j).getMap(), ar.get(j).getRange());
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("8A."+j+") FindHeadSet(?,*,*,<obj>,"+xoClass+","+xfClass+","+") using domain="+RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findHeadSet('*','*', '*', ar.get(j).getDomain(), xfClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		System.out.println("----------\r\nAbove are wildcard permutations. Now retrieve those with object references using the");
		System.out.println("wildcard results. Recall headset is strictly less than 'to' element...");
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("9."+j+") FindHeadSet(<obj>,<obj>,<obj>) using domain="+				
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					",map="+
					RelatrixKVJson.getData(ar.get(j).getMap())+
					",range="+
					RelatrixKVJson.getData(ar.get(j).getRange()));
			it = RelatrixJson.findHeadSet(ar.get(j).getDomain(), ar.get(j).getMap(), ar.get(j).getRange());
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("10."+j+") FindHeadSet(*,*,<obj>,"+xoClass+","+xfClass+","+") using range="+RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findHeadSet('*', '*', ar.get(j).getDomain(), xoClass, xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("11."+j+") FindHeadSet(*,<obj>,*, "+xfClass+","+xo50Class+","+") using map="+RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findHeadSet('*', ar.get(j).getDomain(), '*',xoClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine =0;
			System.out.println("12."+j+") FindHeadSet(<obj>,*,*,"+xfClass+","+xo50Class+","+") using domain="+RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findHeadSet(ar.get(j).getDomain(), '*', '*', xfClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			System.out.println("13."+j+") FindHeadSet(*,<obj>,<obj>,"+xoClass+") using map="+
					RelatrixKVJson.getData(ar.get(j).getMap())+
					",range="+
					RelatrixKVJson.getData(ar.get(j).getRange()));	
			it = RelatrixJson.findHeadSet('*', ar.get(j).getDomain(), ar.get(j).getMap(), xoClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine = 0;
			System.out.println("14."+j+") FindHeadSet(<obj>,*,<obj>,+"+xfClass+") using domain="+RelatrixKVJson.getData(ar.get(j).getDomain())+
					", range="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findHeadSet(ar.get(j).getDomain(), '*', ar.get(j).getMap(), xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("15."+j+") FindHeadSet(<obj>,<obj>,*,"+xo50Class+") using domain="+RelatrixKVJson.getData(ar.get(j).getDomain())+
					", map="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findHeadSet(ar.get(j).getDomain(), ar.get(j).getMap(), '*', xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("16."+j+") FindHeadSet(?,?,<obj>, "+xoClass+","+xfClass+","+") using range="+RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findHeadSet('*', '*', ar.get(j).getDomain(), xoClass, xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine=0;
			System.out.println("17."+j+") FindHeadSet(?,<obj>,?, "+xfClass+","+xo50Class+","+") using map="+RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findHeadSet('*', ar.get(j).getDomain(), '*', xfClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine=0;
			System.out.println("18."+j+") FindHeadSet(<obj>,?,?,"+xfClass+","+xo50Class+","+") using domain="+RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findHeadSet(ar.get(j).getDomain(), '*', '*', xfClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("19."+j+") FindHeadSet(?,<obj>,<obj>,"+xoClass+") using map="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					" range="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findHeadSet('*', ar.get(j).getDomain(), ar.get(j).getMap(), xoClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o; 
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine =0;
			System.out.println("20."+j+") FindHeadSet(<obj>,?,<obj>,"+xo50Class+") using domain="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					" range="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findHeadSet(ar.get(j).getDomain(), '*', ar.get(j).getMap(), xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine =0;
			System.out.println("21."+j+") FindHeadSet(<obj>,<obj>,?,"+xo50Class+") using domain="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					" map="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findHeadSet(ar.get(j).getDomain(), ar.get(j).getMap(), '*',xo50Class);
			//ar = new ArrayList<Comparable>();
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims));
	}
	/**
	 * findTailSet
	 * @param argv
	 * @throws Exception
	 */
	public static void battery2(String[] argv) throws Exception {
		System.out.println("===================== Json Iterator Battery2 ===================== ");
		long tims = System.currentTimeMillis();
		int recs = 0;
		// this list will store an object used to test subsequent queries where a named object is needed
		// it will be extracted from the wildcard queries
		ArrayList<Result> ar = new ArrayList<Result>();
		ArrayList<Comparable> ad = new ArrayList<Comparable>();
		ArrayList<Comparable> am = new ArrayList<Comparable>();
		ArrayList<Comparable> ar2 = new ArrayList<Comparable>(); // will store 2 element result sets map range
		ArrayList<Comparable> ar2dm = new ArrayList<Comparable>(); // will store 2 element result sets domain map
		ArrayList<Comparable> ar2dr = new ArrayList<Comparable>(); // will store 2 element result sets domain range
		ArrayList<Comparable> ar3 = new ArrayList<Comparable>(); // will store 3 element result sets
		Iterator<?> itx = null;
		System.out.println("Wildcard queries:");
		displayLine = 0;
		System.out.println("1.) findTailset(*,*,*,"+xoClass+","+xfClass+","+xo50Class+")...");
		Iterator<?> it =  RelatrixJson.findTailSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			Relation r = (Relation) c.get();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJson.getData(r.getDomain())+"->"+RelatrixKVJson.getData(r.getMap())+"->"+RelatrixKVJson.getData(r.getRange()));
		}
		displayLine = 0;
		System.out.println("2.) findTailSet(*,*,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findTailSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get()));
			if(ar.size() < SAMPLESIZE ) {
				ar.add(c);
			}
		}
		displayLine = 0;
		System.out.println("3.) findTailSet(*,?,*,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findTailSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get()));
			if(am.size() < SAMPLESIZE ) {
				am.add(c);
			}
		}
		displayLine = 0;
		System.out.println("4.) findTailSet(?,*,*."+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findTailSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get()));
			if(ad.size() < SAMPLESIZE) {
				ad.add(c);
			}
		}
		displayLine=0;
		System.out.println("5.) findTailSet(*,?,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findTailSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o; // result2
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0))+"->"+RelatrixKVJson.getData(c.get(1)));
			if(ar.size() < SAMPLESIZE) {
				ar2.add(c);
			}
		}
		displayLine = 0;
		System.out.println("6.) findTailSet(?,*,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findTailSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0))+"->"+RelatrixKVJson.getData(c.get(1)));
			if(ar2dr.size() < SAMPLESIZE) {
				ar2dr.add(c);
			}
		}
		displayLine = 0;
		System.out.println("7.) findTailSet(?,?,*,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findTailSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0))+"->"+RelatrixKVJson.getData(c.get(1)));
			if(ar2dm.size() < SAMPLESIZE) {
				ar2dm.add(c);
			}

		}
		displayLine = 0;
		System.out.println("8.) findTailSet(?,?,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJson.findTailSet('*', '*', '*',xoClass, xfClass, xo50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0))+"->"+RelatrixKVJson.getData(c.get(1))+"->"+RelatrixKVJson.getData(c.get(2)));
			if(ar3.size() < SAMPLESIZE) {
				ar3.add(c);
			}
		}
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("8."+j+") findTailSet(?,?,?,<obj>,<obj>,<obj>) using domain="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					",map="+
					RelatrixKVJson.getData(ar.get(j).getMap())+
					",range="+
					RelatrixKVJson.getData(ar.get(j).getRange()));
			it = RelatrixJson.findTailSet('*','*','*',ar.get(j).getDomain(), ar.get(j).getMap(), ar.get(j).getRange());
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0))+"->"+RelatrixKVJson.getData(c.get(1))+"->"+RelatrixKVJson.getData(c.get(2)));
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("8A."+j+") findTailSet(?,*,*,<obj>,"+xoClass+","+xfClass+","+") using domain="+RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findTailSet('*','*', '*', ar.get(j).getDomain(), xfClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0)));
			}
		}
		System.out.println("----------\r\nAbove are wildcard permutations. Now retrieve those with object references using the");
		System.out.println("wildcard results. Recall tailset is strictly greater or equal to 'from' element...");
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("9."+j+") findTailSet(<obj>,<obj>,<obj>) "+
					"using domain="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					",map="+
					RelatrixKVJson.getData(ar.get(j).getMap())+
					",range="+
					RelatrixKVJson.getData(ar.get(j).getRange()));
			it = RelatrixJson.findTailSet(ar.get(j).getDomain(), ar.get(j).getMap(), ar.get(j).getRange());
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("10."+j+") findTailSet(*,*,<obj>,"+xoClass+","+xfClass+","+") using range="+RelatrixKVJson.getData(ar.get(j).getDomain()));	
			it = RelatrixJson.findTailSet('*', '*', ar.get(j).getDomain(), xoClass, xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(r.getDomain())+"->"+RelatrixKVJson.getData(r.getMap())+"->"+RelatrixKVJson.getData(r.getRange()));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("11."+j+") findTailSet(*,<obj>,*, "+xfClass+","+xo50Class+","+") using map="+RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findTailSet('*', ar.get(j).getDomain(), '*',xoClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(r.getDomain())+"->"+RelatrixKVJson.getData(r.getMap())+"->"+RelatrixKVJson.getData(r.getRange()));
			}
			displayLine =0;
			System.out.println("12."+j+") findTailSet(<obj>,*,*,"+xfClass+","+xo50Class+","+") using domain="+RelatrixKVJson.getData(ar.get(j).getDomain()));	
			it = RelatrixJson.findTailSet(ar.get(j).getDomain(), '*', '*', xfClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(r.getDomain())+"->"+RelatrixKVJson.getData(r.getMap())+"->"+RelatrixKVJson.getData(r.getRange()));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			System.out.println("13."+j+") findTailSet(*,<obj>,<obj>,"+xoClass+") using map="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					" range="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findTailSet('*', ar.get(j).getDomain(), ar.get(j).getMap(), xoClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(r.getDomain())+"->"+RelatrixKVJson.getData(r.getMap())+"->"+RelatrixKVJson.getData(r.getRange()));
			}
			displayLine = 0;
			System.out.println("14."+j+") findTailSet(<obj>,*,<obj>,"+xfClass+") using domain="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					", range="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findTailSet(ar.get(j).getDomain(), '*', ar.get(j).getMap(), xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(r.getDomain())+"->"+RelatrixKVJson.getData(r.getMap())+"->"+RelatrixKVJson.getData(r.getRange()));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("15."+j+") findTailSet(<obj>,<obj>,*,"+xo50Class+") using domain="+
				RelatrixKVJson.getData(ar.get(j).getDomain())
				+", map="+
				RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findTailSet(ar.get(j).getDomain(), ar.get(j).getMap(), '*', xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(r.getDomain())+"->"+RelatrixKVJson.getData(r.getMap())+"->"+RelatrixKVJson.getData(r.getRange()));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("16."+j+") findTailSet(?,?,<obj>, "+xoClass+","+xfClass+","+") using range="+
					RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findTailSet('*', '*', ar.get(j).getDomain(), xoClass, xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0))+"->"+RelatrixKVJson.getData(c.get(1)));
			}
			displayLine=0;
			System.out.println("17."+j+") findTailSet(?,<obj>,?, "+xfClass+","+xo50Class+","+") using map="+
					RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findTailSet('*', ar.get(j).getDomain(), '*', xfClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0))+"->"+RelatrixKVJson.getData(c.get(1)));
			}
			displayLine=0;
			System.out.println("18."+j+") findTailSet(<obj>,?,?,"+xfClass+","+xo50Class+","+") using domain="+
					RelatrixKVJson.getData(ar.get(j).getDomain()));		
			it = RelatrixJson.findTailSet(ar.get(j).getDomain(), '*', '*', xfClass, xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0))+"->"+RelatrixKVJson.getData(c.get(1)));
			}
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("19."+j+") findTailSet(?,<obj>,<obj>,"+xoClass+") using map="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					" range="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findTailSet('*', ar.get(j).getDomain(), ar.get(j).getMap(), xoClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0)));
			}
			displayLine =0;
			System.out.println("20."+j+") findTailSet(<obj>,?,<obj>,"+xo50Class+") using domain="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					" range="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findTailSet(ar.get(j).getDomain(), '*', ar.get(j).getMap(), xo50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0)));
			}
			displayLine =0;
			System.out.println("21."+j+") findTailSet(<obj>,<obj>,?,"+xo50Class+") using domain="+
					RelatrixKVJson.getData(ar.get(j).getDomain())+
					" map="+
					RelatrixKVJson.getData(ar.get(j).getMap()));		
			it = RelatrixJson.findTailSet(ar.get(j).getDomain(), ar.get(j).getMap(), '*',xo50Class);
			//ar = new ArrayList<Comparable>();
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJson.getData(c.get(0)));
			}
		}
		System.out.println("BATTERY2 SUCCESS in "+(System.currentTimeMillis()-tims));
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
