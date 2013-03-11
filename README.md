infra-solr
==========

Grails plugin with Solr resources and query builder helper class.

Usage
-----------

1. Install the plugin

`compile ':infra-solr:0.1-SNAPSHOT'`

2. Install Solr templates 

`grails install-solr-templates`

Templates will be placed to `grails-app/conf/infra/solr`. Don't move them anywhere.

3. Edit templates:
- Change index in `core/conf/schema.xml`
- Add more cores to `solr.xml`. Copy `core` folder for each.
- And so on.

4. Use helper classes to build your own cool Solr binding. Take a look on `SolrService` and `SolrQueryBuilder`,
make service and subclass Builder for every core you use.

5. When you're going on production, remember to set up http server conf. Example is in plugin's `grails-app/conf/Config.groovy` source.

That's all. Plugin is about simplifying the basics of Solr while you're making all the creativity yourself, not about autosolving all your problems :)
