
# reposis-common

## Installation Instructions

* run `mvn clean install`
* copy jar to ~/.mycore/(dev-)mir/lib/

## Features

### Properties GUI

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