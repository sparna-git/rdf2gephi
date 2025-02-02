package fr.sparna.rdf.rdf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Supplier for RepositoryBuilder, from a simple String or a list of files/directory to load.
 * 
 * @author Thomas Francart
 *
 */
public class RepositoryBuilderFactory implements Supplier<RepositoryBuilder> {
	
	public static final String DEFAULT_REPOSITORY_SYSTEM_PROPERTY = "rdf4j.repository";
	
	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	protected List<String> fileOrDirectoryOrURLs = new ArrayList<String>();
	
	protected Supplier<Repository> localRepositorySupplier; 
	
	protected String namedGraphsRootUri = null;
	
	/**
	 * Creates a RepositoryBuilderFactory with a list of Strings that can be file path, directory paths, or URLs, and the
	 * original {@code Supplier<Repository>}.
	 * 
	 * @param fileOrDirectoryOrURLs path to file, directory, or URL
	 * @param localRepositoryFactory supplier of the initial Repository
	 */
	public RepositoryBuilderFactory(List<String> fileOrDirectoryOrURLs, Supplier<Repository> localRepositoryFactory) {
		super();
		this.fileOrDirectoryOrURLs = fileOrDirectoryOrURLs;
		this.localRepositorySupplier = localRepositoryFactory;
	}
	
	public RepositoryBuilderFactory(List<String> fileOrDirectoryOrURLs, Supplier<Repository> localRepositoryFactory, String namedGraphsRootUris) {
		super();
		this.fileOrDirectoryOrURLs = fileOrDirectoryOrURLs;
		this.localRepositorySupplier = localRepositoryFactory;
		this.namedGraphsRootUri = namedGraphsRootUris;
	}
	
	public static RepositoryBuilder fromSystemProperty() {
		return RepositoryBuilderFactory.fromString(DEFAULT_REPOSITORY_SYSTEM_PROPERTY);
	}
	
	/**
	 * Creates a RepositoryBuilder with a single String that can be file path, directory paths, or URLs, and the
	 * original {@code Supplier<Repository>}.
	 * 
	 * @param fileOrDirectoryOrURL path to file, directory, or URL
	 * @param localRepositoryFactory supplier of the initial Repository
	 * 
	 * @return a RepositoryBuilder ready to load provided resource
	 */
	public static RepositoryBuilder fromString(String fileOrDirectoryOrURL, Supplier<Repository> localRepositoryFactory) {
		return new RepositoryBuilderFactory(Collections.singletonList(fileOrDirectoryOrURL), localRepositoryFactory).get();
	}
	
	/**
	 * Creates a RepositoryBuilderFactory with a list of Strings that can be file path, directory paths, or URLs,
	 * and a default LocalMemoryRepositorySupplier.
	 * 
	 * @param fileOrDirectoryOrURLs path to file, directory, or URL
	 * 
	 * @return a RepositoryBuilder ready to load provided resources in a local memory repository
	 */
	public static RepositoryBuilder fromStringList(List<String> fileOrDirectoryOrURLs) {
		return new RepositoryBuilderFactory(fileOrDirectoryOrURLs, new LocalMemoryRepositorySupplier()).get();
	}
	
	public static RepositoryBuilder fromStringList(List<String> fileOrDirectoryOrURLs, String namedGraphsRootUri) {
		return new RepositoryBuilderFactory(fileOrDirectoryOrURLs, new LocalMemoryRepositorySupplier(), namedGraphsRootUri).get();
	}
	
	/**
	 * Creates a RepositoryBuilderFactory with a single String that can be file path, directory paths, or URLs, 
	 * and a default LocalMemoryRepositorySupplier.
	 * 
	 * @param fileOrDirectoryOrURL path to file, directory, or URL
	 */
	public static RepositoryBuilder fromString(String fileOrDirectoryOrURL) {
		return new RepositoryBuilderFactory(Collections.singletonList(fileOrDirectoryOrURL), new LocalMemoryRepositorySupplier()).get();
	}
	
	/**
	 * Creates the RepositoryBuilder. Each String can be :
	 * <ul>
	 *   <li>The name of a System property from which the actual value will be read if it exists</li>
	 *   <li>A URL if it starts with 'http'. In this case if it ends with a known RDF extension it will first be attempted to be loaded as an RDF file, 
	 *   otherwise it will be interpreted as the URL of a SPARQL endpoint;
	 *   </li>
	 *   <li>A URL if it starts with 'jdbc:virtuoso', in this case a VirtuosoReflectionRepositoryFactory will be used as the source {@code Supplier<Repository>}</li>
	 *   <li>The path to a file or directory or classpath resource</li>
	 * </ul>
	 */
	public RepositoryBuilder get() {
		List<Consumer<RepositoryConnection>> operations = new ArrayList<Consumer<RepositoryConnection>>();
		Supplier<Repository> repositorySupplier = localRepositorySupplier;
		
		if(fileOrDirectoryOrURLs != null && fileOrDirectoryOrURLs.size() == 1) {
			String value = fileOrDirectoryOrURLs.get(0);

			// try with a system property
			String pValue = System.getProperty(value);
			if(pValue != null) {
				value = pValue;
			}
			
			// try with a URL
			URL url = null;
			if(value.startsWith("http")) {
				try {
					url = new URL(value);
					log.debug(value+" is a valid URL");
				} catch (MalformedURLException e) {
					log.debug(value+" is not a valid URL. It will be interpreted as a file or directory path.");
				}
			}
			
			
			if(url != null) {
				if(Rio.getParserFormatForFileName(url.toString()).isPresent()) {
					// looks like a file we can parse, let's parse it
					log.debug(value+" can be parsed using an available parser");
					operations.add(new LoadFromUrl(url));
				} else {
					// does not look like a file we can parse, try to ping it to see if it is a endpoint
					log.debug(value+" cannot be parsed using availble parser, will try to ping for a SPARQL endpoint...");
					Repository r = new EndpointRepositorySupplier(value).get();
					try(RepositoryConnection connection = r.getConnection()) {
						if(RepositoryConnections.ping(connection)) {
							log.debug("Ping was successfull, will consider it like a SPARQL endpoint");
							repositorySupplier = new EndpointRepositorySupplier(value, (url.toString().contains("repositories")||url.toString().contains(":7200")||url.toString().contains("rdf4j")));
						} else {
							log.debug("Ping was NOT successfull, will stick to loading a URL");
							operations.add(new LoadFromUrl(url));
						}
					} catch (Exception e) {
						// oups, something bad happened, will stick to a URL
						e.printStackTrace();
						log.debug("Oups, an exception happened ("+e.getMessage()+", see stacktrace),  will stick to loading a URL");
						operations.add(new LoadFromUrl(url));
					}
				}
			} else {
				log.debug(value+" will try to be loaded from a file, directory or classpath resource");
				LoadFromFileOrDirectory lffod = new LoadFromFileOrDirectory(this.fileOrDirectoryOrURLs);
				if(this.namedGraphsRootUri != null) {
					lffod.setAutoNamedGraphs(true);
					lffod.setNamedGraphsRootUri(this.namedGraphsRootUri);
				}
				operations.add(lffod);
			}

		} else {
			// if more than one arg, consider they are necessarily files or directories
			operations.add(new LoadFromFileOrDirectory(this.fileOrDirectoryOrURLs));
		}
		
		RepositoryBuilder repositoryBuilder = new RepositoryBuilder(repositorySupplier, operations);
		return repositoryBuilder;
	}
	
	/**
	 * Test if given URL can be the URL of a SPARQL endpoint, or a URL pointing to a file
	 * @param url
	 * @return true if provided URL does not have a known RDF extension and responds to a ping SPARQL query
	 */
	public static boolean isEndpointURL(String url) {
		// 1. test if a parser is available for that file extension.
		if(Rio.getParserFormatForFileName(url.toString()) != null) {
			return false;
		} 
		
		// 2. if not, try to ping the URL		
		Repository r = new EndpointRepositorySupplier(url).get();
		try(RepositoryConnection connection = r.getConnection()) {
			if(RepositoryConnections.ping(connection)) {
				return true;
			}
		} catch (Exception e) {
			// exception : it is not a endpoint
			return false;
		}

		
		return false;
	}
	
	public void addFileOrDirectoryOrURL(String fileOrDirectoryOrURL) {
		this.fileOrDirectoryOrURLs.add(fileOrDirectoryOrURL);
	}
	
	public List<String> getFileOrDirectoryOrURLs() {
		return fileOrDirectoryOrURLs;
	}

	public void setFileOrDirectoryOrURLs(List<String> fileOrDirectoryOrURLs) {
		this.fileOrDirectoryOrURLs = fileOrDirectoryOrURLs;
	}
	
}
