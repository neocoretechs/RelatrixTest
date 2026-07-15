package com.neocoretechs.relatrix.test.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.neocoretechs.rocksack.TransactionId;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying transaction findSubStream retrieval. We will let our samplesize be dictated by hi and low range values.
 * Subset provides a persistent collection stream of keys 'from' element inclusive, 'to' element exclusive of the keys specified.<p>
 * We first use wildcard retrievals to build some sample arrays of elements in the database. Then, using those concrete instances
 * to limit retrieval, retrieve further subsets based on the values of those objects.
 * NOTES:
 * program arguments are _database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021,2024,2026
 *
 */
public class EmbeddedRetrievalBattery9 {
	public static boolean DEBUG = false;
	public static boolean DISPLAY = false;
	private static boolean DISPLAYALL = true;
	public static int displayLinesOn[]= {0,1000,5000,9990,15000,20000,30000,40000,50000,60000,70000,80000,90000,99000};
	public static int displayLinesOff[]= {100,1100,5100,9999,15999,20999,30999,40999,50999,60999,70999,80999,90999,100000};
	public static int displayLine = 0;
	public static int displayLineCtr = 0;
	public static long displayTimer = 0;
	static long timx = System.currentTimeMillis();
	public static long min = 0;
	public static long max = 100;
	static long lorange = (max/20L);
	static long hirange = (max/10L);
	static Long lo = (long) min;
	static Long hi = (long) max/10;
	static Long increment = 10L;
	static String x =     "{\"timestamp\":1779166030000,\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\"}]}]}";
	static String x50k =  "{\"timestamp\":1779166050000,\"RightImage\":[{\"count\":0, \"affections\":[ {\"name\":\"alligator\"}]}]}";
	static String xfull = "{\"timestamp\":1779166070000,\"LeftImage\":[{ \"count\":1, \"erections\":[ { \"name\":\"toilet\"}]}]}";
	static String xlo =     "{\"timestamp\":"+(1779166030000L+lorange)+",\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\"}]}]}";
	static String x50klo =  "{\"timestamp\":"+(1779166050000L+lorange)+",\"RightImage\":[{\"count\":0, \"affections\":[ {\"name\":\"alligator\"}]}]}";
	static String xfulllo = "{\"timestamp\":"+(1779166070000L+lorange)+",\"LeftImage\":[{ \"count\":1, \"erections\":[ { \"name\":\"toilet\"}]}]}";
	static String xhi =     "{\"timestamp\":"+(1779166030000L+hirange)+",\"LeftImage\":[{ \"count\":1,\"detections\":[ {\"name\":\"refrigerator\"}]}]}";
	static String x50khi =  "{\"timestamp\":"+(1779166050000L+hirange)+",\"RightImage\":[{\"count\":0, \"affections\":[ {\"name\":\"alligator\"}]}]}";
	static String xfullhi = "{\"timestamp\":"+(1779166070000L+hirange)+",\"LeftImage\":[{ \"count\":1, \"erections\":[ { \"name\":\"toilet\"}]}]}";
	static JSONObject xf = new JSONObject(xfull);
	static JSONObject xo50 = new JSONObject(x50k);
	static JSONObject xo = new JSONObject(x);
	static JSONObject xflo = new JSONObject(xfulllo);
	static JSONObject xo50lo = new JSONObject(x50klo);
	static JSONObject xolo = new JSONObject(xlo);
	static JSONObject xfhi = new JSONObject(xfullhi);
	static JSONObject xo50hi = new JSONObject(x50khi);
	static JSONObject xohi = new JSONObject(xhi);
	static Class<?> xfClass, xClass, x50Class;
	static int i;
	private static TransactionId xid;
	/**
	*/
	public static void main(String[] argv) throws Exception {
		System.out.println("Sub Provides a persistent collection stream of keys 'from' element inclusive, 'to' element exclusive of the keys specified");
		RelatrixJsonTransaction.getInstance();
		xid = RelatrixJsonTransaction.getTransactionId();
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		xfClass = RelatrixKVJsonTransaction.getClassType(xf, xid);
		x50Class = RelatrixKVJsonTransaction.getClassType(xo50, xid);
		xClass = RelatrixKVJsonTransaction.getClassType(xo, xid);
		if(argv.length == 2 && argv[1].equals("init")) {
				battery1AR17(argv);
		}
		if(RelatrixJsonTransaction.size(xid) == 0) {
			battery0(argv);
		}
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
		System.out.println("Stream Battery1 ");
		long tims = System.currentTimeMillis();
		// this list will store an object used to test subsequent queries where a named object is needed
		// it will be extracted from the wildcard queries, typically the elements will be Result result set instances of Result, Result2, Result3
		ArrayList<Comparable> ar = new ArrayList<Comparable>(); // range
		ArrayList<Comparable> am = new ArrayList<Comparable>(); // map
		ArrayList<Comparable> ad = new ArrayList<Comparable>(); // domain
		ArrayList<Comparable> ar2 = new ArrayList<Comparable>(); // will store 2 element result sets map, range
		ArrayList<Comparable> ar2dr = new ArrayList<Comparable>(); // will store 2 element result sets domain,range
		ArrayList<Comparable> ar2dm = new ArrayList<Comparable>(); // will store 2 element result sets domain,map
		ArrayList<Comparable> ar3 = new ArrayList<Comparable>(); // will store 3 element result sets
		Stream<?> it = null;
		System.out.println("Wildcard queries:");
		displayLine = 0;
		// return relation domainclass, mapclass, range lo/hi. this is just an example of returning the identity
		System.out.println("1.) findSubStream(xid,*,*,*,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it =  RelatrixJsonTransaction.findSubStream(xid,'*', '*', '*',xClass, xfClass, RelatrixKVJsonTransaction.getObject(xo50lo, xid), RelatrixKVJsonTransaction.getObject(xo50hi, xid));
		it.forEachOrdered(o-> {
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			//ar.add(c[0]);
		});
		// return range, domainclass, mapclass, range lo/hi, use it to build our one-element ar range sample array for later
		displayLine = 0;
		System.out.println("2.) findSubStream(xid,*,*,?,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJsonTransaction.findSubStream(xid,'*', '*', '?',xClass, xfClass, RelatrixKVJsonTransaction.getObject(xo50lo, xid), RelatrixKVJsonTransaction.getObject(xo50hi, xid));
		it.forEachOrdered(o->  {
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar.add(c);
		});
		// return map, domainclass, mapclass, range lo/hi, use it to build our one-element am map sample array for later
		displayLine = 0;
		System.out.println("3.) findSubStream(xid,*,?,*,"+xClass+","+ xflo+","+ xfhi+","+x50Class+")");
		it = RelatrixJsonTransaction.findSubStream(xid,'*', '?', '*',xClass, RelatrixKVJsonTransaction.getObject(xflo, xid), RelatrixKVJsonTransaction.getObject(xfhi, xid), x50Class);
		it.forEachOrdered(o->  {
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			am.add(c);
		});
		// return domain, mapclass, rangeclass, domain lo/hi, use it to build our one-element ad domain sample array for later
		displayLine = 0;
		System.out.println("4.) findSubStream(xid,?,*,*,"+xlo+","+xhi+","+ xfClass+","+x50Class+")");
		it = RelatrixJsonTransaction.findSubStream(xid,'?', '*', '*',RelatrixKVJsonTransaction.getObject(xolo, xid), RelatrixKVJsonTransaction.getObject(xohi, xid), xfClass, x50Class);
		it.forEachOrdered(o->  {
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ad.add(c);
		});
		// return map and range. domainclass, mapclass, range lo/hi to build our two-element ar2 sample array for later
		displayLine = 0;
		System.out.println("5.) findSubStream(xid,*,?,?,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJsonTransaction.findSubStream(xid,'*', '?', '?',xClass, xfClass, RelatrixKVJsonTransaction.getObject(xo50lo, xid), RelatrixKVJsonTransaction.getObject(xo50hi, xid));
		it.forEachOrdered(o->  {
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar2.add(c);
		});
		// return domain and range. domainclass, mapclass, range lo/hi to build our two-element ar2dr sample array for later
		displayLine = 0;
		System.out.println("6.) findSubStream(xid,?,*,?,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJsonTransaction.findSubStream(xid,'?', '*', '?',xClass, xfClass, RelatrixKVJsonTransaction.getObject(xo50lo, xid), RelatrixKVJsonTransaction.getObject(xo50hi, xid));
		it.forEachOrdered(o->  {
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar2dr.add(c);
		});
		// return domain and map. domainclass, mapclass, range lo/hi to build our two-element ar2dm sample array for later
		displayLine = 0;
		System.out.println("7.) findSubStream(xid,?,?,*,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJsonTransaction.findSubStream(xid,'?', '?', '*',xClass, xfClass, RelatrixKVJsonTransaction.getObject(xo50lo, xid), RelatrixKVJsonTransaction.getObject(xo50hi, xid));
		it.forEachOrdered(o->  {
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar2dm.add(c);
		});
		// return domain, map, and range, domainclass, mapclass, range lo/hi to build our three-element ar3 sample array for later
		displayLine = 0;
		System.out.println("8.) findSubStream(xid,?,?,?,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJsonTransaction.findSubStream(xid,'?', '?', '?',xClass, xfClass, RelatrixKVJsonTransaction.getObject(xo50lo, xid), RelatrixKVJsonTransaction.getObject(xo50hi, xid));
		it.forEachOrdered(o->  {
			Result3 c = (Result3)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar3.add(c);
		});

		// Now that we have built our sample arrays from retrieval, use the elements therein to retrieve further subsets based on the sample data and the concrete instances.
		// This demonstrates how we use object instances in retrieval to retrieve subsets. In these cases identity Relations are being retrieved
		System.out.println("----------");
		System.out.println("Above are all the wildcard permutations. Now retrieve those identity Relations with object references using the wildcard results.");
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("9."+j+") findSubStream(xid,<obj>,<obj>,<obj>) using ="+
					Arrays.toString(RelatrixJsonTransaction.tupleResolver((Result)ar3.get(j)))+
					"("+((Result)ar3.get(j)).get(0).getClass().getName()+"),"+
					",("+((Result)ar3.get(j)).get(1).getClass().getName()+"),"+
					",("+((Result)ar3.get(j)).get(2).getClass().getName());
			it = RelatrixJsonTransaction.findSubStream(xid,((Result)ar3.get(j)).get(0), ((Result)ar3.get(j)).get(1), ((Result)ar3.get(j)).get(2));
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("10."+j+") findSubStream(xid,*,*,<obj>,"+xClass+"," +xfClass+") using range="+Arrays.toString(RelatrixJsonTransaction.tupleResolver((Result)ar3.get(j))));		
			it = RelatrixJsonTransaction.findSubStream(xid,'*', '*', ((Result)ar3.get(j)).get(3), xClass, xfClass);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		
		System.out.println("----------");
		System.out.println("Begin return identity Relations from: match 1 object instance, 2 wildcard");
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("11."+j+") findSubStream(xid,*,<obj>,*,"+xClass+","+x50Class+") using map="+Arrays.toString(RelatrixJsonTransaction.tupleResolver((Result)am.get(j))));		
			it = RelatrixJsonTransaction.findSubStream(xid,'*', ((Result)am.get(j)).get(0), '*',xClass, x50Class);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine =0;
			System.out.println("12."+j+") findSubStream(xid,<obj>,*,*,"+ xfClass+","+x50Class+") using domain="+Arrays.toString(RelatrixJsonTransaction.tupleResolver((Result)ad.get(j))));		
			it = RelatrixJsonTransaction.findSubStream(xid,((Result)ad.get(j)).get(0), '*', '*', xfClass, x50Class);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}

		System.out.println("----------");
		System.out.println("Begin return identity Relations from: match 2 object instance, 1 wildcard");
		for(int j = 0; j < ar2.size(); j++) {
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			System.out.println("13."+j+") findSubStream(xid,*,<obj>,<obj>,"+xClass+") using map="+RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).toArray()[0])+" range="+RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).toArray()[1]));		
			it = RelatrixJsonTransaction.findSubStream(xid,'*', ((Result)ar2.get(j)).toArray()[0], ((Result)ar2.get(j)).toArray()[1], xClass);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine = 0;
			System.out.println("14."+j+") findSubStream(xid,<obj>,*,<obj>,"+xfClass+") using ="+RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).toArray()[0])+", "+RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).toArray()[1]));		
			it = RelatrixJsonTransaction.findSubStream(xid,((Result)ar2dr.get(j)).toArray()[0], '*', ((Result)ar2dr.get(j)).toArray()[1], xfClass);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine =0;
			System.out.println("15."+j+") findSubStream(xid,<obj>,<obj>,*,"+x50Class+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).toArray()[0])+", map="+RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).toArray()[1]));		
			it = RelatrixJsonTransaction.findSubStream(xid,((Result)ar2dm.get(j)).toArray()[0], ((Result)ar2dm.get(j)).toArray()[1], '*',x50Class);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		
		System.out.println("----------");
		System.out.println("Begin return 2 element Result set from: match 1 object instance, 2 class types");
		for(int j = 0; j < ar.size(); j++) {
			displayLine =0;
			System.out.println("16."+j+") findSubStream(xid,?,?,<obj>,"+xClass+","+xfClass+") using range="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(((Result)ar.get(j)))));		
			it = RelatrixJsonTransaction.findSubStream(xid,'?', '?', ((Result)ar.get(j)).get(0), xClass, xfClass);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine =0;
			System.out.println("17."+j+") findSubStream(xid,?,<obj>,?,"+xClass+","+x50Class+") using map="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(((Result)am.get(j)))));		
			it = RelatrixJsonTransaction.findSubStream(xid,'?', ((Result)am.get(j)).get(0), '?', xClass, x50Class);
			it.forEachOrdered(o->  {
				Result2 c = (Result2)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine =0;
			System.out.println("18."+j+") findSubStream(xid,<obj>,?,?,"+xfClass+","+x50Class+") using domain="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(((Result)ad.get(j)))));		
			it = RelatrixJsonTransaction.findSubStream(xid,((Result)ad.get(j)).get(0), '?', '?', xfClass, x50Class);
			it.forEachOrdered(o->  {
				Result2 c = (Result2)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		System.out.println("----------");
		System.out.println("Begin return 1 element Result set from: match 2 object instance, 1 class type");
		for(int j = 0; j < ar2.size(); j++) {
			displayLine=0;
			System.out.println("19."+j+") findSubStream(xid,?,<obj>,<obj>,"+xClass+") using map="+RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).get(0))+" range="+RelatrixKVJsonTransaction.getData(((Result)ar2.get(j)).get(1)));		
			it = RelatrixJsonTransaction.findSubStream(xid,'?', ((Result)ar2.get(j)).get(0), ((Result)ar2.get(j)).get(1), xClass);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine=0;
			System.out.println("20."+j+") findSubStream(xid,<obj>,?,<obj>,+"+xfClass+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).get(0))+" range="+ RelatrixKVJsonTransaction.getData(((Result)ar2dr.get(j)).get(1)));		
			it = RelatrixJsonTransaction.findSubStream(xid,((Result)ar2dr.get(j)).get(0), '?', ((Result)ar2dr.get(j)).get(1), xfClass);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			displayLine=0;
			System.out.println("21."+j+") findSubStream(xid,<obj>,<obj>,?,"+x50Class+") using domain="+RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).get(0))+" map="+RelatrixKVJsonTransaction.getData(((Result)ar2dm.get(j)).get(1)));		
			it = RelatrixJsonTransaction.findSubStream(xid,((Result)ar2dm.get(j)).get(0), ((Result)ar2dm.get(j)).get(1), '?',x50Class);
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
		}
		//
		// proceed with hi/lo range tests
		//
		System.out.println("----------");
		System.out.println("Begin hi/lo range testing");
		for(int j = 0; j < ar2dm.size(); j++) {
			displayLine =0;
			System.out.println("22."+j+") findSubStream(xid,*,*,?,<class>,<class>,<obj>,<obj>) using domain="+((Result)ar2dm.get(j)).get(0).getClass()+" map="+((Result)ar2dm.get(j)).get(1).getClass()+
					" range="+RelatrixKVJsonTransaction.getData(RelatrixKVJsonTransaction.getObject(xo50lo, xid))+" to "+ RelatrixKVJsonTransaction.getData(RelatrixKVJsonTransaction.getObject(xo50hi, xid)));		
			it = RelatrixJsonTransaction.findSubStream(xid,'*','*','?',((Result)ar2dm.get(j)).get(0).getClass(), ((Result)ar2dm.get(j)).get(1).getClass(),RelatrixKVJsonTransaction.getObject(xo50lo, xid),RelatrixKVJsonTransaction.getObject(xo50hi, xid));
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			long l = xo50lo.getLong("timestamp");
			l+=increment;                                                                      
			xo50lo.put("timestamp", l);
			l = xo50hi.getLong("timestamp");
			l+=increment;
			xo50hi.put("timestamp", l);
			System.out.println("23."+j+") findSubStream(xid,?,?,?,<class>,<class>,<obj>,<obj>) using domain="+((Result)ar2dm.get(j)).get(0).getClass()+" map="+((Result)ar2dm.get(j)).get(1).getClass()+
					" range="+RelatrixKVJsonTransaction.getData(RelatrixKVJsonTransaction.getObject(xo50lo, xid))+" to "+ RelatrixKVJsonTransaction.getData(RelatrixKVJsonTransaction.getObject(xo50hi, xid)));	
			it = RelatrixJsonTransaction.findSubStream(xid,'?','?','?',((Result)ar2dm.get(j)).get(0).getClass(), ((Result)ar2dm.get(j)).get(1).getClass(),RelatrixKVJsonTransaction.getObject(xo50lo, xid),RelatrixKVJsonTransaction.getObject(xo50hi, xid));
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
			l = xo50lo.getLong("timestamp");
			l+=increment;
			xo50lo.put("timestamp", l);
			l = xo50hi.getLong("timestamp");
			l+=increment;
			xo50hi.put("timestamp", l);
			System.out.println("24."+j+") findSubStream(xid,?,*,?,<class>,<class>,<obj>,<obj>) using domain="+((Result)ar2dm.get(j)).get(0).getClass()+" map="+((Result)ar2dm.get(j)).get(1).getClass()+
					" range="+RelatrixKVJsonTransaction.getData(RelatrixKVJsonTransaction.getObject(xo50lo, xid))+" to "+ RelatrixKVJsonTransaction.getData(RelatrixKVJsonTransaction.getObject(xo50hi, xid)));	
			it = RelatrixJsonTransaction.findSubStream(xid,'?','*','?',((Result)ar2dm.get(j)).get(0).getClass(), ((Result)ar2dm.get(j)).get(1).getClass(),RelatrixKVJsonTransaction.getObject(xo50lo, xid),RelatrixKVJsonTransaction.getObject(xo50hi, xid));
			it.forEachOrdered(o->  {
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJsonTransaction.tupleResolver(c)));
			});
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
		Stream<?> it = RelatrixJsonTransaction.findStream(xid, '*','*','*');

		i = 0;
		it.forEachOrdered(o->  {
			Relation dmr = (Relation)((Result)o).get(0);
			try {
				RelatrixJsonTransaction.remove(xid, dmr.getDomain(), dmr.getMap());
			} catch (IllegalAccessException | ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
			++i;
			if((System.currentTimeMillis()-timx) > 1000) {
				System.out.println("deleting "+i+" "+o);
				timx = System.currentTimeMillis();
			}
		});
		Stream<?> its = RelatrixJsonTransaction.findStream(xid, '*','*','*');
		it.forEachOrdered(o->  {
			//System.out.println(i+"="+nex);
			System.out.println("KV RANGE 1AR17 KEY SHOULD BE DELETED:"+o);
		});
		long siz = RelatrixJsonTransaction.size(xid);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 KEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		its = RelatrixJsonTransaction.entrySetStream(xid, Relation.class);
		it.forEachOrdered(o->  {
			System.out.println("Relation:"+o);
		});
		siz = RelatrixJsonTransaction.size(xid);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixJsonTransaction.entrySetStream(xid, DomainRangeMap.class);
		it.forEachOrdered(o->  {
			System.out.println("DomainRangeMap:"+o);
		});
		siz = RelatrixJsonTransaction.size(xid);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = RelatrixJsonTransaction.entrySetStream(xid, MapDomainRange.class);
		it.forEachOrdered(o->  {
			System.out.println("MapDomainRange:"+o);
		});
		siz = RelatrixJsonTransaction.size(xid, MapDomainRange.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = RelatrixJsonTransaction.entrySetStream(xid, MapRangeDomain.class);
		it.forEachOrdered(o->  {
			System.out.println("MapRangeDomain:"+o);
		});
		siz = RelatrixJsonTransaction.size(xid, MapRangeDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixJsonTransaction.entrySetStream(xid, RangeDomainMap.class);
		it.forEachOrdered(o->  {
			System.out.println("RangeDomainMap:"+o);
		});
		siz = RelatrixJsonTransaction.size(xid, RangeDomainMap.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixJsonTransaction.entrySetStream(xid, RangeMapDomain.class);
		it.forEachOrdered(o->  {
			System.out.println("RangeMapDomain:"+o);
		});
		siz = RelatrixJsonTransaction.size(xid, RangeMapDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeMapDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}/*
		it = RelatrixKV.entrySet(DBKey.class);
		it.forEachOrdered(o->  {
			Comparable nex = (Comparable) it.next();
			System.out.println("DBKey:"+nex);
		}
		siz = RelatrixKV.size(DBKey.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DBKEY MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(Long.class);
		it.forEachOrdered(o->  {
			Comparable nex = (Comparable) it.next();
			System.out.println("Long:"+nex);
		}
		siz = RelatrixKV.size(Long.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Long MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(String.class);
		it.forEachOrdered(o->  {
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
