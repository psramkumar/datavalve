/*
 * Copyright 2010, Andrew M Gibson
 *
 * www.andygibson.net
 *
 * This file is part of DataValve.
 *
 * DataValve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DataValve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with DataValve.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.fluttercode.datavalve.provider.jpa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.fluttercode.datavalve.ParameterResolver;
import org.fluttercode.datavalve.dataset.Dataset;
import org.fluttercode.datavalve.dataset.ObjectDataset;
import org.fluttercode.datavalve.params.Parameter;
import org.fluttercode.datavalve.provider.ParameterizedDataProvider;
import org.fluttercode.datavalve.testing.junit.AbstractObjectDatasetJUnitTest;

/**
 * @author Andy Gibson
 * 
 */
public class GenericJpaDatasetTest extends AbstractObjectDatasetJUnitTest<Person> {

	private JpaDataset<Person> dataset;
	
	private class JpaDataset<T> extends Dataset<T, JpaDataProvider<T>> {

		private static final long serialVersionUID = 1L;

		public JpaDataset(JpaDataProvider<T> provider) {
			super(provider);			
		}
		
	}
	private static final long serialVersionUID = 1L;
	
	//private static Logger log = LoggerFactory.getLogger(GenericJpaDatasetTest.class);

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
		dataset = buildQueryDataset();
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
			Person p = new Person(getDataFactory().getFirstName(),
					getDataFactory().getLastName());
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
		long result = dataset.getResultCount();
		assertEquals(30, result);

		dataset.getProvider().getRestrictions().add("p.id = 3");
		dataset.invalidateResultInfo();
		result = dataset.getResultCount();
		assertEquals(1, result);
	}

	public void testSimpleParameter() {				
		dataset.getProvider().getRestrictions().add("p.id = :personId");
		dataset.getProvider().getParameters().put("personId", 4l);
		List<Person> result = dataset.getResultList();
		Long val = (Long)dataset.getProvider().resolveParameter(":personId");
		assertNotNull(val);
		assertEquals(4, val.intValue());

		assertNotNull(result);

		assertEquals(1, result.size());
		assertEquals(1, dataset.getPage());
		Person p = result.get(0);
		assertEquals(new Long(4), p.getId());
	}

	public void testMissingParameter() {		
		dataset.getProvider().getRestrictions().add("p.id = #{personId}");
		List<Person> result = dataset.getResultList();

		assertNotNull(result);
		assertEquals(1, dataset.getPage());
		assertEquals(30, result.size());

	}

	public void testNullParameter() {
		dataset.getProvider().getRestrictions().add("p.id = #{personId}");
		List<Person> result = dataset.getResultList();
		dataset.getProvider().getParameters().put("personId", null);

		assertNotNull(result);

		assertEquals(30, result.size());
	}

	public void testPagination() {		
		
		assertEquals(1, dataset.getPage());
		assertEquals(false, dataset.isNextAvailable());
		assertEquals(false, dataset.isPreviousAvailable());

		dataset.setMaxRows(10);
		assertEquals(1, dataset.getPage());

		assertEquals(true, dataset.isNextAvailable());
		assertEquals(false, dataset.isPreviousAvailable());

		dataset.next();
		assertEquals(2, dataset.getPage());
		assertEquals(true, dataset.isNextAvailable());
		assertEquals(true, dataset.isPreviousAvailable());

		dataset.previous();
		assertEquals(1, dataset.getPage());
		assertEquals(true, dataset.isNextAvailable());
		assertEquals(false, dataset.isPreviousAvailable());

		dataset.last();
		assertEquals(3, dataset.getPage());
		assertEquals(false, dataset.isNextAvailable());
		assertEquals(true, dataset.isPreviousAvailable());

		dataset.previous();
		assertEquals(2, dataset.getPage());
		assertEquals(true, dataset.isNextAvailable());
		assertEquals(true, dataset.isPreviousAvailable());

	}

	public void testParameterResolverRepeatsEval() {					
		dataset.getProvider().getRestrictions().add("p.id = #{id}");
		dataset.getProvider().getRestrictions().add("p.id = #{id}");

		dataset.getProvider().addParameterResolver(new ParameterResolver() {

			long id = 20;

			public boolean resolveParameter(ParameterizedDataProvider<? extends Object> dataset,Parameter parameter) {
				if (parameter.getName().equals("#{id}")) {
					parameter.setValue(id++);
					return true;
				}
				return false;
			}

			public boolean acceptParameter(String parameter) {
				
				boolean val = parameter.startsWith("#{") && parameter.endsWith("}");
				return val;
			}
			
		});
		List<Person> results = dataset.getResultList();
		assertNotNull(results);
		assertEquals(0, results.size());
		assertEquals(0, dataset.getResultCount().intValue());

	}

	public void testQueryWithOrdering() {
		ObjectDataset<Person> qry = buildObjectDataset();
		qry.setOrderKey("id");
		// check record count
		assertEquals(getDataRowCount(), qry.getResultCount().intValue());
		List<Person> results = qry.getResultList();
		assertNotNull(results);
		assertEquals(getDataRowCount(), results.size());
	}

	public void testOrderByAscNonPaged() {
		ObjectDataset<Person> qry = buildObjectDataset();
		qry.setOrderKey("id");
		performOrderChecks(qry,true);
	}

	public void testOrderByAscPaged() {
		ObjectDataset<Person> qry = buildObjectDataset();
		qry.setMaxRows(7);
		qry.setOrderKey("id");
		performOrderChecks(qry,true);
	}

	public void testOrderByDescNonPaged() {
		ObjectDataset<Person> qry = buildObjectDataset();
		qry.setOrderKey("id");
		performOrderChecks(qry,false);
	}

	public void testOrderByDescPaged() {
		ObjectDataset<Person> qry = buildObjectDataset();
		qry.setMaxRows(7);
		qry.setOrderKey("id");
		performOrderChecks(qry,false);
	}
	
	public void testListParameter() {						
		List<Long> values = new ArrayList<Long>();
		values.add(6l);
		values.add(8l);
		dataset.getProvider().addRestriction("p.id in (:param)",values);		
		
		List<Person> result = dataset.getResultList();
		assertEquals(2, result.size());
		assertEquals(1, dataset.getPage());
		Person person = result.get(0);
		assertEquals(new Long(6), person.getId());
		person = result.get(1);
		assertEquals(new Long(8), person.getId());
		
	}
	

	protected void performOrderChecks(ObjectDataset<Person> qry, boolean isAsc) {
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
	public ObjectDataset<Person> buildObjectDataset() {
		return dataset;
	}
	
	protected JpaDataset<Person> buildQueryDataset() {
		JpaDataProvider<Person> provider = new JpaQueryProvider<Person>(em);		
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
