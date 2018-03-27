package fr.sparna.rdf.gexf;

public class SparqlRequest {

	public static String allNodeUriAndLabel(){
		String sparqlRequest=""
				+ "PREFIX dcterms: <http://purl.org/dc/terms/> "+
				" PREFIX edm: <http://www.europeana.eu/schemas/edm/>"
				+"	PREFIX org: <http://www.w3.org/ns/org#> "+
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> "+
				"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
				+ "select ?node ?label "
				+  "where {"
				+ "{"
				+ "?node ?p ?o. "
				+"  FILTER(?p != rdf:type && isIRI(?o)) "
				+ " }"
				+ " UNION "

				+ "{ "
				+ "?s ?p ?node. "
				+"  FILTER(?p != rdf:type && isIRI(?node)) "
				+ "}"
				+ " OPTIONAL {?node rdfs:label ?label.} "

				+ 
				"}";
		return sparqlRequest;
	}

	public static String literalProperties(){
		String sparqlRequest=""
				+ "PREFIX dcterms: <http://purl.org/dc/terms/> "+
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "select distinct ?p "
				+  "where { "
				+ " ?s ?p ?o. " 
				+ "  FILTER(?p != rdfs:label &&  isLiteral(?o))"
				+ 
				"}";

		return sparqlRequest;
	}

	public static String allEdges(){
		String sparqlRequest=""
				+ "PREFIX dcterms: <http://purl.org/dc/terms/> "+"\n"+
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+"\n"
				+ "select distinct ?s ?p ?o"+"\n"
				+ "where { "+"\n"
				+ " ?s ?p ?o . " +"\n"
				+"FILTER(?p != rdf:type && isIRI(?o))"+"\n"
				+"}";
		return sparqlRequest;
	}

	public static String tripletsFromNode(String node){
		String sparqlRequest=""
				+ "PREFIX dcterms: <http://purl.org/dc/terms/> "+
				" PREFIX edm: <http://www.europeana.eu/schemas/edm/>"
				+"	PREFIX org: <http://www.w3.org/ns/org#> "+
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> "+
				"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
				+ "select distinct ?node ?predicat ?object "
				+  "where { "
				+ " ?node ?predicat ?object. "
				+ "VALUES ?node{<"+node+">}" 
				+"FILTER(?predicat != rdfs:label && isLiteral(?object))"

				+ 
				"}";
		return sparqlRequest;
	}


}
