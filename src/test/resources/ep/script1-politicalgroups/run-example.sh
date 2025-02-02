java -jar target/rdf2gephi-1.0-beta1-onejar.jar sparql \
--input src/test/resources/ep/data \
--edges src/test/resources/ep/script1-politicalgroups/edges.rq \
--labels src/test/resources/ep/script1-politicalgroups/labels.rq \
--attributes src/test/resources/ep/script1-politicalgroups/attributes.rq \
--output output_ep-politicalgroups.gexf