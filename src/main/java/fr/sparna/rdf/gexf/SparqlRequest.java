package fr.sparna.rdf.gexf;

public class SparqlRequest {

	public final static String  QUERY_LIST_NODES=""
			+ "PREFIX dcterms: <http://purl.org/dc/terms/> "+
			" PREFIX edm: <http://www.europeana.eu/schemas/edm/>"
			+ "PREFIX legilux: <http://data.legilux.public.lu/resource/ontology/jolux#> "
			+"	PREFIX org: <http://www.w3.org/ns/org#> "+
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
			"PREFIX dc: <http://purl.org/dc/elements/1.1/> "+
			"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
			+ "select ?node ?label ?start ?end "
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
			+ " OPTIONAL {?node rdfs:label ?rdfsLabel.} "
			+ " BIND(IF(bound(?rdfsLabel),?rdfsLabel,STR(?node)) AS ?label)"
			+ "_OPTIONSTART_"
			+ "_OPTIONEND_"
			+ 
			"}";
	
	public final static String  LIST_LITERAL_PROPERTIES=""
			+ "PREFIX dcterms: <http://purl.org/dc/terms/> "
			+ "PREFIX legilux: <http://data.legilux.public.lu/resource/ontology/jolux#> "+
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
			+ "select distinct ?p "
			+  "where { "
			+ "{ ?s ?p ?o. " 
			+ "  FILTER("
			+ "		((?p != rdfs:label) && isLiteral(?o))||(?p =rdf:type)"
			+ "	  )"
			+ ""
			+ "	_FILTER_"
			+ "}"
			
			+ 
			"}";
	

	public final static String LIST_EDGES=""
			+ "PREFIX dcterms: <http://purl.org/dc/terms/> "+"\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+"\n"
			+ "select distinct ?s ?p ?o"+"\n"
			+ "where { "+"\n"
			+ " ?s ?p ?o . " +"\n"
			+ " FILTER(?p != rdf:type && isIRI(?o))"+"\n"
			+ "}";
		
	public final static String QUERY_READ_RESOURCE_LITERALS_AND_TYPE=""
			+ "PREFIX dcterms: <http://purl.org/dc/terms/> "
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
			+ "select distinct ?node ?predicat ?object "
			+  "where { "
			+ " ?node ?predicat ?object. "
			+ "VALUES ?node{<_node>}" 
			+"FILTER("
			+ "((?predicat != rdfs:label) && (isLiteral(?object) || (isIRI(?object) && ?predicat=rdf:type)))"
			+ ")"
			+ "_FILTER_"

			+ 
			"}";


}
