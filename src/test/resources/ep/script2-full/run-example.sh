java -jar target/rdf2gephi-1.0-beta1-onejar.jar sparql \
--input src/test/resources/ep/data \
--edges src/test/resources/ep/script2-full/edges.rq \
--labels src/test/resources/ep/script2-full/labels.rq \
--attributes src/test/resources/ep/script2-full/attributes.rq \
--dates src/test/resources/ep/script2-full/dates.rq \
--output output_ep-full.gexf