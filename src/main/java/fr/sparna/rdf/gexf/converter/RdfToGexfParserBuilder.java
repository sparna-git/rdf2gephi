package fr.sparna.rdf.gexf.converter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.sparna.rdf.rdf4j.toolkit.query.Perform;
import it.uniroma1.dis.wsngroup.gexf4j.core.Edge;
import it.uniroma1.dis.wsngroup.gexf4j.core.EdgeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.Gexf;
import it.uniroma1.dis.wsngroup.gexf4j.core.Graph;
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

	

	public RdfToGexfParserBuilder(
		Repository repository,
		String edgesQuery,
		String labelsQuery,
		String attributesQuery,
		String datesQuery
	) {
		this.repository = repository;
		this.edgesQuery = edgesQuery;
		this.labelsQuery = labelsQuery;
		this.attributesQuery = attributesQuery;
		this.datesQuery = datesQuery;
	}

	public Gexf buildGexf() throws FileNotFoundException, IOException {

		// création du graphe
		Gexf gexf=GexfFactory.newGexf();
		Graph graph =gexf.getGraph();
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
			Perform.on(c).select(edgesQuery, myEdgesHandler);
			log.info("Done building "+myEdgesHandler.counter+" edges");

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
			
			log.info(myEdgesHandler.counter+" edges");
			log.info(myLabelsHandler.counter+" labels");
			log.info(myLabelsHandler.counter+" attributes");
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
			Perform.on(c).select(finalQuery, handler);
		}	
	}

	class EdgesQueryResultHandler extends AbstractTupleQueryResultHandler {

		private Graph graph;
		private Attribute edgeTypeAttribute;
		public int counter = 0;
		public Map<String, Node> nodesMap = new HashMap<String, Node>();

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
				
				if(!nodeS.hasEdgeTo(nodeO.getId())) {
					log.debug("Edge : "+counter+" "+subject.stringValue()+" --"+label+"--> "+object.stringValue());
					Edge edge = nodeS.connectTo(
						UUID.randomUUID().toString(),
						label,
						nodeO
					).setEdgeType(EdgeType.DIRECTED);
					
					// set edge type attribute
					edge.getAttributeValues().addValue(edgeTypeAttribute, label);
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
				log.debug("Setting attribute of : "+node.getId()+" "+attributeName+"="+value.stringValue());
				counter++;	
			}
		}		
	}

	/*
	class AttributesQueryResultHandler extends AbstractTupleQueryResultHandler {

		private Graph graph;
		private Map<String, Node> nodesMap;
		public int counter = 0;

		private transient AttributeList attributeList;

		public AttributesQueryResultHandler(Graph graph, Map<String, Node> nodesMap) {
			this.graph = graph;
			this.nodesMap = nodesMap;
		}

		@Override
		public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
			// avoid creating a new attribute list for each batch iteration
			// only in the first iteration
			if(graph.getAttributeLists().size() > 1) {
				return;
			}

			// add all attributes to the graph
			AttributeList attributeList = new AttributeListImpl(AttributeClass.NODE);
			for (String property : bindingNames) {
				if(!property.equals("subject")) {						
					log.info("Adding property "+property);
					attributeList.createAttribute(property, AttributeType.STRING, property);
				}
			}
			//Ajout de la liste des attributs au graphe
			graph.getAttributeLists().add(attributeList);
		}

		@Override
		public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {	
			if(
				bindingSet.hasBinding("subject")
			) {

				// subject
				Value subject = bindingSet.getBinding("subject").getValue();
				Node node = this.nodesMap.get(subject.stringValue());

				if(node != null) {
					for (String aBinding : bindingSet.getBindingNames()) {
						// skip the subject
						if(aBinding.equals("subject")) {
							continue;
						}

						// get the value
						Value value = bindingSet.getBinding(aBinding).getValue();
						Attribute at=new AttributeListImpl(AttributeClass.NODE).createAttribute(aBinding,AttributeType.STRING,aBinding);
						node.getAttributeValues().addValue(at, value.stringValue());
						log.debug("Setting attribute of : "+node.getId()+" "+aBinding+"="+value.stringValue());
						counter++;					
					}
				}

				counter++;						
			} else {
				log.error("binding set without expected bindings ('subject'): "+bindingSet);
			}
		}

		@Override
		public void endQueryResult() throws TupleQueryResultHandlerException {
			// TODO Auto-generated method stub
			super.endQueryResult();
		}

		
	}
		 */


	public static String getLocalName(String fullUri) {
		int localNameIndex=fullUri.contains("#")?fullUri.lastIndexOf("#"):fullUri.lastIndexOf("/");
		return fullUri.substring(localNameIndex+1);
	}

}
