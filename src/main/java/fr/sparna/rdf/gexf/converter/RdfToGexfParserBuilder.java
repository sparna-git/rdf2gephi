package fr.sparna.rdf.gexf.converter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.sparna.rdf.rdf4j.RepositoryConnections;
import it.uniroma1.dis.wsngroup.gexf4j.core.Edge;
import it.uniroma1.dis.wsngroup.gexf4j.core.EdgeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;
import it.uniroma1.dis.wsngroup.gexf4j.core.Graph;
import it.uniroma1.dis.wsngroup.gexf4j.core.Mode;
import it.uniroma1.dis.wsngroup.gexf4j.core.Node;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.Attribute;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeClass;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeList;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.data.AttributeListImpl;

public class RdfToGexfParserBuilder {

	private static Logger log = LoggerFactory.getLogger(RdfToGexfParserImpl.class.getName());
	
	private transient Repository repository;
	private String edgesQuery;
	private String labelsQuery;
	private String attributesQuery;
	private String datesQuery;
	private String parentsQuery;

	

	public RdfToGexfParserBuilder(
		Repository repository,
		String edgesQuery,
		String labelsQuery,
		String attributesQuery,
		String datesQuery,
		String parentsQuery
	) {
		this.repository = repository;
		this.edgesQuery = edgesQuery;
		this.labelsQuery = labelsQuery;
		this.attributesQuery = attributesQuery;
		this.datesQuery = datesQuery;
		this.parentsQuery = parentsQuery;
	}

	public Gexf buildGexf() throws FileNotFoundException, IOException {

		// création du graphe
		Gexf gexf = GexfFactory.newGexf("rdf2gephi", "A GEXF graph converted from RDF");
		Graph graph = gexf.getGraph();
		graph.setDefaultEdgeType(EdgeType.DIRECTED);	
		
		//Attributes
		AttributeList attrList = new AttributeListImpl(AttributeClass.EDGE);
		Attribute edgeTypeAttribute = attrList.createAttribute("type",AttributeType.STRING,"type");
		graph.getAttributeLists().add(attrList);

		try(RepositoryConnection c = repository.getConnection()) {

			// populate with edges
			log.info("building edges with query ...");
			log.info(edgesQuery);
			EdgesQueryResultHandler myEdgesHandler = new EdgesQueryResultHandler(graph, edgeTypeAttribute);
			RepositoryConnections.select(c, edgesQuery, myEdgesHandler);
			log.info("Done building "+myEdgesHandler.counter+" edges"+(myEdgesHandler.counterStartDates>0?" with "+myEdgesHandler.counterStartDates+" start dates":"")+(myEdgesHandler.counterEndDates>0?" with "+myEdgesHandler.counterEndDates+" end dates":""));

			// now populate with labels
			log.info("building labels with query ...");

			// process in batches
			List<String> nodeList = new ArrayList<>(myEdgesHandler.nodesMap.keySet());

			LabelsQueryResultHandler myLabelsHandler = new LabelsQueryResultHandler(myEdgesHandler.nodesMap);
			executeInBatch(labelsQuery, c, myLabelsHandler, nodeList);
			log.info("done populating labels : "+myLabelsHandler.counter+" labels");	
			

			// now populate with attributes
			AttributesQueryResultHandler myAttributesHandler = new AttributesQueryResultHandler(graph, myEdgesHandler.nodesMap);
			executeInBatch(attributesQuery, c, myAttributesHandler, nodeList);
			log.info("done populating attributes : "+myAttributesHandler.counter+" attributes");

			// now populate with dates
			DatesQueryResultHandler myDatesHandler = new DatesQueryResultHandler(graph, myEdgesHandler.nodesMap);
			if(this.datesQuery != null) {				
				executeInBatch(datesQuery, c, myDatesHandler, nodeList);
				log.info("done populating dates : "+myDatesHandler.counter+" dates");
			}

			// now populate with parents
			ParentsQueryResultHandler myParentsHandler = new ParentsQueryResultHandler(graph, myEdgesHandler.nodesMap);
			if(this.parentsQuery != null) {				
				executeInBatch(parentsQuery, c, myParentsHandler, nodeList);
				log.info("done populating parents : "+myParentsHandler.counter+" parents");
			}
			
			log.info(myEdgesHandler.counter+" edges"+(myEdgesHandler.counterStartDates>0?" with "+myEdgesHandler.counterStartDates+" start dates":"")+(myEdgesHandler.counterEndDates>0?" with "+myEdgesHandler.counterEndDates+" end dates":""));
			log.info(myLabelsHandler.counter+" labels");
			log.info(myAttributesHandler.counter+" attributes");
			log.info(myDatesHandler.counter+" dates");
			log.info(myParentsHandler.counter+" parents");
		}

			

		return gexf;

	}

	private void executeInBatch(String query, RepositoryConnection c, TupleQueryResultHandler handler, List<String> nodeList) {
		// process in batches
		int BATCH_SIZE = 200;

		for (int i = 0; i < nodeList.size(); i += BATCH_SIZE) {
			int end = Math.min(i + BATCH_SIZE, nodeList.size());
			List<String> batch = nodeList.subList(i, end);

			String valuesClause = "VALUES ?subject { <"+batch.stream().collect(Collectors.joining("> <"))+"> }";

			// insert after final "}"
			int lastIndex = query.lastIndexOf("}");
			if (lastIndex == -1) {
				throw new RuntimeException("Invalid query");
			}

			String finalQuery = query.substring(0, lastIndex) + valuesClause + query.substring(lastIndex);
			
			log.info("batch "+i);
			log.debug(finalQuery);
			
			RepositoryConnections.select(c, finalQuery, handler);
		}	
	}

	class EdgesQueryResultHandler extends AbstractTupleQueryResultHandler {

		private Graph graph;
		private Attribute edgeTypeAttribute;
		
		public Map<String, Node> nodesMap = new HashMap<String, Node>();

		public int counter = 0;
		public int counterStartDates = 0;
		public int counterEndDates = 0;

		public EdgesQueryResultHandler(Graph graph, Attribute edgeTypeAttribute) {
			this.graph = graph;
			this.edgeTypeAttribute = edgeTypeAttribute;
		}

		@Override
		public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {	
			if(
				bindingSet.hasBinding("subject")
				&&
				bindingSet.hasBinding("edge")
				&&
				bindingSet.hasBinding("object")
			) {

				// subject
				Value subject = bindingSet.getBinding("subject").getValue();
				Node nodeS = nodesMap.get(subject.stringValue());
				if(nodeS == null) {
					nodeS = graph.createNode(subject.stringValue());
					// store in cache
					nodesMap.put(subject.stringValue(), nodeS);
				}

				// object
				Value object = bindingSet.getBinding("object").getValue();
				Node nodeO = nodesMap.get(object.stringValue());
				if(nodeO == null) {
					nodeO = graph.createNode(object.stringValue());
					// store in cache
					nodesMap.put(object.stringValue(), nodeO);
				}

				// edge
				String edgeUri = bindingSet.getBinding("edge").getValue().stringValue();
				int index = edgeUri.contains("#")?edgeUri.lastIndexOf("#"):edgeUri.lastIndexOf("/");
				String label=edgeUri.substring(index+1);
				
				Edge edge = null;
				if(!nodeS.hasEdgeTo(nodeO.getId())) {
					log.debug("Edge : "+counter+" "+subject.stringValue()+" --"+label+"--> "+object.stringValue());
					edge = nodeS.connectTo(
						UUID.randomUUID().toString(),
						label,
						nodeO
					).setEdgeType(EdgeType.DIRECTED);
					
					// set edge type attribute
					edge.getAttributeValues().addValue(edgeTypeAttribute, label);
				} else {
					final Node lookupNode = nodeO;
					edge = nodeS.getEdges().stream().filter(e -> e.getTarget().equals(lookupNode)).findFirst().get();
				}

				// start
				if(bindingSet.hasBinding("start")) {
					Value start = bindingSet.getBinding("start").getValue();
					if(start != null) {
						edge.setStartValue(((Literal) start).calendarValue().toGregorianCalendar().getTime());
						counterStartDates++;
					}
				}

				// end
				if(bindingSet.hasBinding("end")) {
					Value end = bindingSet.getBinding("end").getValue();
					if(end != null) {
						edge.setEndValue(((Literal) end).calendarValue().toGregorianCalendar().getTime());
						counterEndDates++;
					}
				}


				counter++;						
				
			} else {
				log.error("binding set without expected bindings ('subject', 'edge', 'object'): "+bindingSet);
			}
		}
	}

	class LabelsQueryResultHandler extends AbstractTupleQueryResultHandler {

		private Map<String, Node> nodesMap;
		public int counter = 0;

		public LabelsQueryResultHandler(Map<String, Node> nodesMap) {
			this.nodesMap = nodesMap;
		}

		@Override
		public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {	
			if(
				bindingSet.hasBinding("subject")
				&&
				bindingSet.hasBinding("label")
			) {

				// subject
				Value subject = bindingSet.getBinding("subject").getValue();
				Node node = this.nodesMap.get(subject.stringValue());

				// just in case
				if(node != null) {
					//Ajout du label
					String label = bindingSet.getBinding("label").getValue().stringValue();
					log.debug("Setting label of : "+node.getId()+" "+label);
					counter++;
					node.setLabel(label);
				}
										
			} else {
				log.error("binding set without expected bindings ('subject', 'label'): "+bindingSet);
			}
		}
	}

	class AttributesQueryResultHandler extends AbstractTupleQueryResultHandler {

		private Graph graph;
		private Map<String, Node> nodesMap;
		public int counter = 0;

		private transient List<String> bindingNames;
		private transient AttributeList attributeList;

		public AttributesQueryResultHandler(Graph graph, Map<String, Node> nodesMap) {
			this.graph = graph;
			this.nodesMap = nodesMap;
		}

		@Override
		public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
			if(bindingNames.size() < 3) {
				log.error("binding set without expected columns: at least 3 columns");
				throw new TupleQueryResultHandlerException("binding set without expected columns: at least 3 columns");
			}

			this.bindingNames = bindingNames;
			
			if(graph.getAttributeLists().stream().filter(a -> a.getAttributeClass().equals(AttributeClass.NODE)).count() == 0) {
				this.attributeList = new AttributeListImpl(AttributeClass.NODE);
				//Ajout de la liste des attributs au graphe
				graph.getAttributeLists().add(attributeList);
			} else {
				this.attributeList = graph.getAttributeLists().stream().filter(a -> a.getAttributeClass().equals(AttributeClass.NODE)).findFirst().get();
			}
		}

		@Override
		public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {	
			// subject
			Value subject = bindingSet.getBinding(this.bindingNames.get(0)).getValue();
			Node node = this.nodesMap.get(subject.stringValue());

			if(node != null) {
				// attribute
				Value attribute = bindingSet.getBinding(this.bindingNames.get(1)).getValue();
				String attributeName = getLocalName(attribute.stringValue());

				// add to attribute list if necessary
				if(attributeList.stream().filter(a -> a.getId().equals(attributeName)).count() == 0) {
					log.info("Adding attribute "+attributeName);
					attributeList.createAttribute(attributeName, AttributeType.STRING, attributeName);
				}

				// value
				Value value = bindingSet.getBinding(this.bindingNames.get(2)).getValue();
				Attribute at = new AttributeListImpl(AttributeClass.NODE).createAttribute(attributeName,AttributeType.STRING,attributeName);
				String valueString = value instanceof org.eclipse.rdf4j.model.Resource ? getLocalName(value.stringValue()) : value.stringValue();
				node.getAttributeValues().addValue(at, valueString);
				log.trace("Setting attribute of : "+node.getId()+" "+attributeName+"="+value.stringValue());
				counter++;	
			}
		}		
	}

	class DatesQueryResultHandler extends AbstractTupleQueryResultHandler {

		private Graph graph;
		private Map<String, Node> nodesMap;
		public int counter = 0;

		public DatesQueryResultHandler(Graph graph, Map<String, Node> nodesMap) {
			this.nodesMap = nodesMap;
			this.graph = graph;
		}

		/**
		 * Sets the graph mode to dynamic if we have any result on dates
		 */
		@Override
		public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
			this.graph.setMode(Mode.DYNAMIC).setTimeType("date");
		}

		@Override
		public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {	
			if(
				bindingSet.hasBinding("subject")
				&&
				(
					bindingSet.hasBinding("start")
					||
					bindingSet.hasBinding("end")
				)
			) {

				// subject
				Value subject = bindingSet.getBinding("subject").getValue();
				Node node = this.nodesMap.get(subject.stringValue());

				if(bindingSet.hasBinding("start")) {
					Value startValue = bindingSet.getBinding("start").getValue();
					if(startValue != null) {
						node.setStartValue(((Literal) startValue).calendarValue().toGregorianCalendar().getTime());
					}
				}

				if(bindingSet.hasBinding("end")) {
					Value endValue = bindingSet.getBinding("end").getValue();
					if(endValue != null) {
						node.setEndValue(((Literal) endValue).calendarValue().toGregorianCalendar().getTime());
					}
				}

				counter++;
										
			} else {
				log.error("binding set without expected bindings ('subject' + ['start' or 'end']): "+bindingSet);
			}
		}
	}


	class ParentsQueryResultHandler extends AbstractTupleQueryResultHandler {

		private Graph graph;
		private Map<String, Node> nodesMap;
		public int counter = 0;

		public ParentsQueryResultHandler(Graph graph, Map<String, Node> nodesMap) {
			this.nodesMap = nodesMap;
			this.graph = graph;
		}

		@Override
		public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {	
			if(
				bindingSet.hasBinding("subject")
				&&
				bindingSet.hasBinding("parent")
			) {

				// subject
				Value subject = bindingSet.getBinding("subject").getValue();
				Node node = this.nodesMap.get(subject.stringValue());

				if(bindingSet.hasBinding("parent")) {
					Value parent = bindingSet.getBinding("parent").getValue();
					if(parent != null) {
						node.setPID(parent.stringValue());
						log.trace("Setting parents of : "+node.getId()+" to "+parent.stringValue());
					}
				}

				counter++;
										
			} else {
				log.error("binding set without expected bindings ('subject' + 'parent'): "+bindingSet);
			}
		}
	}


	public static String getLocalName(String fullUri) {
		int localNameIndex=fullUri.contains("#")?fullUri.lastIndexOf("#"):fullUri.lastIndexOf("/");
		return fullUri.substring(localNameIndex+1);
	}

}
