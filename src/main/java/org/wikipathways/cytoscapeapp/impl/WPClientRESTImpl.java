package org.wikipathways.cytoscapeapp.impl;

import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.IOException;
import java.net.URI;
import java.net.ProxySelector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.HttpEntity;




import org.cytoscape.work.TaskMonitor;
import org.cytoscape.application.CyApplicationConfiguration;
import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;
import org.wikipathways.cytoscapeapp.ResultTask;

public class WPClientRESTImpl implements WPClient {
  protected static final String BASE_URL = "http://webservice.wikipathways.org/";

  final CyApplicationConfiguration appConf;

  final DocumentBuilder xmlParser;
  final CloseableHttpClient client;

  protected static DocumentBuilder newXmlParser() throws ParserConfigurationException {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    final DocumentBuilder builder = factory.newDocumentBuilder();
    return builder;
  }
  
  public WPClientRESTImpl(final CyApplicationConfiguration appConf) {
    this.appConf = appConf;
    try {
      xmlParser = newXmlParser();
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException("Failed to build XML parser", e);
    }
    client = HttpClientBuilder.create()
      .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
      .build();
    }

  /**
   * A convenience class for issuing cancellable REST calls.
   */
  protected abstract class ReqTask<T> extends ResultTask<T> {
    protected volatile boolean cancelled = false;
    protected volatile HttpRequestBase req = null;
    protected volatile CloseableHttpResponse resp = null;
    protected volatile InputStream stream = null;

    protected Document xmlGet(final String url, final String ... args) throws IOException, SAXException {
      // build the request
      URI uri = null;
      try {
        final URIBuilder uriBuilder = new URIBuilder(url);
        for (int i = 0; i < args.length; i += 2) {
          uriBuilder.addParameter(args[i], args[i+1]);
        }
        uri = uriBuilder.build();
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid URL request", e);
      }
      System.out.println("URI: " + uri);
      req = new HttpGet(uri);

      // issue the request
      try {
        resp = client.execute(req);
        final HttpEntity entity = resp.getEntity();
        final String encoding = entity.getContentEncoding() != null ? entity.getContentEncoding().getValue() : null;
        stream = entity.getContent();
        final InputSource inputSource = new InputSource(stream);
        if (encoding != null) {
          inputSource.setEncoding(encoding);
        }
        return xmlParser.parse(inputSource);
      } catch (IOException e) {
        if (!cancelled) {
          throw e; // ignore exceptions thrown during cancellation
        }
      } finally {
        req.releaseConnection();
        resp.close();
        req = null;
        resp = null;
        stream = null;
      }  
      return null;
    }

    public void cancel() {
      cancelled = true; // this must be set before calling abort() or close()
      final HttpRequestBase req2 = req; // copy the ref to req so that it doesn't become null when trying to abort it
      if (req2 != null) {
        req2.abort();
      }
      final InputStream stream2 = stream;
      if (stream2 != null) {
        try {
          stream2.close();
        } catch (IOException e) {
          // don't bother with the exception when trying to close the stream
        }
      }
    }
  }

  private File getSpeciesCacheFile() {
   final File confDir = appConf.getAppConfigurationDirectoryLocation(this.getClass());
   if (!confDir.exists()) {
     if (!confDir.mkdirs()) {
       return null;
     }
   }
   return new File(confDir, "species-cache");
 }

  private void storeSpeciesToCache(final List<String> species) {
    final File speciesCacheFile = getSpeciesCacheFile();
    if (speciesCacheFile == null) {
      return;
    }
    try {
      final FileOutputStream outStream = new FileOutputStream(speciesCacheFile);
      final ObjectOutputStream output = new ObjectOutputStream(outStream);
      output.writeObject(species);
      outStream.close();
    } catch (Exception e) {
      System.out.println("Failed to write species cache");
      e.printStackTrace();
    }
  }

  private List<String> retrieveSpeciesFromCache() {
    final File speciesCacheFile = getSpeciesCacheFile();
    if (speciesCacheFile == null || !speciesCacheFile.exists()) {
      return null;
    }

    try {
      final FileInputStream inStream = new FileInputStream(speciesCacheFile);
      final ObjectInputStream input = new ObjectInputStream(inStream);
      final Object object = input.readObject();
      inStream.close();
      final List<String> result = (List<String>) object;
      return result;
    } catch (Exception e) {
      System.out.println("Failed to read species cache");
      e.printStackTrace();
    }
    return null;
  }

  List<String> species = null;

  public ResultTask<List<String>> newSpeciesTask() {
    return new ReqTask<List<String>>() {
      protected List<String> checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Retrieve list of organisms from WikiPathways");

        if (species == null) {
          species = retrieveSpeciesFromCache();
        }

        if (species != null) {
          return species;
        }

        final Document doc = xmlGet(BASE_URL + "listOrganisms");
        if (super.cancelled)
          return null;
        final Node responseNode = doc.getFirstChild();
        final NodeList organismNodes = responseNode.getChildNodes(); 
        final List<String> species = new ArrayList<String>();
        for (int i = 0; i < organismNodes.getLength(); i++) {
          final Node organismNode = organismNodes.item(i);
          species.add(organismNode.getTextContent());
        }

        storeSpeciesToCache(species);

        return species;
      }
    };
  }

  private static WPPathway parsePathwayInfo(final Node node) {
    final NodeList argNodes = node.getChildNodes();
    String id = "", revision = "", name = "", species = "", url = "";
    for (int j = 0; j < argNodes.getLength(); j++) {
      final Node argNode = argNodes.item(j);
      final String argName = argNode.getNodeName();
      final String argVal = argNode.getTextContent();
      if (argName.equals("ns2:id")) {
        id = argVal;
      } else if (argName.equals("ns2:revision")) {
        revision = argVal;
      } else if (argName.equals("ns2:name")) {
        name = argVal;
      } else if (argName.equals("ns2:species")) {
        species = argVal;
      } else if (argName.equals("ns2:url")) {
        url = argVal;
      }
    }
    if ("".equals(name)) {
      return null;
    } else {
      return new WPPathway(id, revision, name, species, url);
    }
  }

  public ResultTask<List<WPPathway>> newFreeTextSearchTask(final String query, final String species) {
    return new ReqTask<List<WPPathway>>() {
      protected List<WPPathway> checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Search WikiPathways for \'" + query + "\'");
        final Document doc = xmlGet(BASE_URL + "findPathwaysByText", "query", query, "species", species == null ? "" : species);
        if (super.cancelled)
          return null;
        final Node responseNode = doc.getFirstChild();
        final NodeList resultNodes = responseNode.getChildNodes(); 
        final List<WPPathway> result = new ArrayList<WPPathway>();
        for (int i = 0; i < resultNodes.getLength(); i++) {
          String id = "", revision = "", name = "", species = "", url = "";
          final Node resultNode = resultNodes.item(i);
          final WPPathway pathway = parsePathwayInfo(resultNode);
          if (pathway != null) {
            result.add(pathway);
          }
        }
        return result;
      }
    };
  }

  public ResultTask<WPPathway> newPathwayInfoTask(final String id) {
    return new ReqTask<WPPathway>() {
      protected WPPathway checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Retrieve info for \'" + id + "\'");
        final Document doc = xmlGet(BASE_URL + "getPathwayInfo", "pwId", id);
        if (super.cancelled)
          return null;
        final Node responseNode = doc.getFirstChild();
        final Node resultNode = getFirstNonWhitespaceChild(responseNode);
        return parsePathwayInfo(resultNode);
      }

    };
  }

  private Node getFirstNonWhitespaceChild(Node node) {
    final NodeList resultNodes = node.getChildNodes();
    int childCounter = 1;
    Node resultNode = resultNodes.item(childCounter);
    while (resultNode.getNodeType() == Node.TEXT_NODE) {
      childCounter++;
      resultNode = resultNodes.item(childCounter);
    }
    return resultNode;
  }

  protected static Node findChildNode(final Node parentNode, final String nodeName) {
    final NodeList nodes = parentNode.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      final Node node = nodes.item(i);
      if (node.getNodeName().equals(nodeName))
        return node;
    }
    return null;
  }

  public ResultTask<Reader> newGPMLContentsTask(final WPPathway pathway) {
    return new ReqTask<Reader>() {
      protected Reader checkedRun(final TaskMonitor monitor) throws Exception {
        monitor.setTitle("Get \'" + pathway.getName() + "\' from WikiPathways");
        Document doc = null;
        try {
          doc = xmlGet(BASE_URL + "getPathway", "pwId", pathway.getId(), "revision", pathway.getRevision());
        } catch (SAXParseException e) {
          throw new Exception(String.format("'%s' is not available -- invalid GPML", pathway.getName()), e);
        }
        if (super.cancelled)
          return null;
        final Node responseNode = doc.getFirstChild();
        final Node pathwayNode = findChildNode(responseNode, "ns1:pathway");
        final Node gpmlNode = findChildNode(pathwayNode, "ns2:gpml");
        byte[] decoded = Base64.decode(gpmlNode.getTextContent().trim());
        return new InputStreamReader(new ByteArrayInputStream(decoded));
      }
    };
  }
}
