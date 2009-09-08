package org.jdataset.db.jpa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jdataset.IObjectDataset;
import org.jdataset.params.Parameter;
import org.jdataset.params.ParameterResolver;
import org.jdataset.provider.IParameterizedDataProvider;
import org.jdataset.provider.impl.jpa.JpaDataProvider;
import org.jdataset.testing.TestDataFactory;
import org.jdataset.testing.junit.AbstractObjectDatasetJUnitTest;
import org.jdataset.util.GenericDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericJpaDatasetTest extends AbstractObjectDatasetJUnitTest<Person> {

	private class JpaDataset<T> extends GenericDataset<T, JpaDataProvider<T>> {

		private static final long serialVersionUID = 1L;

		public JpaDataset(JpaDataProvider<T> provider) {
			super(provider);			
		}
		
	}
	private static final long serialVersionUID = 1L;
	
	private static Logger log = LoggerFactory.getLogger(GenericJpaDatasetTest.class);

	private EntityManagerFactory emf;
	private EntityManager em;
	private transient Connection connection;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// start up db
		try {
			Class.forName("org.hsqldb.jdbcDriver");
			connection = DriverManager.getConnection(
					"jdbc:hsqldb:mem:unit-testing-jpa", "sa", "");
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Exception during HSQL database startup.");
		}

		emf = Persistence.createEntityManagerFactory("testPU");
		em = emf.createEntityManager();
		generateTestData();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		if (em != null) {
			em.close();
		}

		if (emf != null) {
			emf.close();
		}

		// shutdown db
		try {
			connection.createStatement().execute("SHUTDOWN");
			connection.close();
		} catch (Exception ex) {
		}

	}

	public void generateTestData() {
		em.getTransaction().begin();
		for (int i = 0; i < 30; i++) {
			Person p = new Person(TestDataFactory.getFirstName(),
					TestDataFactory.getLastName());
			for (int or = 0; or < 10; or++) {
				Order order = new Order(i * 10 + or, p);
				em.persist(order);
			}
			em.persist(p);
		}
		em.getTransaction().commit();

	}

	public void testVerifydataGeneration() {

		Person p = new Person("Andy", "Gibson");
		em.getTransaction().begin();
		em.persist(p);
		em.getTransaction().commit();

		@SuppressWarnings("unchecked")
		List<Person> people = em.createQuery("select p from Person p")
				.getResultList();
		assertNotNull(people);
		assertEquals(31, people.size());

		Long res = (Long) em.createQuery(
				"select count(p) from Order p where p.person.id = 10")
				.getSingleResult();
		assertEquals(10, res.longValue());
	}

	public void testResultCount() {
		JpaDataset<Person> qry = buildQueryDataset();
		long result = qry.getResultCount();
		assertEquals(30, result);

		qry.getProvider().getRestrictions().add("p.id = 3");
		qry.invalidateResultInfo();
		result = qry.getResultCount();
		assertEquals(1, result);

	}

	public void testSimpleParameter() {
		JpaDataset<Person> qry = buildQueryDataset();
		
		qry.getProvider().getRestrictions().add("p.id = :personId");
		qry.getProvider().getParameters().put("personId", 4l);
		List<Person> result = qry.getResultList();
		Long val = (Long)qry.getProvider().resolveParameter(":personId");
		assertNotNull(val);
		assertEquals(4, val.intValue());

		assertNotNull(result);

		assertEquals(1, result.size());
		assertEquals(1, qry.getPage());
		Person p = result.get(0);
		assertEquals(new Long(4), p.getId());
	}

	public void testMissingParameter() {
		JpaDataset<Person> qry = buildQueryDataset();
		qry.getProvider().getRestrictions().add("p.id = #{personId}");
		List<Person> result = qry.getResultList();

		assertNotNull(result);
		assertEquals(1, qry.getPage());
		assertEquals(30, result.size());

	}

	public void testNullParameter() {
		JpaDataset<Person> qry = buildQueryDataset();
		qry.getProvider().getRestrictions().add("p.id = #{personId}");
		List<Person> result = qry.getResultList();
		qry.getProvider().getParameters().put("personId", null);

		assertNotNull(result);

		assertEquals(30, result.size());
	}

	public void testPagination() {
		JpaDataset<Person> qry = buildQueryDataset();
		
		assertEquals(1, qry.getPage());
		assertEquals(false, qry.isNextAvailable());
		assertEquals(false, qry.isPreviousAvailable());

		qry.setMaxRows(10);
		assertEquals(1, qry.getPage());

		assertEquals(true, qry.isNextAvailable());
		assertEquals(false, qry.isPreviousAvailable());

		qry.next();
		assertEquals(2, qry.getPage());
		assertEquals(true, qry.isNextAvailable());
		assertEquals(true, qry.isPreviousAvailable());

		qry.previous();
		assertEquals(1, qry.getPage());
		assertEquals(true, qry.isNextAvailable());
		assertEquals(false, qry.isPreviousAvailable());

		qry.last();
		assertEquals(3, qry.getPage());
		assertEquals(false, qry.isNextAvailable());
		assertEquals(true, qry.isPreviousAvailable());

		qry.previous();
		assertEquals(2, qry.getPage());
		assertEquals(true, qry.isNextAvailable());
		assertEquals(true, qry.isPreviousAvailable());

	}

	public void testParameterResolverRepeatsEval() {
		System.out.println("Resolving ");
		log.debug("Entering : resolver repeats eval test");
		JpaDataset<Person> qry = buildQueryDataset();		
		qry.getProvider().getRestrictions().add("p.id = #{id}");
		qry.getProvider().getRestrictions().add("p.id = #{id}");

		qry.getProvider().addParameterResolver(new ParameterResolver() {

			long id = 20;

			public boolean resolveParameter(IParameterizedDataProvider<? extends Object> dataset,Parameter parameter) {
				System.out.println("Resolving "+parameter);
				if (parameter.getName().equals("#{id}")) {
					parameter.setValue(id++);
					return true;
				}
				return false;
			}

			public boolean acceptParameter(String parameter) {
				
				boolean val = parameter.startsWith("#{") && parameter.endsWith("}");
				System.out.println("Do we accept param "+parameter+ " - "+val);
				return val;
			}
			
		});
		List<Person> results = qry.getResultList();
		assertNotNull(results);
		assertEquals(0, results.size());
		assertEquals(0, qry.getResultCount().intValue());

	}

	public void testQueryWithOrdering() {
		IObjectDataset<Person> qry = buildObjectDataset();
		qry.setOrderKey("id");
		// check record count
		assertEquals(getDataRowCount(), qry.getResultCount().intValue());
		List<Person> results = qry.getResultList();
		assertNotNull(results);
		assertEquals(getDataRowCount(), results.size());
	}

	public void testOrderByAscNonPaged() {
		IObjectDataset<Person> qry = buildObjectDataset();
		qry.setOrderKey("id");
		performOrderChecks(qry,true);
	}

	public void testOrderByAscPaged() {
		IObjectDataset<Person> qry = buildObjectDataset();
		qry.setMaxRows(7);
		qry.setOrderKey("id");
		performOrderChecks(qry,true);
	}

	public void testOrderByDescNonPaged() {
		IObjectDataset<Person> qry = buildObjectDataset();
		qry.setOrderKey("id");
		performOrderChecks(qry,false);
	}

	public void testOrderByDescPaged() {
		IObjectDataset<Person> qry = buildObjectDataset();
		qry.setMaxRows(7);
		qry.setOrderKey("id");
		performOrderChecks(qry,false);
	}

	protected void performOrderChecks(IObjectDataset<Person> qry, boolean isAsc) {
		Long lastValue = null;
		do {
			List<Person> results = qry.getResultList();
			for (int i = 0; i < results.size(); i++) {
				Person p = results.get(i);
				if (lastValue != null) {
					if (isAsc) {
						assertTrue(p.getId() > lastValue);
				 	} else {
				 		assertTrue(p.getId() < lastValue);
				 	}
				}
			}
			qry.next();
		} while (qry.isNextAvailable());
	}
	
	@Override
	public IObjectDataset<Person> buildObjectDataset() {
		return buildQueryDataset();
	}
	
	public JpaDataset<Person> buildQueryDataset() {
		JpaDataProvider<Person> provider = new JpaDataProvider<Person>();
		provider.setEntityManager(em);
		provider.setSelectStatement("select p from Person p");
		provider.setCountStatement("select count(p) from Person p");
		provider.getOrderKeyMap().put("id", "p.id");
		provider.getOrderKeyMap().put("name", "p.lastName,p.firstName");
		provider.getOrderKeyMap().put("phone", "p.phone");
		return new JpaDataset<Person>(provider);
	}

	@Override
	public int getDataRowCount() {
		return 30;
	}
	
}