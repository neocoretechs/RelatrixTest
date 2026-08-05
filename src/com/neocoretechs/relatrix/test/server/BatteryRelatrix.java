package com.neocoretechs.relatrix.test.server;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.neocoretechs.relatrix.Relation;
import com.neocoretechs.relatrix.AbstractRelation;
import com.neocoretechs.relatrix.Result;
import com.neocoretechs.relatrix.client.RelatrixClient;
import com.neocoretechs.relatrix.parallel.SynchronizedThreadManager;

/**
 * This series of tests loads up arrays to create a cascading set of retrievals mostly checking
 * and verifying findStream retrieval using the client to a remote {@link com.neocoretechs.relatrix.server.RelatrixServer}.
 * To test with the embedded database change the session to Relatrix.getInstance()
 * NOTES:
 * program arguments are remote_node remote_port_for_database <p/>
 * or database_tablespace for embedded test.
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2024
 */
public class BatteryRelatrix {
	public static boolean DEBUG = false;
	static String key = "This is a test"; // holds the base random key string for tests
	static String uniqKeyFmt = "%0100d"; // base + counter formatted with this gives equal length strings for canonical ordering
	static int min = 0;
	static int max = 2000;
	public static String DATABASE;
	//static Relatrix session = Relatrix.getInstance();
	static RelatrixClient session = null;
	static String fkey;
	static int i;
	static Result r = null;

	/**
	* Analysis test fixture
	*/
	public static void main(String[] argv) throws Exception {
		if(argv.length > 3 && argv[2].equals("max")) {
			System.out.println("Setting max items to "+argv[3]);
			max = Integer.parseInt(argv[3]);
		} else {
			if(argv.length > 2 && argv[2].equals("init")) {
				System.out.println("Initialize database to zero items, then terminate...");
				System.exit(0);
			}
		}
		session = new RelatrixClient(argv[0], Integer.parseInt(argv[1]) );
		if(session.size() == 0) {
			battery1();
		}
		battery1A();
		battery2();
		battery2A();
		System.out.println("TEST BATTERY COMPLETE.");	
		System.exit(0);
	}
	/**
	 * Loads up on keys
	 */
	public static void battery1() throws Exception {
		System.out.println("Battery0 ");
		long tims = System.currentTimeMillis();
		int dupes = 0;
		int recs = 0;
		String fkey = null;
		int i = min;
		for(; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			Relation dmr = session.store(fkey, "Has unit", Long.valueOf(i));
			if(DEBUG)
			System.out.println(i+".)"+dmr);
			Relation dmr2 = session.store(dmr ,"has identity",Long.valueOf(i));
			if(DEBUG)
			System.out.println(i+".)"+dmr2);
			++recs;
		}
		System.out.println("BATTERY0 SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Stored "+recs+" records, rejected "+dupes+" dupes.");
	}
	/**
	 * sequential iterator with client re-use via setIterator()
	 * @throws Exception
	 */
	public static void battery1A() throws Exception {
		System.out.println("Battery1A ");
		long tims = System.currentTimeMillis();
		int recs = 0;
		Iterator<?> itPrimary = null;
		Iterator itSecondary = null;
		for(i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			//Optional<?> o =  ((Stream)session.findStream(fkey, "Has unit", Long.valueOf(i)).parallel()).findFirst();
			if(itPrimary != null)
				session.setIterator(itPrimary);
			itPrimary =  session.findSet(fkey, "Has unit", Long.valueOf(i));
			System.out.println(i+".) Obtained primary Optional iterator -- PASSED");
			while(itPrimary.hasNext()) {
				r = (Result) itPrimary.next();
				System.out.println(i+".) Obtained primary Optional Result -- PASSED");
			}
			//Optional<?> p = session.findStream(((Result)o.get()).get(), '*', '*').findFirst();
			if(itSecondary != null)
				session.setIterator(itSecondary);
			itSecondary = session.findSet(r.get(), '*', '*');
			System.out.println(i+".) Obtained secondary Optional iterator -- PASSED");
			while(itSecondary.hasNext()) {
				r = (Result) itSecondary.next();
				System.out.println(i+".) Obtained secondary Optional Result -- PASSED");
			}
			if(!(r.get() instanceof AbstractRelation))
				System.out.println(r.get().getClass()+" isnt AbstractRelation; value:"+r.get()+" FAIL");
			else {
				// main morphism
				Relation m = (Relation) r.get();
				System.out.println("Obtained Primary Relation from secondary Result  -- PASSED.");
				if(!(m.getDomain() instanceof AbstractRelation)) 
					System.out.println(m.getDomain().getClass()+" isnt AbstractRelation; value:"+m+" FAIL");
				else {
					System.out.println("Obtained Domain Relation  -- PASSED.");
					// morphism in domain "has unit"
					Relation d = (Relation) m.getDomain();
					if(!(d.getDomain() instanceof String))
						System.out.println(d.getDomain().getClass()+" domain isnt String; value:"+d+" FAIL");
					else {
						System.out.println("Obtained Relation Domain value  -- PASSED.");
						if(!d.getDomain().equals(fkey))
							System.out.println("Domain doesnt match "+fkey+" FAIL");
						else
							System.out.println("Domain matches fkey  -- PASSED.");
					}
					if(!(d.getMap() instanceof String))
						System.out.println(d.getMap().getClass()+" map isnt String; value:"+d);
					else {
						System.out.println("Map is expected vale  -- PASSED.");
						if(!d.getMap().equals("Has unit"))
							System.out.println("Map doesnt match 'Has unit'"+d+" FAIL");
					}
					if(!(d.getRange() instanceof Long))
						System.out.println(d.getRange().getClass()+" range isnt Long; value:"+d+" FAIL");
					else {
						System.out.println("Map is expected value  -- PASSED.");
						if(!d.getRange().equals(Long.valueOf(i)))
							System.out.println("Range doesnt match "+i+" FAIL");
						else
							System.out.println("Range is expected value  -- PASSED.");
					}
					// that takes care of morphism within morphism, now check remainder of composite morphism
					if(!(m.getMap() instanceof String))
						System.out.println(m.getMap().getClass()+" composite relation map isnt String; value:"+m+ "FAIL");
					else {
						System.out.println("Composite relation map is expected value  -- PASSED.");
						if(!m.getMap().equals("has identity"))
							System.out.println("Composite relation Map doesnt match 'has identity'"+m+" FAIL");
						else
							System.out.println("Composite relation map is expected value  -- PASSED.");
					}
				}
			}
			++recs;
		}
		System.out.println("BATTERY1A verification SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Retrieved "+recs);
	}

	/**
	 * Parallel stream
	 * @throws Exception
	 */
	public static void battery2() throws Exception {
		System.out.println("Battery2 ");
		long tims = System.currentTimeMillis();
		int recs = 0;
		Stream streamPrimary = null;
		Stream streamSecondary = null;
		for(i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			if(streamPrimary != null)
				session.setStream(streamPrimary);
			streamPrimary = session.findStream(fkey, "Has unit", Long.valueOf(i));
			Optional<?> o =  ((Stream) streamPrimary.parallel()).findFirst();
			if(o.isPresent()) {
				System.out.println(i+".) Obtained primary Optional relation -- PASSED");
				if(streamSecondary != null)
					session.setStream(streamSecondary);
				streamSecondary = session.findStream(((Result)o.get()).get(), '*', '*');
				Optional<?> p = ((Stream) streamSecondary.parallel()).findFirst();
				if(p.isPresent()) {
					System.out.println(i+".) Obtained secondary Optional relation -- PASSED");
					Result c = (Result) p.get();
					if(!(c.get() instanceof AbstractRelation))
						System.out.println(c.get().getClass()+" isnt AbstractRelation; value:"+c.get()+" FAIL");
					else {
						// main morphism
						Relation m = (Relation) c.get();
						System.out.println("Obtained Primary Relation from secondary Result  -- PASSED.");
						if(!(m.getDomain() instanceof AbstractRelation)) 
							System.out.println(m.getDomain().getClass()+" isnt AbstractRelation; value:"+m+" FAIL");
						else {
							System.out.println("Obtained Domain Relation  -- PASSED.");
							// morphism in domain "has unit"
							Relation d = (Relation) m.getDomain();
							if(!(d.getDomain() instanceof String))
								System.out.println(d.getDomain().getClass()+" domain isnt String; value:"+d+" FAIL");
							else {
								System.out.println("Obtained Relation Domain value  -- PASSED.");
								if(!d.getDomain().equals(fkey))
									System.out.println("Domain doesnt match "+fkey+" FAIL");
								else
									System.out.println("Domain matches fkey  -- PASSED.");
							}
							if(!(d.getMap() instanceof String))
								System.out.println(d.getMap().getClass()+" map isnt String; value:"+d);
							else {
								System.out.println("Map is expected vale  -- PASSED.");
								if(!d.getMap().equals("Has unit"))
									System.out.println("Map doesnt match 'Has unit'"+d+" FAIL");
							}
							if(!(d.getRange() instanceof Long))
								System.out.println(d.getRange().getClass()+" range isnt Long; value:"+d+" FAIL");
							else {
								System.out.println("Map is expected value  -- PASSED.");
								if(!d.getRange().equals(Long.valueOf(i)))
									System.out.println("Range doesnt match "+i+" FAIL");
								else
									System.out.println("Range is expected value  -- PASSED.");
							}
							// that takes care of morphism within morphism, now check remainder of composite morphism
							if(!(m.getMap() instanceof String))
								System.out.println(m.getMap().getClass()+" composite relation map isnt String; value:"+m+ "FAIL");
							else {
								System.out.println("Composite relation map is expected value  -- PASSED.");
								if(!m.getMap().equals("has identity"))
									System.out.println("Composite relation Map doesnt match 'has identity'"+m+" FAIL");
								else
									System.out.println("Composite relation map is expected value  -- PASSED.");
							}
						}
					}
				} else
					System.out.println("Failed to find any result set for "+o.get()+" FAIL!");	
			} else
				System.out.println("Failed to find any result set for domain:"+fkey+" FAIL!");
			++recs;
		}
		System.out.println("BATTERY2 verification SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Retrieved "+recs);
	}
	/**
	 * Sequential stream
	 * @throws Exception
	 */
	public static void battery2A() throws Exception {
		System.out.println("Battery2 ");
		long tims = System.currentTimeMillis();
		int recs = 0;
		Stream streamPrimary = null;
		Stream streamSecondary = null;
		for(i = min; i < max; i++) {
			fkey = key + String.format(uniqKeyFmt, i);
			if(streamPrimary != null)
				session.setStream(streamPrimary);
			streamPrimary = (Stream)session.findStream(fkey, "Has unit", Long.valueOf(i));
			Optional<?> o =  streamPrimary.findFirst();
			if(o.isPresent()) {
				System.out.println(i+".) Obtained primary Optional relation -- PASSED");
				if(streamSecondary != null)
					session.setStream(streamSecondary);
				streamSecondary = (Stream)session.findStream(((Result)o.get()).get(), '*', '*');
				Optional<?> p = streamSecondary.findFirst();
				if(p.isPresent()) {
					System.out.println(i+".) Obtained secondary Optional relation -- PASSED");
					Result c = (Result) p.get();
					if(!(c.get() instanceof AbstractRelation))
						System.out.println(c.get().getClass()+" isnt AbstractRelation; value:"+c.get()+" FAIL");
					else {
						// main morphism
						Relation m = (Relation) c.get();
						System.out.println("Obtained Primary Relation from secondary Result  -- PASSED.");
						if(!(m.getDomain() instanceof AbstractRelation)) 
							System.out.println(m.getDomain().getClass()+" isnt AbstractRelation; value:"+m+" FAIL");
						else {
							System.out.println("Obtained Domain Relation  -- PASSED.");
							// morphism in domain "has unit"
							Relation d = (Relation) m.getDomain();
							if(!(d.getDomain() instanceof String))
								System.out.println(d.getDomain().getClass()+" domain isnt String; value:"+d+" FAIL");
							else {
								System.out.println("Obtained Relation Domain value  -- PASSED.");
								if(!d.getDomain().equals(fkey))
									System.out.println("Domain doesnt match "+fkey+" FAIL");
								else
									System.out.println("Domain matches fkey  -- PASSED.");
							}
							if(!(d.getMap() instanceof String))
								System.out.println(d.getMap().getClass()+" map isnt String; value:"+d);
							else {
								System.out.println("Map is expected vale  -- PASSED.");
								if(!d.getMap().equals("Has unit"))
									System.out.println("Map doesnt match 'Has unit'"+d+" FAIL");
							}
							if(!(d.getRange() instanceof Long))
								System.out.println(d.getRange().getClass()+" range isnt Long; value:"+d+" FAIL");
							else {
								System.out.println("Map is expected value  -- PASSED.");
								if(!d.getRange().equals(Long.valueOf(i)))
									System.out.println("Range doesnt match "+i+" FAIL");
								else
									System.out.println("Range is expected value  -- PASSED.");
							}
							// that takes care of morphism within morphism, now check remainder of composite morphism
							if(!(m.getMap() instanceof String))
								System.out.println(m.getMap().getClass()+" composite relation map isnt String; value:"+m+ "FAIL");
							else {
								System.out.println("Composite relation map is expected value  -- PASSED.");
								if(!m.getMap().equals("has identity"))
									System.out.println("Composite relation Map doesnt match 'has identity'"+m+" FAIL");
								else
									System.out.println("Composite relation map is expected value  -- PASSED.");
							}
						}
					}
				} else
					System.out.println("Failed to find any result set for "+o.get()+" FAIL!");	
			} else
				System.out.println("Failed to find any result set for domain:"+fkey+" FAIL!");
			++recs;
		}
		System.out.println("BATTERY2 verification SUCCESS in "+(System.currentTimeMillis()-tims)+" ms. Retrieved "+recs);
	}

}
