package org.chk.jdbc;

public class BaseQueryCondition implements Cloneable {

	protected int page = 1;
	protected int pageSize = 1;
	protected int maxPageSize = 100;
	protected String sort;
	protected SortBuilder sortBuilder;

	public void beforeQuery() {
	}

	public void trimToNull() {
		Utils.trimToNull(this);
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	public BaseQueryCondition clone(int pageSize) {
		BaseQueryCondition t = (BaseQueryCondition) clone();
		t.maxPageSize = pageSize;
		t.setPageSize(pageSize);
		return t;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = Math.max(1, page);
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		pageSize = Math.min(pageSize, maxPageSize);
		pageSize = Math.max(pageSize, 1);
		this.pageSize = pageSize;
	}

	public int getOffset() {
		return (page - 1) * pageSize;
	}

	public int getLen() {
		return pageSize;
	}

	public int getBegin() {
		return (page - 1) * pageSize + 1;
	}

	public int getEnd() {
		return getBegin() + pageSize - 1;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public SortBuilder buildSort(String defaultColumn) {
		this.sortBuilder = new SortBuilder(defaultColumn);
		return this.sortBuilder;
	}

	public SortBuilder getSortBuilder() {
		return sortBuilder;
	}
}
