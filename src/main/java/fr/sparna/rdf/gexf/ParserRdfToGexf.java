package fr.sparna.rdf.gexf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.sparna.rdf.rdf4j.toolkit.query.Perform;
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
import it.uniroma1.dis.wsngroup.gexf4j.core.viz.NodeShape;

public class ParserRdfToGexf implements RdfToGexfIfc{

	private static Logger log = LoggerFactory.getLogger(ParserRdfToGexf.class.getName());

	private Repository repo;

	private GexfCli args;

	private Properties property;

	@Override
	public void rdfToGexf(Object o) throws FileNotFoundException, IOException {


		//chargement du fichier rdf et stockage dans le repository
		GexfCli args=(GexfCli)o;
		this.args=args;
		RepositorySupplier repositorySupplier=new RepositorySupplier(
				new FileInputStream(args.getInput()),
				Rio.getParserFormatForFileName(args.getInput())
				.orElse(RDFFormat.RDFXML)
				);

		this.repo=repositorySupplier.getRepository();

		//Chargement du fichier de config
		if(args.getConfig()!=null){
			this.property = new java.util.Properties();
			this.property.load(new FileInputStream(new File(args.getConfig())));
		}
		
		// création du graphe
		Gexf gexf=GexfClass.getGexf();
		Graph graph =gexf.getGraph();
		graph.setDefaultEdgeType(EdgeType.DIRECTED);

		// set in dynamic mode if proper options are enabled
		if(args.getEndDateProperty() != null || args.getStartDateProperty() != null) {
			graph.setMode(Mode.DYNAMIC).setTimeType("date");
		}		

		//Liste des attributs
		AttributeList attrList = new AttributeListImpl(AttributeClass.NODE);	

		for (String property  : getAllLiteralProperties()) {
			log.debug("Adding property "+property);
			int index=property.contains("#")?property.lastIndexOf("#"):property.lastIndexOf("/");
			String label=property.substring(index+1);
			attrList.createAttribute(property, AttributeType.STRING, label);
		}
		//Ajout de la liste des attributs au graphe
		graph.getAttributeLists().add(attrList);

		//requête sparql 
		String sparqlRequest=SparqlRequest.QUERY_LIST_NODES;
		if(args.getStartDateProperty() != null){
			sparqlRequest=sparqlRequest.replaceAll("_OPTIONSTART_", "OPTIONAL{?node <"+args.getStartDateProperty()+"> ?start.}");
		}else{
			sparqlRequest=sparqlRequest.replaceAll("_OPTIONSTART_", " ");
		}
		
		if(args.getEndDateProperty() != null){
			sparqlRequest=sparqlRequest.replaceAll("_OPTIONEND_", "OPTIONAL{?node <"+args.getEndDateProperty()+"> ?end.}");
		}else{
			sparqlRequest=sparqlRequest.replaceAll("_OPTIONEND_", " ");
		}
		
		SimpleDateFormat formatter=new SimpleDateFormat("yyyy-mm-dd");
		//Création des noeuds
		try(RepositoryConnection c = repo.getConnection()) {
			log.debug("Nodes");
			Perform.on(c).select(sparqlRequest, new AbstractTupleQueryResultHandler() {
				@Override
				public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {					
					
					int counter=0;
					
					if(bindingSet.getValue("node")!=null){
						Resource currentNode = (Resource)bindingSet.getValue("node");
						
						log.debug("Node : "+(counter++)+" ("+currentNode.stringValue()+")");
						Node node = graph.createNode(currentNode.stringValue());

						//Ajout du label
						if(bindingSet.getValue("label")!=null){
							node.setLabel(bindingSet.getValue("label").stringValue());
						}
						
						//pour chaque uri on récupère tous les attributs et objets
						List<NodeAttribute> datas=getTriplets(currentNode.stringValue());
						for (NodeAttribute data : datas) {
							String property=data.getPredicat();
							int index=property.contains("#")?property.lastIndexOf("#"):property.lastIndexOf("/");
							String label=property.substring(index+1);
							Attribute at=new AttributeListImpl(AttributeClass.NODE).createAttribute(property,AttributeType.STRING,label);
							node.getAttributeValues().addValue(at, data.getValue());
							node.getShapeEntity().setNodeShape(NodeShape.DIAMOND).setUri(bindingSet.getValue("node").stringValue());
						}
						
						//Ajout des dates si elles existent
						try {
							if(bindingSet.getValue("start")!=null){
								node.setStartValue(formatter.parse(bindingSet.getValue("start").stringValue()));
							}
							if(bindingSet.getValue("end")!=null){
								node.setEndValue(formatter.parse(bindingSet.getValue("end").stringValue()));
							}
						}catch (ParseException e) {
							e.printStackTrace();
						}

					}
				}

			});
			log.debug("End Nodes");
			
			
			//On récupère les edges
			this.addEdges(graph,c);
		}

		//sauvegarde du fichier gexf
		StaxGraphWriter graphWriter = new StaxGraphWriter();
		File f = new File(args.getOutput());
		Writer out;
		try {
			out =  new FileWriter(f, false);
			graphWriter.writeToStream(gexf, out, "UTF-8");
			log.debug("path to gexf file ->"+f.getAbsolutePath());
			log.info("Done");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	/**
	 * 
	 * @return
	 * Retourne la liste de toutes les propriétés dont
	 * les objets sont de Literal
	 */
	private List<String> getAllLiteralProperties() {

		List<String> properties=new ArrayList<String>();
		try(RepositoryConnection c = repo.getConnection()) {
			String sparqlRequest=SparqlRequest.LIST_LITERAL_PROPERTIES;
			
			if(args.getEndDateProperty() != null || args.getStartDateProperty() != null) {
				List<String> filters = new ArrayList<String>();
				if(args.getStartDateProperty() != null) {
					filters.add("?p != <"+args.getStartDateProperty()+">");
				}
				if(args.getEndDateProperty() != null) {
					filters.add("?p != <"+args.getEndDateProperty()+">");
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

			Perform.on(c).select(sparqlRequest, new AbstractTupleQueryResultHandler() {
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
	private void addEdges(Graph graph, RepositoryConnection c) throws FileNotFoundException, IOException {

		log.debug("Edges");
		//Attributes
		AttributeList attrList = new AttributeListImpl(AttributeClass.EDGE);
		Attribute at=attrList.createAttribute("type",AttributeType.STRING,"type");
		graph.getAttributeLists().add(attrList);
		
		String sparqlRequest=SparqlRequest.LIST_EDGES;
		Perform.on(c).select(sparqlRequest, new AbstractTupleQueryResultHandler() {
			
			int counter=0;
			
			@Override
			public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
				if(bindingSet.getValue("s")!=null) {
					log.debug("Edge : "+counter++);
					String predicat=bindingSet.getValue("p").stringValue();
					int index=predicat.contains("#")?predicat.lastIndexOf("#"):predicat.lastIndexOf("/");
					String label=predicat.substring(index+1);

					try {
						Edge edge = graph.getNode(bindingSet.getValue("s").stringValue())
								.connectTo(
										UUID.randomUUID().toString(),
										label,
										graph.getNode(bindingSet.getValue("o").stringValue()
												)
										).setEdgeType(EdgeType.DIRECTED);
						if(property!=null){
							String value=property.getProperty(label);
							if(value!=null){
								float weight=Float.parseFloat(value);
								edge.setWeight(weight);
							}
						}
						edge.getAttributeValues().addValue(at, label);

					} catch (Exception e) {
						log.debug("warning -> "+e.getMessage());
					}
				}	
			}
		});
		log.debug("Fin edges");
	}

	/**
	 * 
	 * @param uri : Représente l'uri d'un noeud
	 * @return
	 * Une liste des Triplets ayant pour uri l'uri du noeud passé en paramètre
	 */
	private List<NodeAttribute> getTriplets(String uri) {
		log.debug("Traitement des triplets");
		String sparqlRequest=SparqlRequest.QUERY_READ_RESOURCE_LITERALS.replaceAll("_node", uri);
		
		if(args.getEndDateProperty() != null || args.getStartDateProperty() != null) {
			List<String> filters = new ArrayList<String>();
			if(args.getStartDateProperty() != null) {
				filters.add("?predicat != <"+args.getStartDateProperty()+">");
			}
			if(args.getEndDateProperty() != null) {
				filters.add("?predicat != <"+args.getEndDateProperty()+">");
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

			Perform.on(c).select(sparqlRequest, new AbstractTupleQueryResultHandler() {
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
		log.debug("Fin du traitement des triplets");
		return list;
	}
}
