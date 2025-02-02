PREFIX rico: <https://www.ica.org/standards/RiC/ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
DELETE {
	?s ?p ?o .
}
WHERE {
	?s ?p ?o .
	FILTER(?p IN (rico:isOrWasRegulatedBy, rico:hasCreator, rico:hasOrHadHolder, rico:isOrWasIncludedIn))
}