package fr.sparna.rdf.gexf.converter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.sparna.rdf.gexf.SparqlRequest;
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
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.StaxGraphWriter;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.data.AttributeListImpl;

public class RdfToGexfParserImpl implements RdfToGexfParserIfc{

	private static Logger log = LoggerFactory.getLogger(RdfToGexfParserImpl.class.getName());

	
	private String startDateProperty;
	private String endDateProperty;
	private Properties propertyWeights;
	
	private transient Repository repo;

	@Override
	public Gexf buildGexf(
		Repository repo,
		String edgesQuery,
		String labelsQuery,
		String attributesQuery,
		String datesQuery,
		String parentsQuery
	) throws FileNotFoundException, IOException {

		// defer to a separate class
		RdfToGexfParserBuilder builder = new RdfToGexfParserBuilder(
			repo,
			edgesQuery,
			labelsQuery,
			attributesQuery,
			datesQuery,
			parentsQuery
		);			

		return builder.buildGexf();

	}

	@Override
	public Gexf rdfToGexf(Repository repo, String startDateProperty, String endDateProperty, Properties propertyWeights) throws FileNotFoundException, IOException {

		this.startDateProperty = startDateProperty;
		this.endDateProperty = endDateProperty;
		this.propertyWeights = propertyWeights;
		this.repo=repo;
		
		// création du graphe
		Gexf gexf=GexfFactory.newGexf("SPARNA", "rdf 2 gephi");
		Graph graph =gexf.getGraph();
		graph.setDefaultEdgeType(EdgeType.DIRECTED);

		// set in dynamic mode if proper options are enabled
		if(this.endDateProperty != null || this.startDateProperty != null) {
			graph.setMode(Mode.DYNAMIC).setTimeType("date");
		}		

		//Liste des attributs
		AttributeList attrList = new AttributeListImpl(AttributeClass.NODE);	

		for (String property : getAllLiteralProperties()) {
			log.debug("Adding property "+property);
			int index=property.contains("#")?property.lastIndexOf("#"):property.lastIndexOf("/");
			String label=property.substring(index+1);
			attrList.createAttribute(property, AttributeType.STRING, label);
		}
		//Ajout de la liste des attributs au graphe
		graph.getAttributeLists().add(attrList);

		//requête sparql 
		String sparqlRequest=SparqlRequest.QUERY_LIST_NODES;
		if(this.startDateProperty != null){
			sparqlRequest=sparqlRequest.replaceAll("_OPTIONSTART_", "OPTIONAL{?node <"+this.startDateProperty+"> ?start.}");
		}else{
			sparqlRequest=sparqlRequest.replaceAll("_OPTIONSTART_", " ");
		}
		
		if(this.endDateProperty != null){
			sparqlRequest=sparqlRequest.replaceAll("_OPTIONEND_", "OPTIONAL{?node <"+this.endDateProperty+"> ?end.}");
		}else{
			sparqlRequest=sparqlRequest.replaceAll("_OPTIONEND_", " ");
		}
		
		SimpleDateFormat formatter=new SimpleDateFormat("yyyy-mm-dd");
		//Création des noeuds
		try(RepositoryConnection c = repo.getConnection()) {
			log.debug("Adding nodes");

			class MyTupleQueryResultHandler extends AbstractTupleQueryResultHandler {

				public int counter = 0;
				public Map<String, Node> nodesMap = new HashMap<String, Node>();

				@Override
				public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {					
					
					if(bindingSet.getValue("node")!=null){
						Resource currentNode = (Resource)bindingSet.getValue("node");

						counter++;
						
						log.debug("Node : "+counter+" ("+currentNode.stringValue()+")");
						Node node = graph.createNode(currentNode.stringValue());
						// store in cache
						nodesMap.put(currentNode.stringValue(), node);

						//Ajout du label
						if(bindingSet.getValue("label")!=null){
							node.setLabel(bindingSet.getValue("label").stringValue());
						} else {
							// really nothing ? use the URI as node label
							node.setLabel(bindingSet.getValue("node").stringValue());
						}
						
						//pour chaque uri on récupère tous les attributs et objets
						List<NodeAttribute> datas = getAttributes(currentNode.stringValue());
						for (NodeAttribute data : datas) {
							String property=data.getPredicat();
							int index=property.contains("#")?property.lastIndexOf("#"):property.lastIndexOf("/");
							String label=property.substring(index+1);
							Attribute at=new AttributeListImpl(AttributeClass.NODE).createAttribute(property,AttributeType.STRING,label);
							node.getAttributeValues().addValue(at, data.getValue());
							// node.getShapeEntity().setNodeShape(NodeShape.DIAMOND).setUri(bindingSet.getValue("node").stringValue());
						}
						
						//Ajout des dates si elles existent
						try {
							if(bindingSet.getValue("start")!=null){
								node.setStartValue(formatter.parse(bindingSet.getValue("start").stringValue()));
							}
							if(bindingSet.getValue("end")!=null){
								node.setEndValue(formatter.parse(bindingSet.getValue("end").stringValue()));
							}
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				}
			};

			MyTupleQueryResultHandler myNodeHandler = new MyTupleQueryResultHandler();

			RepositoryConnections.select(c, sparqlRequest, myNodeHandler);
			log.debug("Done adding nodes");
			
			
			//On récupère les edges
			this.addEdges(graph,c,myNodeHandler.nodesMap);
		}

		return gexf;
	}

	/**
	 * @return
	 * Retourne la liste de toutes les propriétés dont
	 * les objets sont de Literal
	 */
	private List<String> getAllLiteralProperties() {

		List<String> properties=new ArrayList<String>();
		try(RepositoryConnection c = repo.getConnection()) {
			String sparqlRequest=SparqlRequest.LIST_LITERAL_PROPERTIES;
			
			if(this.endDateProperty != null || this.startDateProperty != null) {
				List<String> filters = new ArrayList<String>();
				if(this.startDateProperty != null) {
					filters.add("?p != <"+this.startDateProperty+">");
				}
				if(this.endDateProperty != null) {
					filters.add("?p != <"+this.endDateProperty+">");
				}
				
				sparqlRequest=sparqlRequest.replaceAll(
						"_FILTER_", 
						" FILTER("+String.join(" && ", filters)+")"
						);
			} else {
				sparqlRequest=sparqlRequest.replaceAll(
						"_FILTER_", 
						""
				);
			}

			RepositoryConnections.select(c, sparqlRequest, new AbstractTupleQueryResultHandler() {
				@Override
				public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
					if(bindingSet.getValue("p")!=null){
						properties.add(bindingSet.getValue("p").stringValue());
					}
				}
			});
		}	
		return properties;
	}

	/**
	 * Ajout des edges au graphe
	 * @param graph
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * 
	 */
	private void addEdges(Graph graph, RepositoryConnection c, Map<String, Node> nodes) throws FileNotFoundException, IOException {

		log.debug("Adding edges");

		//Attributes
		AttributeList attrList = new AttributeListImpl(AttributeClass.EDGE);
		Attribute at=attrList.createAttribute("type",AttributeType.STRING,"type");
		graph.getAttributeLists().add(attrList);
		
		String sparqlRequest=SparqlRequest.LIST_EDGES;
		RepositoryConnections.select(c, sparqlRequest, new AbstractTupleQueryResultHandler() {
			
			int counter=0;
			
			@Override
			public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
				if(bindingSet.getValue("s")!=null) {
					log.debug("Edge : "+counter++);
					String predicat=bindingSet.getValue("p").stringValue();
					int index=predicat.contains("#")?predicat.lastIndexOf("#"):predicat.lastIndexOf("/");
					String label=predicat.substring(index+1);

					try {
						Edge edge = nodes.get(bindingSet.getValue("s").stringValue()).connectTo(
							UUID.randomUUID().toString(),
							label,
							nodes.get(bindingSet.getValue("o").stringValue())
						).setEdgeType(EdgeType.DIRECTED);
						
						// set edge type attribute
						edge.getAttributeValues().addValue(at, label);

						if(propertyWeights != null){
							String value=propertyWeights.getProperty(label);
							if(value!=null){
								float weight=Float.parseFloat(value);
								edge.setWeight(weight);
							}
						}
						

					} catch (Exception e) {
						e.printStackTrace();
					}
				}	
			}
		});
		log.debug("Done adding edges");
	}

	/**
	 * 
	 * @param uri : Représente l'uri d'un noeud
	 * @return Une liste des Triplets ayant pour sujet l'uri du noeud passé en paramètre
	 */
	private List<NodeAttribute> getAttributes(String uri) {
		log.trace("Getting attributes...");
		String sparqlRequest=SparqlRequest.QUERY_READ_RESOURCE_LITERALS_AND_TYPE.replaceAll("_node", uri);
		
		if(this.endDateProperty != null || this.startDateProperty != null) {
			List<String> filters = new ArrayList<String>();
			if(this.startDateProperty != null) {
				filters.add("?predicat != <"+this.startDateProperty+">");
			}
			if(this.endDateProperty != null) {
				filters.add("?predicat != <"+this.endDateProperty+">");
			}
			
			sparqlRequest=sparqlRequest.replaceAll(
					"_FILTER_", 
					" FILTER("+String.join(" && ", filters)+")"
					);
		} else {
			sparqlRequest=sparqlRequest.replaceAll(
					"_FILTER_", 
					""
			);
		}		

		List<NodeAttribute> list=new ArrayList<NodeAttribute>();
		try(RepositoryConnection c = repo.getConnection()) {

			RepositoryConnections.select(c, sparqlRequest, new AbstractTupleQueryResultHandler() {
				@Override
				public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {					
					if(bindingSet.getValue("node")!=null){
						NodeAttribute node=new NodeAttribute();
						node.setSubject(bindingSet.getValue("node").stringValue());
						node.setPredicat(bindingSet.getValue("predicat").stringValue());
						node.setValue(bindingSet.getValue("object").stringValue());
						list.add(node);			
					}
				}
			});
		}
		log.trace("Done getting atrributes");
		return list;
	}

	
	public static void writeGexf(Gexf gexf, File outFile) throws FileNotFoundException, IOException {
		//sauvegarde du fichier gexf
		StaxGraphWriter graphWriter = new StaxGraphWriter();
		Writer out;
		try {
			out =  new FileWriter(outFile, false);
			graphWriter.writeToStream(gexf, out, "UTF-8");
			log.debug("path to gexf file ->"+outFile.getAbsolutePath());
			log.info("Done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
