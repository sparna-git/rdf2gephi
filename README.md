# RDF-to-Gephi

Converts RDF knowledge graphs to a [Gephi](https://gephi.org/) GEXF file that can be opened in Gephi. GEXF stands for [Graph Exchange XML Format](https://gexf.net/).
Supports single RDF file, multiple files in a folder, or remote SPARQL endpoint URL. Can work either in a _"direct and simple conversion"_ mode, turning triples into edges, or using a set of SPARQL queries to define exactly the scope and structure of the nodes and edges that should appear in the Gexf file.

Supports attributes on nodes and edges, and supports dynamic graphs generations, with a start and end date on each node and edge.

## How to run

1. Make sure you have Java installed
2. Download the application from the [release section](https://github.com/sparna-git/rdf2gexf/releases)
3. Have some RDF data at hand (one or more RDF file, or a SPARQL service you can query)
4. Open a command-line in the directory you downloaded the app, and run `java -jar rdf2gexf-x.y-onejar.jar --help` to list the available commands and options
5. Run a conversion command, typically the following:

```sh
java -jar rdf2gephi-1.0-onejar.jar sparql \
--input http://my.sparql.endpoint \
--edges queries/edges.rq \
--attributes queries/attributes.rq \
--labels queries/labels.rq \
--output output.gexf
```

6. Download and run [Gephi](https://gephi.org/)
7. In Gephi, open the generated gexf file, and start applying layouts and colors to your graph to make it beautiful and tell your story


## Available commands and options

### direct convertion (discouraged)

Converts RDF data to GEXF format directly. All literals are  considered as attributes, and all triples as edges, except `rdf:type`. `rdfs:label` is used as label.

The full options of the command are:

```
    direct      Converts RDF data to GEXF format directly. All literals are 
            considered as attributes, and all triples as edges, except 
            rdf:type. rdfsl:label is used as label.
      Usage: direct [options]
        Options:
          -e, --endDateProperty
            URI of the property in the knowledge graph holding the end date of 
            entities 
        * -i, --input
            Path to RDF input file, or directory containing RDF files, or URL 
            of a SPARQL endpoint.
        * -o, --output
            Path to GEXF output file
          -s, --startDateProperty
            URI of the property in the knowledge graph holding the start date 
            of entities
          -w, --weight
            Path to a properties file associating properties to weights

```

### SPARQL-based conversion (preferred)

The `sparql` commands takes a set of SPARQL queries to build the structure of the Gephi graph. The command synopsis is the following:

```
java -jar rdf2gephi-1.0-onejar.jar sparql \
--input <file or directory or url of SPARQL endpoint> \
--edges <SPARQL query file to create edges> \
--attributes <SPARQL query file to create attributes> \
--labels <SPARQL query file to create label> \
--output <output gexf file>
```

The full options of the command are:

```
    sparql      Converts RDF data to GEXF format using SPARQL queries.
      Usage: sparql [options]
        Options:
          -a, --attributes
            Path to the file containing the SPARQL query to retrieve 
            attributes, e.g. 'sparql/attribute.rq'. The query MUST return 3 
            columns: the first one is the subject, the second one is the 
            attribute URI, the third one is the attribute value (a literal or 
            a URI).
          -d, --dates
            Path to the file containing the SPARQL query to retrieve date 
            ranges, e.g. 'sparql/dates.rq'
          -e, --edges
            Path to the file containing the SPARQL query to retrieve edges, 
            e.g. 'sparql/edges.rq'. The query MUST return the following 
            variables: ?subject, ?edge, ?object
        * -i, --input
            Path to RDF input file(s), or directory containing RDF files, or 
            URL of a SPARQL endpoint.
          -l, --labels
            Path to the file containing the SPARQL query to retrieve labels, 
            e.g. 'sparql/labels.rq'. The query MUST return the following 
            variables: ?subject, ?label
        * -o, --output
            Path to GEXF output file
          -p, --parents
            Deprecated. Path to the file containing the SPARQL query to 
            retrieve parents relationship, e.g. 'sparql/parents.rq'
```

All queries are optional, and default queries are used if not provided. See below.

*/!\ Attention :* the provided queries MUST follow the following rules

#### --edges / -e query

This query defines the graph structure. 
The edges query MUST return the 3 variables: `?subject`, `?edge`, `?object`. _Optionaly_, the query CAN also result the variables `?start` and `?end` which will be interpreted as the dates of the edge in the gexf graph.

An example of such query is:

```sparql
PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?subject ?edge ?object
WHERE {
    ?subject a cdm:resource_legal .
    ?subject cdm:resource_legal_in-force true .
    ?subject cdm:resource_legal_based_on_resource_legal ?object .
    BIND(cdm:resource_legal_based_on_resource_legal as ?edge)
}
```

If not provided, the following query is used:

```sparql
# Default edges query
# Selects all the triples in the graph
SELECT ?subject ?edge ?object
WHERE {
	?subject ?edge ?object .
}
```

#### --labels / -l query

This query returns the labels of each node in the graph. Typically from an `rdfs:label`, `skos:prefLabel`, or anything.
The labels query MUST use the `?subject` variable to hold the node in the graph, and MUST return the 2 variables `?subject` and `?label`.

An example of such query is:

```sparql
PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?subject ?label
WHERE {
    ?subject cdm:resource_legal_eli ?eli .
    BIND(STRAFTER(STR(?eli), "http://data.europa.eu/") AS ?label)
}
```

This query is optional. If not provided, the following query is used:

```sparql
# Default labels query
# Selects the first present : foaf:name, rdfs:label in english or without language, skos:prefLabel in english or without language
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX org: <http://www.w3.org/ns/org#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX epvoc: <https://data.europarl.europa.eu/def/epvoc#>
SELECT ?subject ?label
WHERE {
	OPTIONAL { ?subject foaf:name ?foafName }
	OPTIONAL { ?subject skos:prefLabel ?prefLabel . FILTER(lang(?prefLabel) = "en" || lang(?prefLabel) = "") }
	OPTIONAL { ?subject rdfs:label ?rdfsLabel . FILTER(lang(?rdfsLabel) = "en" || lang(?rdfsLabel) = "") }
	
	BIND(COALESCE(?foafName, ?prefLabel, ?rdfsLabel) AS ?label)
}
```

#### --attributes / -a query

This query returns the attributes of each node in the graph. Typically the value of `rdf:type`, and other attributes.
The attributes query MUST use the `?subject` variable to hold the node in the graph, and MUST return 3 variables : `?subject`, `?attribute` as the attribute type, and `?value` as the attribute value (a URI or a literal).

An example of such query is:

```sparql
PREFIX cdm: <http://publications.europa.eu/ontology/cdm#>

SELECT ?subject ?attribute ?value
WHERE {
    ?subject cdm:work_has_resource-type ?value .
	BIND(cdm:work_has_resource-type AS ?attribute)
}
```

This query is optional. If not provided, the following query is used:

```sparql
# Default attributes query
# Selects the rdf:type value and any other property pointing to a skos:Concept
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
SELECT ?subject ?attribute ?value
WHERE {
	# The rdf:type is always an attribute
	{ 
		?subject a ?value .
		BIND(rdf:type AS ?attribute)
	}
	# Everything that is a skos:Concept is an attribute by default
	UNION
	{
		?subject ?attribute ?concept .
		?concept a skos:Concept .
	}
}	
```


#### --dates /-d query

This query returns the start and end date that will be associated to each node in the graph. For edges, the start and end date can be provided in the `--edges` query.
The dates query MUST use the `?subject` variable to hold the node in the graph, and MUST return a `?start` and `?end` variables. Only `?start` or `?end` can be returned, in which case the corresponding node will not have a start or end date associated.

An example of such query is:

```sparql
PREFIX rico: <https://www.ica.org/standards/RiC/ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
SELECT ?subject ?start ?end
WHERE {
    ?subject a ?type .
    OPTIONAL { ?subject rico:beginningDate ?start . }
    OPTIONAL { ?subject rico:endDate ?end . }
}
```

This query is optional. If not provided, no start and end date will be associated to the nodes.

#### --parents /-p query

**This is discouraged** since Gephi does not support hierarchical graphs anymore. But this could be useful for other tools, or with older versions of Gephi.
The parents query MUST use the `?subject` variable to hold the node in the graph, and MUST return a `?parent` variable that will hold the parent of that node in the graph.
This query is optional. If not provided, no default query is used and nodes will not have a parent in the graph.


## Support for dynamic graphs

rdf2gephi supports the creation of dynamic graphs where we can see the evolution of the graph over time. For this:
  1. To associate dates to edges : In the `--edges` query, return a `?start` and `?end` variables
  2. To associate dates to nodes : provide a `--dates` query

## Typical actions in Gephi to view your RDF graph

1. Apply a layout algorithm : Use "Force Atlas 2".
2. Give colors to nodes based on the type attribute : Appearance > Nodes > Partition > Choose an attribute
3. Size the nodes based on (incoming or outgoing) degree : Appearance > Size icon > Ranking > Degree
4. Print labels only of biggest nodes : Filter > Topology > Degree Range > drag and drop to Queries below > set the parameters. Then click on filter. Then click on icon above "hide node/edges labels if not in filtered graph" 
5. Click on "Show node labels" button
6. You could also apply a clustering algorithm : go to Statistics > Community detection > Modularity. Then apply node color following the modularity attribute.
7. Go in "Preview" tab, regenerate the preview, export as SVG/PNG/PDF

This is illustrated in the screencast below:

[![](docs/gallery/youtube-video.png)](https://youtu.be/A_YbxNapaBY)

## Gallery

EU in-force legislation from Cellar SPARQL endpoint. Links shows the "based_on" links (acts legally based on another act). ELI identifiers of acts that are most often used as basis are shown

![](docs/gallery/cellar.png)
