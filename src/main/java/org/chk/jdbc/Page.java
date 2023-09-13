package org.chk.jdbc;

import java.util.List;
import java.util.Map;

public class Page<T> {

	protected int number;
	protected int total;
	protected List<T> content;
	private Map<String, Object> attributes;

	public Page() {
	}

	public Page(int number, int total, List<T> content) {
		this.number = number;
		this.total = total;
		this.content = content;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public List<T> getContent() {
		return content;
	}

	public void setContent(List<T> content) {
		this.content = content;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

}
