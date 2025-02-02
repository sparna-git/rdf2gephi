java -jar target/rdf2gephi-1.0-beta1-onejar.jar sparql \
--input http://graphdb.sparna.fr/repositories/rdf2gephi \
--edges src/test/resources/an/script1/edges.rq \
--attributes src/test/resources/an/script1/attributes.rq \
--labels src/test/resources/an/script1/labels.rq \
--dates src/test/resources/an/script1/dates.rq \
--parents src/test/resources/an/script1/parents.rq \
--output an-simple.gexf