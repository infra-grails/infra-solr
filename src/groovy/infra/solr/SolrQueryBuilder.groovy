package infra.solr

import groovy.transform.CompileStatic
import org.apache.solr.client.solrj.SolrQuery

/**
 * @author alari
 * @since 3/5/13 4:54 PM
 */
@CompileStatic
abstract class SolrQueryBuilder<T extends SolrQueryBuilder<T>> {

    private final static Map<String,String> searchReplace = [
            '\\': '\\\\',
            '+': '\\+',
            '-': '\\-',
            '&': '\\&',
            '|': '\\|',
            '!': '\\!',
            '(': '\\(',
            ')': '\\)',
            '{': '\\{',
            '}': '\\}',
            '[': '\\[',
            ']': '\\]',
            '^': '\\^',
            '~': '\\~',
            '*': '\\*',
            '?': '\\?',
            ':': '\\:',
            '"': '\\"',
            ';': '\\;',
            ' ': '\\ '
    ]

    private long offset
    private long limit
    private String searchField = "*"
    private String searchQuery = "*"
    private String sortBy
    private SolrQuery.ORDER sortOrder = SolrQuery.ORDER.desc

    private Map<String,List<Collection<String>>> inVariants = [:]

    private Map<String,String> filters = [:]

    /**
     * Descending sort by field
     * @param field
     * @return
     */
    protected T sortDesc(String field) {
        sortBy = field
        sortOrder = SolrQuery.ORDER.desc
        (T)this
    }

    /**
     * Ascending sort by field
     * @param field
     * @return
     */
    protected T sortAsc(String field) {
        sortBy = field
        sortOrder = SolrQuery.ORDER.asc
        (T)this
    }

    /**
     * $field:variants AND $field:variants
     * @param field
     * @param variants
     * @return
     */
    protected T filterInAnd(String field, Collection variants) {
        if (variants && variants.size()) {
            if (!inVariants.containsKey(field)) {
                inVariants.put(field, [])
            }
            inVariants.get(field).push variants
        }
        (T)this
    }

    protected T filter(String field, String value) {
        filters.put(field, value)
        (T)this
    }

    protected T filter(String field, boolean value) {
        filters.put(field, value ? 'true' : 'false')
        (T)this
    }

    protected T filter(String field, Collection values) {
        filters.put(field, "("+values.join(" ")+")")
        (T)this
    }

    protected T filterOr(String field, Collection values) {
        filters.put(field, "("+values.join(" OR ")+")")
        (T)this
    }

    protected T filterRange(String field, from, to) {
        filters.put(field, "[${from} TO ${to}]")
        (T)this
    }

    T limit(long b) {
        limit = b
        (T)this
    }

    T offset(long b) {
        offset = b
        (T)this
    }

    T search(String query) {
        search("text", query)
    }

    T search(String field, String query) {
        searchField = field
        for (entry in searchReplace.entrySet()) {
            query.replace(entry.key, entry.value)
        }
        searchQuery = '"'.concat(query).concat('"')

        (T)this
    }

    SolrQuery build() {
        SolrQuery query = new SolrQuery("${searchField}:${searchQuery}")
        if (sortBy) query.addSortField(sortBy, sortOrder)

        if (offset) query.setParam("start", offset.toString())
        if (limit) query.setParam("rows", limit.toString())

        for(Map.Entry<String,String> f in filters.entrySet()) {
            query.addFilterQuery(f.key + ":" + f.value)
        }

        for(Map.Entry<String,List<Collection<String>>> v in inVariants.entrySet()) {
            if(!v.value?.size()) continue;
            StringBuffer variantsQuery = new StringBuffer()

            Collection variants
            for(int i=0; i<v.value.size(); i++){
                variants=v.value[i]
                variantsQuery.append v.key
                variantsQuery.append ":"
                if (i > 0) {
                    variantsQuery.append " AND "
                }
                variantsQuery.append "("
                variantsQuery.append variants.join(' ')
                variantsQuery.append ")"
            }
            query.addFilterQuery(variantsQuery.toString())
        }
        query
    }

    SolrQuery build(Closure c) {
        c.delegate = this
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        build()
    }
}
