package infra.solr

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.core.CoreContainer
import org.apache.solr.parser.ParseException
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.io.ClassPathResource

class SolrService implements ApplicationContextAware {

    static transactional = false

    def grailsApplication

    private SolrServer server
    int COMMIT_WITHIN_MS = 2000

    SolrServer getServer() {
        server
    }

    List queryIds(SolrQueryBuilder builder) {
        queryIds(builder.build())
    }

    List queryIds(SolrQuery query) {
        QueryResponse response
        try {
            response = server.query(query)
        } catch (ParseException e) {
            log.error("Cannot parse query: ${query}", e)
            return []
        }

        List<Long> ids = []
        if (response.results.numFound) {
            for (SolrDocument d : response.results) {
                ids << Long.parseLong((String) d.getFieldValue("id"))
            }
        }

        ids
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        synchronized (this) {
            if (config.host) {
                initHttpSolr()
            } else {
                initEmbeddedSolr()
            }
        }
        if (config.commitWithinMs) {
            COMMIT_WITHIN_MS = config.commitWithinMs
        }
    }

    private Map getConfig() {
        ((ConfigObject)grailsApplication.config.plugin.infraSolr).flatten()
    }

    private initHttpSolr() {
        log.info "Running Http Solr Server..."

        server = new HttpSolrServer("http://${config.host}")

        config.keySet().each { String k ->
            String setter = "set"+k[0].toUpperCase()+k.substring(1)
            if(server.metaClass.methods.any {it.name == setter}) {
                server[k] = config[k]
            }
        }

        log.info "Http Solr Server is running."
    }

    private initEmbeddedSolr() {
        String solrHome = new ClassPathResource(".", this.class).file.absolutePath
        String solrConfig = solrHome.concat("/solr.xml")

        CoreContainer container = new CoreContainer(solrHome, new File(solrConfig));

        log.info "Running Embedded Solr Server..."

        server = new EmbeddedSolrServer(container, "")

        println "Embedded Solr Server is running."
    }
}
