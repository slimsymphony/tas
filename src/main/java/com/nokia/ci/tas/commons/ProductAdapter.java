package com.nokia.ci.tas.commons;

import java.util.List;

/**
 * Product adapter for Products.
 * 
 * @author Frank Wang
 * @since Jul 31, 2012
 */
public class ProductAdapter extends MessageItem<ProductAdapter> {
	
	public ProductAdapter() {
		this.setItemName( "Product" );
	}
	
	private List<Product> products;
	
	public List<Product> getProducts() {
		return products;
	}
	public void setProducts( List<Product> products ) {
		this.products = products;
	}
	
	@Override
	public void free() {
		if(products!=null)
			products.clear();
	}
}
