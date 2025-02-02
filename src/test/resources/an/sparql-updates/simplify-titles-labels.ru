PREFIX rico: <https://www.ica.org/standards/RiC/ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
DELETE {
	?x rdfs:label ?title .
}
WHERE {
	?x rico:title ?title .
	?x rdfs:label ?title .
}