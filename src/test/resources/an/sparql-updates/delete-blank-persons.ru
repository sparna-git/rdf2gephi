PREFIX rico: <https://www.ica.org/standards/RiC/ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
DELETE {
	?x rico:hasOrHadSubject ?p .
	?p a rico:Agent .
	?p a rico:Person .
	?p rdfs:label ?label .
	?p rico:hasOrHadAgentName ?name .
	?name a rico:AgentName .
	?name rdfs:label ?nameLabel .
	?name rico:textualValue ?textualValue .	
}
INSERT {
	?x rico:hasOrHadSubject ?label .
}
WHERE {
	?x rico:hasOrHadSubject ?p .
	FILTER(isBlank(?p))
	?p a rico:Agent .
	?p a rico:Person .
	?p rdfs:label ?label .
	?p rico:hasOrHadAgentName ?name .
	?name a rico:AgentName .
	?name rdfs:label ?nameLabel .
	?name rico:textualValue ?textualValue .
}