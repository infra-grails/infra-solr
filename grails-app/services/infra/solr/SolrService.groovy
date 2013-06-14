package infra.solr

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrException
import org.apache.solr.core.CoreContainer
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.io.ClassPathResource

class SolrService implements ApplicationContextAware {

    static transactional = false

    def grailsApplication

    private SolrServer mainServer
    private Map<String,SolrServer> coreServers = [:]
    int COMMIT_WITHIN_MS = 2000

    SolrServer getMainServer() {
        mainServer
    }

    SolrServer getCoreServer(String core) {
        if (!core) {
            mainServer
        } else {
            coreServers.get(core, runServer(core))
        }
    }

    List<Long> queryIds(SolrQueryBuilder builder) {
        QueryResponse response = queryResponse(builder)

        List<Long> ids = []
        if (response.results.numFound) {
            for (SolrDocument d : response.results) {
                ids << Long.parseLong((String) d.getFieldValue("id"))
            }
        }
        ids
    }

    QueryResponse queryResponse(SolrQueryBuilder builder) {
        SolrQuery query = builder.build()
        try {
            return getCoreServer(builder.core).query(query)
        } catch (SolrException e) {
            log.error("Cannot parse query: ${query}", e)
            return null
        }
    }

    void delete(id, String core=null) {
        getCoreServer(core).deleteById(id.toString(), COMMIT_WITHIN_MS)
    }

    void indexBean(bean, String core=null) {
        getCoreServer(core).addBean(bean, COMMIT_WITHIN_MS)
    }

    void deleteAll(String core=null) {
        getCoreServer(core).deleteByQuery("*:*", COMMIT_WITHIN_MS)
    }

    long countAll(String core=null) {
        SolrQuery q = new SolrQuery("*:*")
        q.setParam("rows", "0")
        getCoreServer(core).query(q).results.numFound
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        synchronized (this) {
            mainServer = runServer("")
            if (config.cores instanceof List) {
                List<String> cores = config.cores
                if (cores.size()) {
                    cores.each {
                        coreServers.put(it, runServer(it))
                    }
                }
            }
        }
        if (config.commitWithinMs) {
            COMMIT_WITHIN_MS = config.commitWithinMs
        }
    }

    private Map getConfig() {
        ((ConfigObject)grailsApplication.config.plugin.infraSolr).flatten()
    }

    private SolrServer runServer(String core) {
        config.host ? runHttpServer(core) : runEmbeddedServer(core)
    }

    private SolrServer runHttpServer(String core) {
        log.info "Running Http Solr Server [${core?:'(main)'}]..."

        SolrServer s = new HttpSolrServer("http://${config.host}"+(core?"/${core}":""))

        config.keySet().each { String k ->
            String setter = "set"+k[0].toUpperCase()+k.substring(1)
            if(s.metaClass.methods.any {it.name == setter}) {
                s[k] = config[k]
            }
        }

        log.info "Http Solr Server [${core?:'(main)'}] is running."

        s
    }

    private SolrServer runEmbeddedServer(String core) {
        String solrHome = new ClassPathResource(".", this.class).file.absolutePath
        String solrConfig = solrHome.concat("/solr.xml")

        CoreContainer container = new CoreContainer(solrHome, new File(solrConfig));

        log.info "Running Embedded Solr Server [${core?:'(main)'}]..."

        SolrServer s = new EmbeddedSolrServer(container, core)

        println "Embedded Solr Server [${core?:'(main)'}] is running."
        s
    }
}
