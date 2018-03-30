package fr.sparna.rdf.gexf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ExceptionTypeFilter;

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

		// création du graphe
		Gexf gexf=GexfClass.getGexf();
		Graph graph =gexf.getGraph();
		graph.setDefaultEdgeType(EdgeType.DIRECTED).setMode(Mode.DYNAMIC);

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

		//requête sparql ramenant tous les noeuds et leur libellés
		String sparqlRequest=SparqlRequest.QUERY_LIST_NODES;

		//Création des noeuds
		try(RepositoryConnection c = repo.getConnection()) {

			Perform.on(c).select(sparqlRequest, new AbstractTupleQueryResultHandler() {
				@Override
				public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {					
					if(bindingSet.getValue("node")!=null){

						Node node = graph.createNode(bindingSet.getValue("node").stringValue());
						if(bindingSet.getValue("label")!=null){
							node.setLabel(bindingSet.getValue("label").stringValue());
						}
						//pour chaque uri on récupère tous les attributs et objets
						List<NodeAttribute> datas=getTriplets(bindingSet.getValue("node").stringValue());
						for (NodeAttribute data : datas) {
							String property=data.getPredicat();
							int index=property.contains("#")?property.lastIndexOf("#"):property.lastIndexOf("/");
							String label=property.substring(index+1);
							Attribute at=new AttributeListImpl(AttributeClass.NODE).createAttribute(property,AttributeType.STRING,label);
							node.getAttributeValues().addValue(at, data.getValue());
							node.getShapeEntity().setNodeShape(NodeShape.DIAMOND).setUri(bindingSet.getValue("node").stringValue());
						}

					}
				}

			});
		}
		//On récupère les edges
		this.addEdges(graph);

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
	 * 
	 */
	private void addEdges(Graph graph) {

		log.debug("récupération des edges");
		//Attributes
		AttributeList attrList = new AttributeListImpl(AttributeClass.EDGE);	
		attrList.createAttribute("type", AttributeType.STRING, "type");
		graph.getAttributeLists().add(attrList);
		List<BindingSet> listBindingSet=new ArrayList<BindingSet>();

		//Récupération du résultat
		try(RepositoryConnection c = repo.getConnection()) {
			String sparqlRequest=SparqlRequest.LIST_EDGES;
			Perform.on(c).select(sparqlRequest, new AbstractTupleQueryResultHandler() {
				@Override
				public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
					listBindingSet.add(bindingSet);	
				}
			});
		}
		//Ajout des edges
		for(BindingSet bindingSet : listBindingSet){
			if(bindingSet.getValue("s")!=null){
				String property=bindingSet.getValue("p").stringValue();
				int index=property.contains("#")?property.lastIndexOf("#"):property.lastIndexOf("/");
				String label=property.substring(index+1);
				Edge edge=null;
				
				try {
					edge = graph.getNode(bindingSet.getValue("s").stringValue())
							.connectTo(
									UUID.randomUUID().toString(),
									label,
									graph.getNode(bindingSet.getValue("o").stringValue()
											)
									).setEdgeType(EdgeType.DIRECTED);
					if(args.getWeight()!=null){
						Properties p = new java.util.Properties();
						p.load(new FileInputStream(new File(args.getWeight())));
						String value=p.getProperty(label);
						if(value!=null){
							float weight=Float.parseFloat(value);
							edge.setWeight(weight);
						}
					}
					
				} catch (Exception e) {
					log.debug("warning -> "+e.getMessage());
					continue;
				}

				Attribute at=new AttributeListImpl(AttributeClass.EDGE).createAttribute("type",AttributeType.STRING,"type");
				edge.getAttributeValues().addValue(at, label);
			}
		}
	}

	/**
	 * 
	 * @param uri : Représente l'uri d'un noeud
	 * @return
	 * Une liste des Triplets ayant pour uri l'uri du noeud passé en paramètre
	 */
	private List<NodeAttribute> getTriplets(String uri) {

		String sparqlRequest=SparqlRequest.QUERY_READ_RESOURCE_LITERALS.replaceAll("_node", uri);
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
						System.out.println(bindingSet.getValue("predicat").stringValue());
						list.add(node);			
					}
				}
			});
		}
		return list;
	}
}
