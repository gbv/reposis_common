
# reposis-common

This is a collection of common code used by the reposis projects.

## Installation Instructions

* run `mvn clean install`
* copy jar to ~/.mycore/(dev-)mir/lib/

## Features

### Solr Facet Time Bar

The Solr Facet Time Bar is a component that allows to display a time bar for Solr range facet. It´s intended to be displayed
in the response-mir.xsl.

```xslt
<xsl:variable name="timebarField" select="'fdrwiso.mods.period_of_reference'"/>
<div class="card">
    <div class="card-header" data-toggle="collapse-next">
        <h3 class="card-title">
            <xsl:value-of select="i18n:translate('mir.search_facet.date.period_of_reference')"/>
        </h3>
    </div>
    <div class="card-body collapse show">
        <script src="{$WebApplicationBaseURL}js/timebar.js" type="text/javascript"></script>
        <div data-timebar="true"
             data-timebar-height="100"
             data-search-field="{$timebarField}"
             data-timebar-start="0001-01-01T00:00:00Z"
             data-timebar-end="NOW"
             data-timebar-gap="+1YEAR"
             data-timebar-mincount="1"
        >
        </div>
    </div>
/div>
```

In the example the Variable `timebarField` is used to define the Solr field that should be used for the time bar. The field needs to have the type `date_range`:

```json
[
  {
    "add-field-type": {
      "name": "date_range_fdrwiso",
      "class": "solr.DateRangeField"
    }
  },
  {
    "add-field": {
      "name": "fdrwiso.mods.period_of_reference",
      "type": "date_range_fdrwiso",
      "multiValued": false
    }
  }
]
```

The attributes of the div will be translated to Solr parameters. The following parameters are supported:

| Attribute             | SOLR Parameter or Description  | Default              |
|-----------------------|--------------------------------|----------------------|
| data-timebar-start    | facet.range.start              | 0001-01-01T00:00:00Z |
| data-timebar-end      | facet.range.end                | NOW                  |
| data-timebar-gap      | facet.range.gap                | +1YEAR               |
| data-timebar-mincount | facet.mincount                 | 1                    |
| ata-timebar-height    | the height in PX of the Canvas | 100                  |

### GeoSearch

The GeoSearch is a component that show a map with all coordinates of the documents in this repository. 
The user can then search for documents in a specific area, with different shapes (circle, polygon).

| Property                              | Description                                                            |
|---------------------------------------|------------------------------------------------------------------------|
| MCR.GeoSearch.Solr.Map.CenterX        | The center of the map in X direction (longitude) for the initial view  |
| MCR.GeoSearch.Solr.Map.CenterY        | The center of the map in Y direction (latitude) for the initial view   |
| MCR.GeoSearch.Solr.Map.Zoom           | The zoom level for the initial view                                    |
| MCR.GeoSearch.Solr.Public.SearchURI   | The URI for the public search                                          |
| MCR.GeoSearch.Solr.Internal.SearchURI | The URI for the internal search                                        |
| MCR.GeoSearch.Solr.InternalRoles      | The roles that are allowed to see the internal search                  |
| MCR.GeoSearch.Solr.WKT.Field          | The field that will be used for the WKT search                         |

The properties contain sane defaults, so you only need to change them if you want to change the default behavior.

To enable the GeoSearch you need to add the link to the navigation.xml:
    
```xml
<item href="/vue/geo-search/" type="intern" replaceMenu="false" constrainPopUp="false">
    <label xml:lang="de">Geografischer Sucheinstieg</label>
    <label xml:lang="en">Geographic search entry</label>
</item>
```

### Properties GUI

The Properties GUI is a component that displays the properties of a mycore application grouped by the module they belong to. 
It shows the order of the properties and the history in which module they were already defined and what the value was.
It supports the chaining mechanism of mycore properties, but not the replacement mechanism.

The properties GUI is visible under: http://localhost:8291/mir/servlets/PropertyHelperContentServlet?action=analyze

You need Admin rights to see the GUI. It shows the properties defined in every component.

### Metrics
The metrics implementation allows to load journal metrics from different providers. The metrics are stored  encrypted in the mods:extension section of the journal object.

To enable the implementation you have to set the following properties in mycore.properties:  

```properties
MCR.EventHandler.MCRObject.010.Class=de.gbv.reposis.metrics.MCRUpdateJournalMetricsEventHandler


MCR.Crypt.Cipher.jcr.class=org.mycore.crypt.MCRAESCipher
MCR.Crypt.Cipher.jcr.KeyFile=%MCR.datadir%/cipher/keyjcr.secret

MCR.Crypt.Cipher.jcr_intern.class=org.mycore.crypt.MCRAESCipher
MCR.Crypt.Cipher.jcr_intern.KeyFile=%MCR.datadir%/cipher/keyjcr.secret
MCR.Crypt.Cipher.jcr_intern.EnableACL=false
```

To generate the keys you have to run the following command:

```bash
generate keyfile for cipher jcr
```

#### Scopus
To use the Scopus metrics provider you have to set the following properties in mycore.properties:

```properties
MCR.MODS.Metrics.Provider.Scopus=de.gbv.reposis.metrics.scopus.MCRScopusMetricsProvider
MIR.Scopus.API.Key=YOUR_API_KEY
```


#### Web of Science
To use the Web of Science metrics provider you have to set the following properties in mycore.properties:

```properties
MCR.MODS.Metrics.Provider.WebOfScience=de.gbv.reposis.metrics.wos.MCRWOSMetricsProvider
MIR.WebOfScience.API.Key=YOUR_API_KEY
```

## Development

You can add these to your ~/.mycore/(dev-)mir/.mycore.properties
```
MCR.Developer.Resource.Override=/path/to/reposis_common/src/main/resources
MCR.LayoutService.LastModifiedCheckPeriod=0
MCR.UseXSLTemplateCache=false
MCR.SASS.DeveloperMode=true
```