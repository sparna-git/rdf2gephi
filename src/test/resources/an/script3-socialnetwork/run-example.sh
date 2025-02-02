java -jar target/rdf2gephi-1.0-beta1-onejar.jar sparql \
--input http://graphdb.sparna.fr/repositories/rdf2gephi \
--edges src/test/resources/an/script3-socialnetwork/1-edges.rq \
--attributes src/test/resources/an/script3-socialnetwork/2-attributes.rq \
--labels src/test/resources/an/script3-socialnetwork/3-labels.rq \
--dates src/test/resources/an/script3-socialnetwork/4-dates.rq \
--output an-socialnetwork.gexf