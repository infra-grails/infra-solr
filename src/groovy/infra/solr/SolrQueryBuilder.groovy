package infra.solr

import org.apache.solr.client.solrj.SolrQuery

/**
 * @author alari
 * @since 3/5/13 4:54 PM
 */
class SolrQueryBuilder {

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

    private Map<String,List<Collection>> inVariants = [:]

    private Map<String,String> filters = [:]

    /**
     * Descending sort by field
     * @param field
     * @return
     */
    protected SolrQueryBuilder sortDesc(String field) {
        sortBy = field
        sortOrder = SolrQuery.ORDER.desc
        this
    }

    /**
     * Ascending sort by field
     * @param field
     * @return
     */
    protected SolrQueryBuilder sortAsc(String field) {
        sortBy = field
        sortOrder = SolrQuery.ORDER.asc
        this
    }

    /**
     * $field:variants AND $field:variants
     * @param field
     * @param variants
     * @return
     */
    protected SolrQueryBuilder filterInAnd(String field, List<Collection> variants) {
        inVariants.put(field, variants)
        this
    }

    /**
     * $field:variants AND $field:variants
     * @param field
     * @param variants
     * @return
     */
    protected SolrQueryBuilder filterInAnd(String field, Collection variants) {
        if (!inVariants.containsKey(field)) {
            inVariants.put(field, [])
        }
        inVariants.get(field).add variants
        this
    }

    protected SolrQueryBuilder filter(String field, String value) {
        filters.put(field, value)
        this
    }

    protected SolrQueryBuilder filter(String field, boolean value) {
        filters.put(field, value ? 'true' : 'false')
        this
    }

    protected SolrQueryBuilder filter(String field, Collection values) {
        filters.put(field, "("+values.join(" ")+")")
        this
    }

    protected SolrQueryBuilder filterOr(String field, Collection values) {
        filters.put(field, "("+values.join(" OR ")+")")
        this
    }

    protected SolrQueryBuilder filterRange(String field, from, to) {
        filters.put(field, "[${from} TO ${to}]")
        this
    }

    SolrQueryBuilder limit(long b) {
        limit = b
        this
    }

    SolrQueryBuilder offset(long b) {
        offset = b
        this
    }

    SolrQueryBuilder search(String query) {
        search("text", query)
    }

    SolrQueryBuilder search(String field, String query) {
        searchField = field
        for (entry in searchReplace.entrySet()) {
            query.replace(entry.key, entry.value)
        }
        searchQuery = '"'.concat(query).concat('"')

        this
    }

    SolrQuery build() {
        SolrQuery query = new SolrQuery("${searchField}:${searchQuery}")
        if (sortBy) query.addSortField(sortBy, sortOrder)

        if (offset) query.setParam("start", offset.toString())
        if (limit) query.setParam("rows", limit.toString())

        for(f in filters) {
            query.addFilterQuery("${f.key}:${f.value}")
        }

        for(v in inVariants.entrySet()) {
            if(!v.value?.size()) continue;
            StringBuffer variantsQuery = new StringBuffer()
            variantsQuery.append v.key
            variantsQuery.append ":"
            for(int i = 0; i<v.value.size(); i++) {
                if (i > 0) {
                    variantsQuery.append " AND "
                    variantsQuery.append v.key
                    variantsQuery.append ":"
                }
                variantsQuery.append "("
                variantsQuery.append v.value.getAt(i).join(' ')
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
