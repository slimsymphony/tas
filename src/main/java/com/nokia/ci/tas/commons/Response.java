package com.nokia.ci.tas.commons;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 * Response from Communicator for monitor request.
 * 
 * @author Frank Wang
 * @since Jul 31, 2012
 */
public class Response extends Message<Response> {

	private List<MessageItem<?>> items = new ArrayList<MessageItem<?>>();

	public void addItem( MessageItem<?> item ) {
		this.items.add( item );
	}

	public List<MessageItem<?>> getItems() {
		return items;
	}

	public void setItems( List<MessageItem<?>> items ) {
		this.items = items;
	}

	@Override
	public String toJson() {
		return MonitorUtils.toJson( this );
	}

	/**
	 * TypeAdpater for transform Response class to Json.
	 * 
	 * @author Frank Wang
	 * @since Jul 31, 2012
	 */
	public static class ResponseTypeAdpater implements JsonSerializer<Response>, JsonDeserializer<Response> {
		@Override
		public JsonElement serialize( Response src, Type typeOfSrc, JsonSerializationContext context ) {
			if ( src == null )
				return null;
			JsonObject je = new JsonObject();
			JsonArray arr = new JsonArray();
			for ( MessageItem<?> it : src.getItems() ) {
				JsonObject item = null;
				if ( it.getItemName().equalsIgnoreCase( "TestNodeAdapter" ) ) {
					TestNodeAdapter ta = ( TestNodeAdapter ) it;
					item = new JsonObject();
					item.add( "nodes", context.serialize( ta.getNodes() ) );
					item.addProperty( "itemName", it.getItemName() );
				} else if ( it.getItemName().equalsIgnoreCase( "Client" ) ) {
					TacAdapter ta = ( TacAdapter ) it;
					Map<String, List<Test>> clients = ta.getClients();
					item = new JsonObject();
					item.add( "clients", context.serialize( clients ) );
					item.addProperty( "itemName", it.getItemName() );
				} else if ( it.getItemName().equalsIgnoreCase( "TestNode" ) ) {
					item = new JsonObject();
					for ( Field field : TestNode.class.getDeclaredFields() ) {
						try {
							field.setAccessible( true );
							if ( field.get( it ) == null )
								continue;
							String fn = field.getName();
							if ( fn.equalsIgnoreCase( "memory" ) || fn.equalsIgnoreCase( "cpu" ) || fn.equalsIgnoreCase( "disk" ) || fn.equalsIgnoreCase( "os" ) ) {
								try {
									item.add( fn, context.serialize( field.get( it ), new HashMap<String, String>().getClass() ) );
								} catch ( Exception e ) {
									e.printStackTrace();
								}
							} else if ( fn.equalsIgnoreCase( "network" ) ) {
								try {
									item.add( fn, context.serialize( field.get( it ), new ArrayList<String>().getClass() ) );
								} catch ( Exception e ) {
									e.printStackTrace();
								}
							} else if ( fn.equalsIgnoreCase( "processes" ) ) {
								try {
									item.add( fn, context.serialize( field.get( it ), new HashMap<Integer, String>().getClass() ) );
								} catch ( Exception e ) {
									e.printStackTrace();
								}
							}
						} catch ( Exception e1 ) {
							e1.printStackTrace();
						}
					}
					item.addProperty( "itemName", it.getItemName() );
				} else if ( it.getItemName().equalsIgnoreCase( "Product" ) ) {
					item = context.serialize( it ).getAsJsonObject();
				} else if ( it.getItemName().equalsIgnoreCase( "Test" ) ) {
					TestAdapter ta = ( TestAdapter ) it;
					Map<Test, String> tests = ta.getTests();
					item = new JsonObject();
					item.addProperty( "waitingRequests", ta.getWaitingRequests() );
					JsonArray ja = new JsonArray();
					JsonObject ts = null;
					if ( tests != null ) {
						for ( Test test : tests.keySet() ) {
							ts = new JsonObject();
							ts.addProperty( context.serialize( test ).toString(), tests.get( test ) );
							ja.add( ts );
						}
					}
					item.add( "tests", ja );
					item.addProperty( "itemName", it.getItemName() );
				} else if ( it.getItemName().equalsIgnoreCase( "Log" ) ) {
					item = context.serialize( it ).getAsJsonObject();
				}

				arr.add( item );
			}
			je.add( "items", arr );
			return je;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public Response deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException {
			Response response = new Response();
			JsonArray arr = json.getAsJsonObject().get( "items" ).getAsJsonArray();
			for ( JsonElement j : arr ) {
				JsonObject je = j.getAsJsonObject();
				String itemname = je.getAsJsonObject().get( "itemName" ).getAsString();
				if ( itemname.equalsIgnoreCase( "TestNodeAdapter" ) ) {
					TestNodeAdapter tn = new TestNodeAdapter();
					tn.setItemName( itemname );
					tn.setNodes( (Map<String,List<Product>>)context.deserialize( je.get( "nodes" ), new TypeToken<Map<String, List<Product>>>() {}.getType() ) );
					response.addItem( tn );
				} else if ( itemname.equalsIgnoreCase( "Client" ) ) {
					TacAdapter ta = new TacAdapter();
					ta.setItemName( itemname );
					ta.setClients( (Map<String, List<Test>>)context.deserialize( je.get( "clients" ), new TypeToken<Map<String, List<Test>>>() {
					}.getType() ) );
					response.addItem( ta );
				} else if ( itemname.equalsIgnoreCase( "TestNode" ) ) {
					TestNode tn = new TestNode();
					for ( Field field : TestNode.class.getDeclaredFields() ) {
						String fn = field.getName();
						field.setAccessible( true );
						if ( je.has( fn ) ) {
							if ( fn.equalsIgnoreCase( "memory" ) || fn.equalsIgnoreCase( "cpu" ) || fn.equalsIgnoreCase( "disk" ) || fn.equalsIgnoreCase( "os" ) ) {
								try {
									field.set( tn, context.deserialize( je.get( fn ), new HashMap<String, String>().getClass() ) );
								} catch ( Exception e ) {
									e.printStackTrace();
								}
							} else if ( fn.equalsIgnoreCase( "network" ) ) {
								try {
									field.set( tn, context.deserialize( je.get( fn ), new ArrayList<String>().getClass() ) );
								} catch ( Exception e ) {
									e.printStackTrace();
								}
							} else if ( fn.equalsIgnoreCase( "processes" ) ) {
								try {
									field.set( tn, context.deserialize( je.get( fn ), new HashMap<Integer, String>().getClass() ) );
								} catch ( Exception e ) {
									e.printStackTrace();
								}
							}
						}
					}
					response.addItem( tn );
				} else if ( itemname.equalsIgnoreCase( "Product" ) ) {
					ProductAdapter product = context.deserialize( je, ProductAdapter.class );
					response.addItem( product );
				} else if ( itemname.equalsIgnoreCase( "Test" ) ) {
					TestAdapter ta = new TestAdapter();
					if ( je.has( "waitingRequests" ) ) {
						ta.setWaitingRequests( je.get( "waitingRequests" ).getAsInt() );
					}
					if ( je.has( "tests" ) ) {
						JsonArray jar = je.get( "tests" ).getAsJsonArray();
						if ( jar.size() > 0 ) {
							ta.setTests( new HashMap<Test, String>() );
							for ( JsonElement jo : jar ) {
								for ( Entry<String, JsonElement> entry : jo.getAsJsonObject().entrySet() ) {
									ta.getTests().put( MonitorUtils.fromJson( entry.getKey(), Test.class ), entry.getValue().getAsString() );
								}
							}
						}
					}
					response.addItem( ta );
				} else if ( itemname.equalsIgnoreCase( "Log" ) ) {
					LogInfo logInfo = context.deserialize( je, LogInfo.class );
					response.addItem( logInfo );
				}
			}
			return response;
		}
	}

}
