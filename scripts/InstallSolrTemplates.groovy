includeTargets << grailsScript("_GrailsInit")

target(main: "Installs Solr template config to grails-app/conf/infra/solr") {
    ant.copydir(src: "${infraSolrPluginDir}/src/templates/solr-conf", dest:"${basedir}/grails-app/conf/infra/solr")
}

setDefaultTarget(main)
