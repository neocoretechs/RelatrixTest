package com.neocoretechs.relatrix.test.json;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

import org.json.JSONObject;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.RelatrixJsonTransaction;
import com.neocoretechs.relatrix.RelatrixKVJsonTransaction;
import com.neocoretechs.relatrix.DomainRangeMap;
import com.neocoretechs.relatrix.DuplicateKeyException;
import com.neocoretechs.relatrix.MapDomainRange;
import com.neocoretechs.relatrix.MapRangeDomain;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.RangeDomainMap;
import com.neocoretechs.relatrix.RangeMapDomain;

import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.Result2;
import com.neocoretechs.relatrix.Result3;
import com.neocoretechs.relatrix.AbstractRelation.displayLevels;
import com.neocoretechs.rocksack.TransactionId;


/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findHeadStream Json retrieval. Transaction context.
 * NOTES:
 * program arguments are _database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2026
 *
 */
public class EmbeddedRetrievalBattery8 {
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
	private static TransactionId xid;
	/**
	*/
	public static void main(String[] argv) throws Exception {
		 //System.out.println("Analysis of all");
		RelatrixJsonTransaction.setTablespace(argv[0]);
		xid = RelatrixJsonTransaction.getTransactionId();
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.VERBOSE;
		xfClass = RelatrixKVJsonTransaction.getClassType(xf, xid);
		xo50Class = RelatrixKVJsonTransaction.getClassType(xo50, xid);
		xoClass = RelatrixKVJsonTransaction.getClassType(xo, xid);
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
		long siz = RelatrixJsonTransaction.size(xid);
		if(siz == 0) {
			if(DEBUG)
				System.out.println("Zero items, Begin insertion from "+min+" to "+max);
			battery0(argv);
		} else
			System.out.println("size="+siz);
		battery1(argv);
		battery2(argv);
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
				dmr = RelatrixJsonTransaction.store(xid, xox, xfx, x50x);
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
		System.out.println("======================Json Stream Battery1 =================================");
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
		System.out.println("Wildcard queries:");
		displayLine = 0;
		System.out.println("1.) findHeadStream(*,*,*,"+xoClass+","+xfClass+","+xo50Class+")...");
		Stream<?> it =  RelatrixJsonTransaction.findHeadStream(xid, '*', '*', '*',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
		});
		displayLine = 0;
		System.out.println("2.) findHeadStream(*,*,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findHeadStream(xid, '*', '*', '?',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			if(ar.size() < SAMPLESIZE ) {
				ar.add(c);
			}
		});
		displayLine = 0;
		System.out.println("3.) findHeadStream(*,?,*,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findHeadStream(xid, '*', '?', '*',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			if(am.size() < SAMPLESIZE ) {
				am.add(c);
			}
		});
		displayLine = 0;
		System.out.println("4.) findHeadStream(?,*,*."+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findHeadStream(xid, '?', '*', '*',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			if(ad.size() < SAMPLESIZE) {
				ad.add(c);
			}
		});
		displayLine=0;
		System.out.println("5.) findHeadStream(*,?,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findHeadStream(xid, '*', '?', '?',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result2 c = (Result2)o; // result2
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			if(ar2.size() < SAMPLESIZE) {
				ar2.add(c);
			}
		});
		displayLine = 0;
		System.out.println("6.) findHeadStream(?,*,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findHeadStream(xid, '?', '*', '?',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			if(ar2dr.size() < SAMPLESIZE) {
				ar2dr.add(c);
			}
		});
		displayLine = 0;
		System.out.println("7.) findHeadStream(?,?,*,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findHeadStream(xid, '?', '?', '*',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			if(ar2dm.size() < SAMPLESIZE) {
				ar2dm.add(c);
			}
		});
		displayLine = 0;
		System.out.println("8.) findHeadStream(?,?,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findHeadStream(xid, '?', '?', '?',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result3 c = (Result3)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			if(ar3.size() < SAMPLESIZE) {
				ar3.add(c);
			}
		});
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("8."+j+") findHeadStream(?,?,?,<obj>,<obj>,<obj>) using domain="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(0))+
					",map="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(1))+
					",range="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(2)));
			it = RelatrixJsonTransaction.findHeadStream(xid, '?','?','?',((Result)ar3.get(j)).get(0), ((Result)ar3.get(j)).get(1), ((Result)ar3.get(j)).get(2));
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine=0;
			//RelatrixHeadsetStream.DEBUG = true;
			System.out.println("8A."+j+") findHeadStream(?,*,*,<obj>,"+xoClass+","+xfClass+","+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, '?','*', '*', ((Result)ar3.get(j)).get(0), xfClass, xo50Class);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		System.out.println("----------\r\nAbove are wildcard permutations. Now retrieve those with object references using the");
		System.out.println("wildcard results. Recall headstream is strictly less than 'to' element...");
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("9."+j+") findHeadStream(<obj>,<obj>,<obj>) using domain="+				
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(0))+
					",map="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(1))+
					",range="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(2)));
			it = RelatrixJsonTransaction.findHeadStream(xid, ((Result)ar3.get(j)).get(0), ((Result)ar3.get(j)).get(1), ((Result)ar3.get(j)).get(2));
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine=0;
			//RelatrixHeadsetStream.DEBUG = true;
			System.out.println("10."+j+") findHeadStream(*,*,<obj>,"+xoClass+","+xfClass+","+") using range="+RelatrixKVJsonTransaction.getData(((Result)ar.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, '*', '*', ((Result)ar.get(j)).get(0), xoClass, xfClass);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetStream.DEBUG = true;
			System.out.println("11."+j+") findHeadStream(*,<obj>,*, "+xfClass+","+xo50Class+","+") using map="+RelatrixKVJsonTransaction.getData(((Result)am.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, '*', ((Result)am.get(j)).get(0), '*',xoClass, xo50Class);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine =0;
			System.out.println("12."+j+") findHeadStream(<obj>,*,*,"+xfClass+","+xo50Class+","+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ad.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, ((Result)ad.get(j)).get(0), '*', '*', xfClass, xo50Class);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		for(int j = 0; j < ar2.size(); j++) {
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			System.out.println("13."+j+") findHeadStream(*,<obj>,<obj>,"+xoClass+") using map="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(1))+
					",range="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(2)));	
			it = RelatrixJsonTransaction.findHeadStream(xid, '*', ((Result)ar2.get(j)).toArray()[0], ((Result)ar2.get(j)).toArray()[1], xoClass);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine = 0;
			System.out.println("14."+j+") findHeadStream(<obj>,*,<obj>,+"+xfClass+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).toArray()[0])+
					", range="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).toArray()[1]));		
			it = RelatrixJsonTransaction.findHeadStream(xid, ((Result)ar2dr.get(j)).toArray()[0], '*', ((Result)ar2dr.get(j)).toArray()[1], xfClass);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		for(int j = 0; j < ar2.size(); j++) {
			displayLine=0;
			System.out.println("15."+j+") findHeadStream(<obj>,<obj>,*,"+xo50Class+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).toArray()[0])+
					", map="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).toArray()[1]));		
			it = RelatrixJsonTransaction.findHeadStream(xid, ((Result)ar2dm.get(j)).toArray()[0], ((Result)ar2dm.get(j)).toArray()[1], '*', xo50Class);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("16."+j+") findHeadStream(?,?,<obj>, "+xoClass+","+xfClass+","+") using range="+RelatrixKVJsonTransaction.getData(((Result)ar.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, '?', '?', ((Result)ar.get(j)).get(0), xoClass, xfClass);
			it.forEachOrdered(o-> {		
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine=0;
			System.out.println("17."+j+") findHeadStream(?,<obj>,?, "+xfClass+","+xo50Class+","+") using map="+RelatrixKVJsonTransaction.getData(((Result)am.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, '?', ((Result)am.get(j)).get(0), '?', xfClass, xo50Class);
			it.forEachOrdered(o-> {	
				Result2 c = (Result2)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine=0;
			System.out.println("18."+j+") findHeadStream(<obj>,?,?,"+xfClass+","+xo50Class+","+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ad.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, ((Result)ad.get(j)).get(0), '?', '?', xfClass, xo50Class);
			it.forEachOrdered(o-> {		
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		for(int j = 0; j < ar2.size(); j++) {
			displayLine=0;
			System.out.println("19."+j+") findHeadStream(?,<obj>,<obj>,"+xoClass+") using map="+
					RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).get(0))+
					" range="+
					RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).get(1)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, '?', ((Result)ar2.get(j)).get(0), ((Result)ar2.get(j)).get(1), xoClass);
			it.forEachOrdered(o-> {	
				Result c = (Result)o; 
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine =0;
			System.out.println("20."+j+") findHeadStream(<obj>,?,<obj>,"+xo50Class+") using domain="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).get(0))+
					" range="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).get(1)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, ((Result)ar2dr.get(j)).get(0), '?', ((Result)ar2dr.get(j)).get(1), xo50Class);
			it.forEachOrdered(o-> {	
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine =0;
			System.out.println("21."+j+") findHeadStream(<obj>,<obj>,?,"+xo50Class+") using domain="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).get(0))+
					" map="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).get(1)));		
			it = RelatrixJsonTransaction.findHeadStream(xid, ((Result)ar2dm.get(j)).get(0), ((Result)ar2dm.get(j)).get(1), '?',xo50Class);
			//ar = new ArrayList<Comparable>();
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		System.out.println("BATTERY1 SUCCESS in "+(System.currentTimeMillis()-tims));
	}
	/**
	 * findTailStream
	 * @param argv
	 * @throws Exception
	 */
	public static void battery2(String[] argv) throws Exception {
		System.out.println("===================== Json Stream Battery2 ===================== ");
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
		Stream<?> itx = null;
		System.out.println("Wildcard queries:");
		displayLine = 0;
		System.out.println("1.) findTailStream(*,*,*,"+xoClass+","+xfClass+","+xo50Class+")...");
		Stream<?> it =  RelatrixJsonTransaction.findTailStream(xid, '*', '*', '*',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result c = (Result)o;
			displayCtrl();
			Relation r = (Relation) c.get();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(r.getDomain())+"->"+RelatrixKVJsonTransaction.getData(r.getMap())+"->"+RelatrixKVJsonTransaction.getData(r.getRange()));
		});
		it.close();
		displayLine = 0;
		System.out.println("2.) findTailStream(*,*,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findTailStream(xid, '*', '*', '?',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {	
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get()));
			if(ar.size() < SAMPLESIZE ) {
				ar.add(c);
			}
		});
		it.close();
		displayLine = 0;
		System.out.println("3.) findTailStream(*,?,*,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findTailStream(xid, '*', '?', '*',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {	
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get()));
			if(am.size() < SAMPLESIZE ) {
				am.add(c);
			}
		});
		it.close();
		displayLine = 0;
		System.out.println("4.) findTailStream(?,*,*."+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findTailStream(xid, '?', '*', '*',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result  c = (Result )o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get()));
			if(ad.size() < SAMPLESIZE) {
				ad.add(c);
			}
		});
		it.close();
		displayLine=0;
		System.out.println("5.) findTailStream(*,?,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findTailStream(xid, '*', '?', '?',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result2 c = (Result2)o; // result2
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0))+"->"+RelatrixKVJsonTransaction.getData(c.get(1)));
			if(ar2.size() < SAMPLESIZE) {
				ar2.add(c);
			}
		});
		it.close();
		displayLine = 0;
		System.out.println("6.) findTailStream(?,*,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findTailStream(xid, '?', '*', '?',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0))+"->"+RelatrixKVJsonTransaction.getData(c.get(1)));
			if(ar2dr.size() < SAMPLESIZE) {
				ar2dr.add(c);
			}
		});
		it.close();
		displayLine = 0;
		System.out.println("7.) findTailStream(?,?,*,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findTailStream(xid, '?', '?', '*',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0))+"->"+RelatrixKVJsonTransaction.getData(c.get(1)));
			if(ar2dm.size() < SAMPLESIZE) {
				ar2dm.add(c);
			}
		});
		it.close();
		displayLine = 0;
		System.out.println("8.) findTailStream(?,?,?,"+xoClass+","+xfClass+","+xo50Class+")...");		
		it = RelatrixJsonTransaction.findTailStream(xid, '?', '?', '?',xoClass, xfClass, xo50Class);
		it.forEachOrdered(o-> {
			Result3 c = (Result3)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0))+"->"+RelatrixKVJsonTransaction.getData(c.get(1))+"->"+RelatrixKVJsonTransaction.getData(c.get(2)));
			if(ar3.size() < SAMPLESIZE) {
				ar3.add(c);
			}
		});
		it.close();
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("8."+j+") findTailStream(?,?,?,<obj>,<obj>,<obj>) using domain="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(0))+
					",map="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(1))+
					",range="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(2)));
			it = RelatrixJsonTransaction.findTailStream(xid, '?','?','?',((Result)ar3.get(j)).get(0), ((Result)ar3.get(j)).get(1), ((Result)ar3.get(j)).get(2));
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0))+"->"+RelatrixKVJsonTransaction.getData(c.get(1))+"->"+RelatrixKVJsonTransaction.getData(c.get(2)));
			});
			it.close();
			displayLine=0;
			//RelatrixHeadsetStream.DEBUG = true;
			System.out.println("8A."+j+") findTailStream(?,*,*,<obj>,"+xoClass+","+xfClass+","+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findTailStream(xid, '?','*', '*', ((Result)ar3.get(j)).get(0), xfClass, xo50Class);
			it.forEachOrdered(o-> {		
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0)));
			});
			it.close();
		}
		it.close();
		System.out.println("----------\r\nAbove are wildcard permutations. Now retrieve those with object references using the");
		System.out.println("wildcard results. Recall tailstream is strictly greater or equal to 'from' element...");
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("9."+j+") findTailStream(<obj>,<obj>,<obj>) "+
					"using domain="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(0))+
					",map="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(1))+
					",range="+
					RelatrixKVJsonTransaction.getData(((Result)ar3.get(j)).get(2)));
			it = RelatrixJsonTransaction.findTailStream(xid, ((Result)ar3.get(j)).get(0), ((Result)ar3.get(j)).get(1), ((Result)ar3.get(j)).get(2));
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			it.close();
			displayLine=0;
			//RelatrixHeadsetStream.DEBUG = true;
			System.out.println("10."+j+") findTailStream(*,*,<obj>,"+xoClass+","+xfClass+","+") using range="+RelatrixKVJsonTransaction.getData(((Result)ar.get(j)).get(0)));	
			it = RelatrixJsonTransaction.findTailStream(xid, '*', '*', ((Result)ar.get(j)).get(0), xoClass, xfClass);
			it.forEachOrdered(o-> {	
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(r.getDomain())+"->"+RelatrixKVJsonTransaction.getData(r.getMap())+"->"+RelatrixKVJsonTransaction.getData(r.getRange()));
			});
			it.close();
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetStream.DEBUG = true;
			System.out.println("11."+j+") findTailStream(*,<obj>,*, "+xfClass+","+xo50Class+","+") using map="+RelatrixKVJsonTransaction.getData(((Result)am.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findTailStream(xid, '*', ((Result)am.get(j)).get(0), '*',xoClass, xo50Class);
			it.forEachOrdered(o-> {	
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(r.getDomain())+"->"+RelatrixKVJsonTransaction.getData(r.getMap())+"->"+RelatrixKVJsonTransaction.getData(r.getRange()));
			});
			it.close();
			displayLine =0;
			System.out.println("12."+j+") findTailStream(<obj>,*,*,"+xfClass+","+xo50Class+","+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ad.get(j)).get(0)));	
			it = RelatrixJsonTransaction.findTailStream(xid, ((Result)ad.get(j)).get(0), '*', '*', xfClass, xo50Class);
			it.forEachOrdered(o-> {	
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(r.getDomain())+"->"+RelatrixKVJsonTransaction.getData(r.getMap())+"->"+RelatrixKVJsonTransaction.getData(r.getRange()));
			});
			it.close();
		}
		for(int j = 0; j < ar2.size(); j++) {
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			System.out.println("13."+j+") findTailStream(*,<obj>,<obj>,"+xoClass+") using map="+
					RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).toArray()[0])+
					" range="+
					RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).toArray()[1]));		
			it = RelatrixJsonTransaction.findTailStream(xid, '*', ((Result)ar2.get(j)).toArray()[0], ((Result)ar2.get(j)).toArray()[1], xoClass);
			it.forEachOrdered(o-> {	
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(r.getDomain())+"->"+RelatrixKVJsonTransaction.getData(r.getMap())+"->"+RelatrixKVJsonTransaction.getData(r.getRange()));
			});
			it.close();
			displayLine = 0;
			System.out.println("14."+j+") findTailStream(<obj>,*,<obj>,"+xfClass+") using domain="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).toArray()[0])+
					", range="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).toArray()[1]));		
			it = RelatrixJsonTransaction.findTailStream(xid, ((Result)ar2dr.get(j)).toArray()[0], '*', ((Result)ar2dr.get(j)).toArray()[1], xfClass);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(r.getDomain())+"->"+RelatrixKVJsonTransaction.getData(r.getMap())+"->"+RelatrixKVJsonTransaction.getData(r.getRange()));
			});
			it.close();
		}
		for(int j = 0; j < ar2.size(); j++) {
			displayLine=0;
			System.out.println("15."+j+") findTailStream(<obj>,<obj>,*,"+xo50Class+") using domain="+
				RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).toArray()[0])
				+", map="+
				RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).toArray()[1]));		
			it = RelatrixJsonTransaction.findTailStream(xid, ((Result)ar2dm.get(j)).toArray()[0], ((Result)ar2dm.get(j)).toArray()[1], '*', xo50Class);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				Relation r = (Relation)c.get();
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(r.getDomain())+"->"+RelatrixKVJsonTransaction.getData(r.getMap())+"->"+RelatrixKVJsonTransaction.getData(r.getRange()));
			});
			it.close();
		}
		for(int j = 0; j < ar.size(); j++) {
			displayLine=0;
			System.out.println("16."+j+") findTailStream(?,?,<obj>, "+xoClass+","+xfClass+","+") using range="+
					RelatrixKVJsonTransaction.getData(((Result)ar.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findTailStream(xid, '?', '?', ((Result)ar.get(j)).get(0), xoClass, xfClass);
			it.forEachOrdered(o-> {	
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0))+"->"+RelatrixKVJsonTransaction.getData(c.get(1)));
			});
			it.close();
			displayLine=0;
			System.out.println("17."+j+") findTailStream(?,<obj>,?, "+xfClass+","+xo50Class+","+") using map="+
					RelatrixKVJsonTransaction.getData(((Result)am.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findTailStream(xid, '?', ((Result)am.get(j)).get(0), '?', xfClass, xo50Class);
			it.forEachOrdered(o-> {	
				Result2 c = (Result2)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0))+"->"+RelatrixKVJsonTransaction.getData(c.get(1)));
			});
			it.close();
			displayLine=0;
			System.out.println("18."+j+") findTailStream(<obj>,?,?,"+xfClass+","+xo50Class+","+") using domain="+
					RelatrixKVJsonTransaction.getData(((Result)ad.get(j)).get(0)));		
			it = RelatrixJsonTransaction.findTailStream(xid, ((Result)ad.get(j)).get(0), '?', '?', xfClass, xo50Class);
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0))+"->"+RelatrixKVJsonTransaction.getData(c.get(1)));
			});
			it.close();
		}
		for(int j = 0; j < ar2.size(); j++) {
			displayLine=0;
			System.out.println("19."+j+") findTailStream(?,<obj>,<obj>,"+xoClass+") using map="+
					RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).get(0))+
					" range="+
					RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).get(1)));		
			it = RelatrixJsonTransaction.findTailStream(xid, '?', ((Result)ar2.get(j)).get(0), ((Result)ar2.get(j)).get(1), xoClass);
			it.forEachOrdered(o-> {	
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0)));
			});
			it.close();
			displayLine =0;
			System.out.println("20."+j+") findTailStream(<obj>,?,<obj>,"+xo50Class+") using domain="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).get(0))+
					" range="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).get(1)));		
			it = RelatrixJsonTransaction.findTailStream(xid, ((Result)ar2dr.get(j)).get(0), '?', ((Result)ar2dr.get(j)).get(1), xo50Class);
			it.forEachOrdered(o-> {	
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0)));
			});
			it.close();
			displayLine =0;
			System.out.println("21."+j+") findTailStream(<obj>,<obj>,?,"+xo50Class+") using domain="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).get(0))+
					" map="+
					RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).get(1)));		
			it = RelatrixJsonTransaction.findTailStream(xid, ((Result)ar2dm.get(j)).get(0), ((Result)ar2dm.get(j)).get(1), '?',xo50Class);
			//ar = new ArrayList<Comparable>();
			it.forEachOrdered(o-> {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+RelatrixKVJsonTransaction.getData(c.get(0)));
			});
			it.close();
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
		Stream<?> it = RelatrixJsonTransaction.findStream(xid, '*','*','*');
		timx = System.currentTimeMillis();
		it.forEachOrdered(o-> {
			Relation dmr = (Relation)((Result)o).get(0);
			try {
				RelatrixJsonTransaction.remove(xid, dmr);
			} catch (IllegalArgumentException | ClassNotFoundException | IllegalAccessException | IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" "+dmr);
				timx = System.currentTimeMillis();
			}
		});
		it.close();
		Stream<?> its = RelatrixJsonTransaction.findStream(xid, '*','*','*');
		its.forEachOrdered(o->{
			Result nex = (Result)o;
			//System.out.println(i+"="+nex);
			System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+nex);
		});
		it.close();
		long siz = RelatrixJsonTransaction.size(xid);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		siz = RelatrixJsonTransaction.size(xid);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixJsonTransaction.entrySetStream(xid, DomainRangeMap.class);
		it.forEachOrdered(o-> {
			Comparable nex = (Comparable)o;
			System.out.println("DomainRangeMap:"+nex);
		});
		it.close();
		siz = RelatrixJsonTransaction.size(xid);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixJsonTransaction.entrySetStream(xid, MapDomainRange.class);
		it.forEachOrdered(o-> {
			Comparable nex = (Comparable)o;
			System.out.println("MapDomainRange:"+nex);
		});
		it.close();
		siz = RelatrixJsonTransaction.size(xid, MapDomainRange.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixJsonTransaction.entrySetStream(xid, MapRangeDomain.class);
		it.forEachOrdered(o-> {
			Comparable nex = (Comparable)o;
			System.out.println("MapRangeDomain:"+nex);
		});
		it.close();
		siz = RelatrixJsonTransaction.size(xid, MapRangeDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixJsonTransaction.entrySetStream(xid, RangeDomainMap.class);
		it.forEachOrdered(o-> {
			Comparable nex = (Comparable)o;
			System.out.println("RangeDomainMap:"+nex);
		});
		it.close();
		siz = RelatrixJsonTransaction.size(xid, RangeDomainMap.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixJsonTransaction.entrySetStream(xid, RangeMapDomain.class);
		it.forEachOrdered(o-> {
			Comparable nex = (Comparable)o;
			System.out.println("RangeMapDomain:"+nex);
		});
		it.close();
		siz = RelatrixJsonTransaction.size(xid, RangeMapDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}/*
		it = RelatrixKV.entrySet(DBKey.class);
		it.forEach(o-> {
			Comparable nex = (Comparable) it.next();
			System.out.println("DBKey:"+nex);
		}
		siz = RelatrixKV.size(DBKey.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(Long.class);
		it.forEach(o-> {
			Comparable nex = (Comparable) it.next();
			System.out.println("Long:"+nex);
		}
		siz = RelatrixKV.size(Long.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(String.class);
		it.forEach(o-> {
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
