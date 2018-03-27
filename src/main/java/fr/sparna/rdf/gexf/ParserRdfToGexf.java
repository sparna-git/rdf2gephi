package fr.sparna.rdf.gexf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.GexfImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.StaxGraphWriter;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.data.AttributeListImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.viz.NodeShape;

public class ParserRdfToGexf implements RdfToGexfIfc{

	private static Logger log = LoggerFactory.getLogger(ParserRdfToGexf.class.getName());

	private Repository repo;


	@Override
	public void rdfToGexf(Object o) throws FileNotFoundException, IOException {

		//chargement du fichier rdf et stockage dans le repository

		GexfCli args=(GexfCli)o;
		RepositoryParser repositoryClass=new RepositoryParser();
		repositoryClass.init();
		repositoryClass.storeRepository(
				new FileInputStream(args.getInput()), 
				Rio.getParserFormatForFileName(args.getInput())
				.orElse(RDFFormat.RDFXML)
				);
		this.repo=repositoryClass.getRepository();

		// création du graphe
		Gexf gexf = new GexfImpl();
		Calendar date = Calendar.getInstance();

		gexf.getMetadata()
		.setLastModified(date.getTime())
		.setCreator("SPARNA")
		.setDescription("rdf to gexf");
		gexf.setVisualization(true);
		Graph graph = gexf.getGraph();
		graph.setDefaultEdgeType(EdgeType.DIRECTED).setMode(Mode.DYNAMIC);

		//Liste des attributs
		AttributeList attrList = new AttributeListImpl(AttributeClass.NODE);	

		for (String property  : getAllLiteralProperties()) {
			System.out.println("Adding property "+property);
			int index=property.contains("#")?property.lastIndexOf("#"):property.lastIndexOf("/");
			String label=property.substring(index+1);
			attrList.createAttribute(property, AttributeType.STRING, label);
		}
		//Ajout de la liste des attributs au graphe
		graph.getAttributeLists().add(attrList);

		//requête sparql ramenant tous les noeuds et leur libellés
		String sparqlRequest=SparqlRequest.allNodeUriAndLabel();

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
						List<NodeData> datas=getTriplets(bindingSet.getValue("node").stringValue());
						for (NodeData data : datas) {
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
		File f = new File(args.getOutput()+"/graph.gexf");
		Writer out;
		try {
			out =  new FileWriter(f, false);
			graphWriter.writeToStream(gexf, out, "UTF-8");
			System.out.println(f.getAbsolutePath());
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
			String sparqlRequest=SparqlRequest.literalProperties();
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
		try(RepositoryConnection c = repo.getConnection()) {
			String sparqlRequest=SparqlRequest.allEdges();
			Perform.on(c).select(sparqlRequest, new AbstractTupleQueryResultHandler() {
				@Override
				public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
					log.debug(bindingSet.getValue("s")+" / "+bindingSet.getValue("p")+" / "+bindingSet.getBinding("o"));
					if(bindingSet.getValue("s")!=null){
						Edge edge = graph.getNode(bindingSet.getValue("s").stringValue()).connectTo(graph.getNode(bindingSet.getValue("o").stringValue()));
						edge.setEdgeType(EdgeType.DIRECTED);
						String property=bindingSet.getValue("p").stringValue();
						int index=property.contains("#")?property.lastIndexOf("#"):property.lastIndexOf("/");
						String label=property.substring(index+1);
						Attribute at=new AttributeListImpl(AttributeClass.EDGE).createAttribute("type",AttributeType.STRING,"type");
						edge.getAttributeValues().addValue(at, label);
					}

				}
			});

		}	

	}

	/**
	 * 
	 * @param uri : Représente l'uri d'un noeud
	 * @return
	 * Une liste des Triplets ayant pour uri l'uri du noeud passé en paramètre
	 */
	private List<NodeData> getTriplets(String uri) {

		log.debug("récupération des triplets d'un uri");
		String sparqlRequest=SparqlRequest.tripletsFromNode(uri);
		List<NodeData> list=new ArrayList<NodeData>();
		try(RepositoryConnection c = repo.getConnection()) {

			Perform.on(c).select(sparqlRequest, new AbstractTupleQueryResultHandler() {
				@Override
				public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {					
					if(bindingSet.getValue("node")!=null){
						NodeData node=new NodeData();
						node.setSubject(bindingSet.getValue("node").stringValue());
						node.setPredicat(bindingSet.getValue("predicat").stringValue());
						node.setValue(bindingSet.getValue("object").stringValue());
						list.add(node);			
					}
				}
			});
		}
		return list;
	}
}
