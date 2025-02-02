PREFIX rico: <https://www.ica.org/standards/RiC/ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
DELETE {
	?x rico:hasOrHadAgentName ?name .
	?name ?p ?o .
}
INSERT {
	?x skos:prefLabel ?textualValue .
}
WHERE {
	?x a rico:Agent .
	?x rico:hasOrHadAgentName ?name .
	?name a rico:AgentName .
	?name rdfs:label ?nameLabel .
	?name rico:textualValue ?textualValue .
	?name rico:isOrWasAgentNameOf ?x .
	?name rico:type "nom d'agent : forme préférée"@fr .
	?name ?p ?o .
}