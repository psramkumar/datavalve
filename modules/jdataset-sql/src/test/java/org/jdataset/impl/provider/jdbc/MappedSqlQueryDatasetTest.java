package org.jdataset.impl.provider.jdbc;

import org.jdataset.ObjectDataset;
import org.jdataset.impl.combo.DefaultQueryDataset;
import org.jdataset.impl.provider.jdbc.MappedJdbcQueryDataset;
import org.jdataset.impl.provider.jdbc.TableRow;
import org.jdataset.provider.QueryDataProvider;

public class MappedSqlQueryDatasetTest extends BaseJdbcDatasetTest<TableRow> {
	
	private static final long serialVersionUID = 1L;
	
	public ObjectDataset<TableRow> buildDataset() {
		QueryDataProvider<TableRow> provider = new MappedJdbcQueryDataset(getConnection());
		provider.setSelectStatement("select * from TestValues");
		provider.setCountStatement("select count(1) from TestValues");
		provider.getOrderKeyMap().put("id", "id");
		provider.getOrderKeyMap().put("value", "value");		
		return new DefaultQueryDataset<TableRow>(provider);		
	}
	

	@Override
	public ObjectDataset<TableRow> buildObjectDataset() {
		return buildDataset();
	}

	public void testQueryWithOrdering() {
		ObjectDataset<TableRow> ds= buildDataset();
		ds.setOrderKey("id");		
		assertEquals(getDataRowCount(),ds.getResultCount().intValue());
	}
}