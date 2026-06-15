package com.neocoretechs.relatrix.test.json;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.neocoretechs.relatrix.RelatrixKV;
import com.neocoretechs.relatrix.RelatrixKVJson;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.Result2;
import com.neocoretechs.relatrix.Result3;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findSubSet retrieval. We will let our samplesize be dictated by hi and low range values.
 * Subset provides a persistent collection iterator of keys 'from' element inclusive, 'to' element exclusive of the keys specified.<p>
 * We first use wildcard retrievals to build some sample arrays of elements in the database. Then, using those concrete instances
 * to limit retrieval, retrieve further subsets based on the values of those objects.
 * NOTES:
 * program arguments are _database
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2021,2024
 *
 */
public class EmbeddedRetrievalBattery4 {
	public static boolean DEBUG = false;
	public static boolean DISPLAY = false;
	private static boolean DISPLAYALL = true;
	public static int displayLinesOn[]= {0,1000,5000,9990,15000,20000,30000,40000,50000,60000,70000,80000,90000,99000};
	public static int displayLinesOff[]= {100,1100,5100,9999,15999,20999,30999,40999,50999,60999,70999,80999,90999,100000};
	public static int displayLine = 0;
	public static int displayLineCtr = 0;
	public static long displayTimer = 0;
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

	/**
	*/
	public static void main(String[] argv) throws Exception {
		System.out.println("Subset Provides a persistent collection iterator of keys 'from' element inclusive, 'to' element exclusive of the keys specified");
		RelatrixJson.setTablespace(argv[0]);
		AbstractRelation.displayLevel = AbstractRelation.displayLevels.MINIMAL;
		xfClass = RelatrixKVJson.getClassType(xf);
		x50Class = RelatrixKVJson.getClassType(xo50);
		xClass = RelatrixKVJson.getClassType(xo);
		if(argv.length == 2 && argv[1].equals("init")) {
				battery1AR17(argv);
		}
		if(RelatrixJson.size() == 0) {
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
		System.out.println("Iterator Battery1 ");
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
		Iterator<?> it = null;
		System.out.println("Wildcard queries:");
		displayLine = 0;
		// return relation domainclass, mapclass, range lo/hi. this is just an example of returning the identity
		System.out.println("1.) findSubSet(*,*,*,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it =  RelatrixJson.findSubSet('*', '*', '*',xClass, xfClass, RelatrixKVJson.getObject(xo50lo), RelatrixKVJson.getObject(xo50hi));
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			//ar.add(c[0]);
		}
		// return range, domainclass, mapclass, range lo/hi, use it to build our one-element ar range sample array for later
		displayLine = 0;
		System.out.println("2.) findSubSet(*,*,?,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJson.findSubSet('*', '*', '?',xClass, xfClass, RelatrixKVJson.getObject(xo50lo), RelatrixKVJson.getObject(xo50hi));
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar.add(c);
		}
		// return map, domainclass, mapclass, range lo/hi, use it to build our one-element am map sample array for later
		displayLine = 0;
		System.out.println("3.) findSubSet(*,?,*,"+xClass+","+ xflo+","+ xfhi+","+x50Class+")");
		it = RelatrixJson.findSubSet('*', '?', '*',xClass, RelatrixKVJson.getObject(xflo), RelatrixKVJson.getObject(xfhi), x50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			am.add(c);
		}
		// return domain, mapclass, rangeclass, domain lo/hi, use it to build our one-element ad domain sample array for later
		displayLine = 0;
		System.out.println("4.) findSubSet(?,*,*,"+xlo+","+xhi+","+ xfClass+","+x50Class+")");
		it = RelatrixJson.findSubSet('?', '*', '*',RelatrixKVJson.getObject(xolo), RelatrixKVJson.getObject(xohi), xfClass, x50Class);
		while(it.hasNext()) {
			Object o = it.next();
			Result c = (Result)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ad.add(c);
		}
		// return map and range. domainclass, mapclass, range lo/hi to build our two-element ar2 sample array for later
		displayLine = 0;
		System.out.println("5.) findSubSet(*,?,?,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJson.findSubSet('*', '?', '?',xClass, xfClass, RelatrixKVJson.getObject(xo50lo), RelatrixKVJson.getObject(xo50hi));
		while(it.hasNext()) {
			Object o = it.next();
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar2.add(c);
		}
		// return domain and range. domainclass, mapclass, range lo/hi to build our two-element ar2dr sample array for later
		displayLine = 0;
		System.out.println("6.) findSubSet(?,*,?,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJson.findSubSet('?', '*', '?',xClass, xfClass, RelatrixKVJson.getObject(xo50lo), RelatrixKVJson.getObject(xo50hi));
		while(it.hasNext()) {
			Object o = it.next();
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar2dr.add(c);
		}
		// return domain and map. domainclass, mapclass, range lo/hi to build our two-element ar2dm sample array for later
		displayLine = 0;
		System.out.println("7.) findSubSet(?,?,*,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJson.findSubSet('?', '?', '*',xClass, xfClass, RelatrixKVJson.getObject(xo50lo), RelatrixKVJson.getObject(xo50hi));
		while(it.hasNext()) {
			Object o = it.next();
			Result2 c = (Result2)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar2dm.add(c);
		}
		// return domain, map, and range, domainclass, mapclass, range lo/hi to build our three-element ar3 sample array for later
		displayLine = 0;
		System.out.println("8.) findSubSet(?,?,?,"+xClass+","+ xfClass+","+ x50klo+","+ x50khi+")");
		it = RelatrixJson.findSubSet('?', '?', '?',xClass, xfClass, RelatrixKVJson.getObject(xo50lo), RelatrixKVJson.getObject(xo50hi));
		while(it.hasNext()) {
			Object o = it.next();
			Result3 c = (Result3)o;
			displayCtrl();
			if(DISPLAY || DISPLAYALL)
				System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			// samplesize is dictated by hi and low range
			ar3.add(c);
		}

		// Now that we have built our sample arrays from retrieval, use the elements therein to retrieve further subsets based on the sample data and the concrete instances.
		// This demonstrates how we use object instances in retrieval to retrieve subsets. In these cases identity Relations are being retrieved
		System.out.println("----------");
		System.out.println("Above are all the wildcard permutations. Now retrieve those identity Relations with object references using the wildcard results.");
		for(int j = 0; j < ar3.size(); j++) {
			displayLine = 0;
			System.out.println("9."+j+") findSubSet(<obj>,<obj>,<obj>) using ="+
					Arrays.toString(RelatrixJson.tupleResolver((Result)ar3.get(j)))+
					"("+((Result)ar3.get(j)).get(0).getClass().getName()+"),"+
					",("+((Result)ar3.get(j)).get(1).getClass().getName()+"),"+
					",("+((Result)ar3.get(j)).get(2).getClass().getName());
			it = RelatrixJson.findSubSet(((Result)ar3.get(j)).get(0), ((Result)ar3.get(j)).get(1), ((Result)ar3.get(j)).get(2));
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine=0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("10."+j+") findSubSet(*,*,<obj>,"+xClass+"," +xfClass+") using range="+Arrays.toString(RelatrixJson.tupleResolver((Result)ar3.get(j))));		
			it = RelatrixJson.findSubSet('*', '*', ((Result)ar3.get(j)).get(3), xClass, xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		
		System.out.println("----------");
		System.out.println("Begin return identity Relations from: match 1 object instance, 2 wildcard");
		for(int j = 0; j < ar.size(); j++) {
			displayLine = 0;
			//RelatrixHeadsetIterator.DEBUG = true;
			System.out.println("11."+j+") findSubSet(*,<obj>,*,"+xClass+","+x50Class+") using map="+Arrays.toString(RelatrixJson.tupleResolver((Result)am.get(j))));		
			it = RelatrixJson.findSubSet('*', ((Result)am.get(j)).get(0), '*',xClass, x50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine =0;
			System.out.println("12."+j+") FindSubset(<obj>,*,*,"+ xfClass+","+x50Class+") using domain="+Arrays.toString(RelatrixJson.tupleResolver((Result)ad.get(j))));		
			it = RelatrixJson.findSubSet(((Result)ad.get(j)).get(0), '*', '*', xfClass, x50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}

		System.out.println("----------");
		System.out.println("Begin return identity Relations from: match 2 object instance, 1 wildcard");
		for(int j = 0; j < ar2.size(); j++) {
			// From a Result2 we can call get(0) and get(1), like an array, we can also call toArray
			displayLine = 0;
			System.out.println("13."+j+") findSubSet(*,<obj>,<obj>,"+xClass+") using map="+RelatrixKVJson.getData(((Result)ar2.get(j)).toArray()[0])+" range="+RelatrixKVJson.getData(((Result)ar2.get(j)).toArray()[1]));		
			it = RelatrixJson.findSubSet('*', ((Result)ar2.get(j)).toArray()[0], ((Result)ar2.get(j)).toArray()[1], xClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine = 0;
			System.out.println("14."+j+") findSubSet(<obj>,*,<obj>,"+xfClass+") using ="+RelatrixKVJson.getData(((Result)ar2dr.get(j)).toArray()[0])+", "+RelatrixKVJson.getData(((Result)ar2dr.get(j)).toArray()[1]));		
			it = RelatrixJson.findSubSet(((Result)ar2dr.get(j)).toArray()[0], '*', ((Result)ar2dr.get(j)).toArray()[1], xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine =0;
			System.out.println("15."+j+") findSubSet(<obj>,<obj>,*,"+x50Class+") using domain="+RelatrixKVJson.getData(((Result)ar2dm.get(j)).toArray()[0])+", map="+RelatrixKVJson.getData(((Result)ar2dm.get(j)).toArray()[1]));		
			it = RelatrixJson.findSubSet(((Result)ar2dm.get(j)).toArray()[0], ((Result)ar2dm.get(j)).toArray()[1], '*',x50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		
		System.out.println("----------");
		System.out.println("Begin return 2 element Result set from: match 1 object instance, 2 class types");
		for(int j = 0; j < ar.size(); j++) {
			displayLine =0;
			System.out.println("16."+j+") findSubSet(?,?,<obj>,"+xClass+","+xfClass+") using range="+Arrays.toString(RelatrixJson.tupleResolver(((Result)ar.get(j)))));		
			it = RelatrixJson.findSubSet('?', '?', ((Result)ar.get(j)).get(0), xClass, xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine =0;
			System.out.println("17."+j+") findSubSet(?,<obj>,?,"+xClass+","+x50Class+") using map="+Arrays.toString(RelatrixJson.tupleResolver(((Result)am.get(j)))));		
			it = RelatrixJson.findSubSet('?', ((Result)am.get(j)).get(0), '?', xClass, x50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result2 c = (Result2)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine =0;
			System.out.println("18."+j+") findSubSet(<obj>,?,?,"+xfClass+","+x50Class+") using domain="+Arrays.toString(RelatrixJson.tupleResolver(((Result)ad.get(j)))));		
			it = RelatrixJson.findSubSet(((Result)ad.get(j)).get(0), '?', '?', xfClass, x50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result2 c = (Result2)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		System.out.println("----------");
		System.out.println("Begin return 1 element Result set from: match 2 object instance, 1 class type");
		for(int j = 0; j < ar2.size(); j++) {
			displayLine=0;
			System.out.println("19."+j+") findSubSet(?,<obj>,<obj>,"+xClass+") using map="+RelatrixKVJson.getData(((Result)ar2.get(j)).get(0))+" range="+RelatrixKVJson.getData(((Result)ar2.get(j)).get(1)));		
			it = RelatrixJson.findSubSet('?', ((Result)ar2.get(j)).get(0), ((Result)ar2.get(j)).get(1), xClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine=0;
			System.out.println("20."+j+") findSubSet(<obj>,?,<obj>,+"+xfClass+") using domain="+RelatrixKVJson.getData(((Result)ar2dr.get(j)).get(0))+" range="+ RelatrixKVJson.getData(((Result)ar2dr.get(j)).get(1)));		
			it = RelatrixJson.findSubSet(((Result)ar2dr.get(j)).get(0), '?', ((Result)ar2dr.get(j)).get(1), xfClass);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			displayLine=0;
			System.out.println("21."+j+") findSubSet(<obj>,<obj>,?,"+x50Class+") using domain="+RelatrixKVJson.getData(((Result)ar2dm.get(j)).get(0))+" map="+RelatrixKVJson.getData(((Result)ar2dm.get(j)).get(1)));		
			it = RelatrixJson.findSubSet(((Result)ar2dm.get(j)).get(0), ((Result)ar2dm.get(j)).get(1), '?',x50Class);
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
		}
		//
		// proceed with hi/lo range tests
		//
		System.out.println("----------");
		System.out.println("Begin hi/lo range testing");
		for(int j = 0; j < ar2dm.size(); j++) {
			displayLine =0;
			System.out.println("22."+j+") findSubSet(*,*,?,<class>,<class>,<obj>,<obj>) using domain="+((Result)ar2dm.get(j)).get(0).getClass()+" map="+((Result)ar2dm.get(j)).get(1).getClass()+
					" range="+RelatrixKVJson.getData(RelatrixKVJson.getObject(xo50lo))+" to "+ RelatrixKVJson.getData(RelatrixKVJson.getObject(xo50hi)));		
			it = RelatrixJson.findSubSet('*','*','?',((Result)ar2dm.get(j)).get(0).getClass(), ((Result)ar2dm.get(j)).get(1).getClass(),RelatrixKVJson.getObject(xo50lo),RelatrixKVJson.getObject(xo50hi));
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			long l = xo50lo.getLong("timestamp");
			l+=increment;                                                                      
			xo50lo.put("timestamp", l);
			l = xo50hi.getLong("timestamp");
			l+=increment;
			xo50hi.put("timestamp", l);
			System.out.println("23."+j+") findSubSet(?,?,?,<class>,<class>,<obj>,<obj>) using domain="+((Result)ar2dm.get(j)).get(0).getClass()+" map="+((Result)ar2dm.get(j)).get(1).getClass()+
					" range="+RelatrixKVJson.getData(RelatrixKVJson.getObject(xo50lo))+" to "+ RelatrixKVJson.getData(RelatrixKVJson.getObject(xo50hi)));	
			it = RelatrixJson.findSubSet('?','?','?',((Result)ar2dm.get(j)).get(0).getClass(), ((Result)ar2dm.get(j)).get(1).getClass(),RelatrixKVJson.getObject(xo50lo),RelatrixKVJson.getObject(xo50hi));
			while(it.hasNext()) {
				Object o = it.next();
				Result c = (Result)o;
				displayCtrl();
				if(DISPLAY || DISPLAYALL)
					System.out.println(displayLine+"="+Arrays.toString(RelatrixJson.tupleResolver(c)));
			}
			l = xo50lo.getLong("timestamp");
			l+=increment;
			xo50lo.put("timestamp", l);
			l = xo50hi.getLong("timestamp");
			l+=increment;
			xo50hi.put("timestamp", l);
			System.out.println("24."+j+") findSubSet(?,*,?,<class>,<class>,<obj>,<obj>) using domain="+((Result)ar2dm.get(j)).get(0).getClass()+" map="+((Result)ar2dm.get(j)).get(1).getClass()+
					" range="+RelatrixKVJson.getData(RelatrixKVJson.getObject(xo50lo))+" to "+ RelatrixKVJson.getData(RelatrixKVJson.getObject(xo50hi)));	
			it = RelatrixJson.findSubSet('?','*','?',((Result)ar2dm.get(j)).get(0).getClass(), ((Result)ar2dm.get(j)).get(1).getClass(),RelatrixKVJson.getObject(xo50lo),RelatrixKVJson.getObject(xo50hi));
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
	 * remove entries
	 * @param argv
	 * @throws Exception
	 */
	public static void battery1AR17(String[] argv) throws Exception {
		long tims = System.currentTimeMillis();
		System.out.println("CleanDB");
		Iterator<?> it = RelatrixJson.findSet('*','*','*');
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
		it = RelatrixKV.entrySet(Relation.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("Relation:"+nex);
		}
		siz = RelatrixJson.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 Relation MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(DomainRangeMap.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("DomainRangeMap:"+nex);
		}
		siz = RelatrixJson.size();
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 DomainRangeMap MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = RelatrixKV.entrySet(MapDomainRange.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("MapDomainRange:"+nex);
		}
		siz = RelatrixKV.size(MapDomainRange.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapDomainRange MISMATCH:"+siz+" > 0 after delete/commit");
		}

		it = RelatrixKV.entrySet(MapRangeDomain.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("MapRangeDomain:"+nex);
		}
		siz = RelatrixKV.size(MapRangeDomain.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 MapRangeDomain MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(RangeDomainMap.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("RangeDomainMap:"+nex);
		}
		siz = RelatrixKV.size(RangeDomainMap.class);
		if(siz > 0) {
			System.out.println("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after all deleted and committed");
			throw new Exception("KV RANGE 1AR17 RangeDomainMap MISMATCH:"+siz+" > 0 after delete/commit");
		}
		it = RelatrixKV.entrySet(RangeMapDomain.class);
		while(it.hasNext()) {
			Comparable nex = (Comparable) it.next();
			System.out.println("RangeMapDomain:"+nex);
		}
		siz = RelatrixKV.size(RangeMapDomain.class);
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
