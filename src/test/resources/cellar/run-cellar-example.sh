java -jar target/rdf2gexf-0.4-onejar.jar fromsparql \
--input http://publications.europa.eu/webapi/rdf/sparql \
--edges src/test/resources/cellar/edges.rq \
--attributes src/test/resources/cellar/attributes.rq \
--labels src/test/resources/cellar/labels.rq \
--output output_cellar.gexf