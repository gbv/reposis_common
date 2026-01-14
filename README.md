
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

### Shibboleth

#### Realm Updater

The realm updater takes the dfn aai metadata and takes all the scopes and creates a realm for each scope.
The realm updater is configured in mycore.properties, e.g.:

```properties
MCR.Cronjob.Jobs.RealmFileUpdater=de.gbv.reposis.user.shibboleth.MCRSAMLEntitiesRealmFileUpdater
MCR.Cronjob.Jobs.RealmFileUpdater.Cron=0 4 * * *
#MCR.Cronjob.Jobs.RealmFileUpdater.Cron=* * * * *
MCR.Cronjob.Jobs.RealmFileUpdater.Url=https://www.aai.dfn.de/metadata/dfn-aai-idp-metadata.xml
MCR.Cronjob.Jobs.RealmFileUpdater.PreserveRealmsWithUsers=true
```

The cronjob uses the [mycore-cronjob implementation](https://www.mycore.de/documentation/basics/basics_cronjob/). It has the following properties:

| Property                                                  | Default                  | Description                                                                                  |
|-----------------------------------------------------------|--------------------------|----------------------------------------------------------------------------------------------|
| MCR.Cronjob.Jobs.RealmFileUpdater                         |                          | The Cronjob definition. Contains the class of the Implementation.                            |
| MCR.Cronjob.Jobs.RealmFileUpdater.Cron                    |                          | When does the cron job run.                                                                  |
| MCR.Cronjob.Jobs.RealmFileUpdater.Url                     |                          | The URL to the DFN Metadata, which will be converted.                                        |
| MCR.Cronjob.Jobs.RealmFileUpdater.PreserveRealmsWithUsers | true                     | If true Cronjob does not delete realms if there is a user in the db with the realm assigned. |
| MCR.Cronjob.Jobs.RealmFileUpdater.RealmsFilePath          | %MCR.datadir%/realms.xml | The path where the realms.xml can be found.                                                  |
| MCR.Cronjob.Jobs.RealmFileUpdater.FilterEntities          |                          | A comma separated list of entities that should be filtered.                                  |
| MCR.Cronjob.Jobs.RealmFileUpdater.PreserveRealms          | local,shibboleth         | A comma separated list of realms that should not be changed.                                 |

#### Shibboleth Login 2

The Shibboleth Login 2 is a new implementation of the Shibboleth Login. Its main difference to the old implementation is that it is more flexible regarding the attribute mapping and the user creation.
You need to add the Servlet to the web-fragment.xml:

```xml
  <servlet>
    <servlet-name>MCRShibbolethLoginServlet2</servlet-name>
    <servlet-class>de.gbv.reposis.user.shibboleth.MCRShibbolethLoginServlet2</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>MCRShibbolethLoginServlet2</servlet-name>
    <url-pattern>/servlets/MCRShibbolethLoginServlet2</url-pattern>
  </servlet-mapping>
```

The configuration is done in mycore.properties:

```properties
MCR.User.Shibboleth.PersistUser=true
MCR.User.Shibboleth.Mapper=de.gbv.reposis.user.shibboleth.MCRDefaultConfigurableShibbolethUserMapper
MCR.User.Shibboleth.Merger=de.gbv.reposis.user.shibboleth.MCRDefaultConfigurableShibbolethUserMerger
MCR.User.Shibboleth.NewUserHandler=de.gbv.reposis.user.shibboleth.MCRDefaultConfigurableShibbolethNewUserHandler
```

| Property                                | Default                                                                       | Description                                                             |
|-----------------------------------------|-------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| MCR.User.Shibboleth.PersistUser         | true                                                                          | If true the user will be persisted in the database when he logs in.     |
| MCR.User.Shibboleth.Mapper              | de.gbv.reposis.user.shibboleth.MCRDefaultConfigurableShibbolethUserMapper     | The class that maps the shibboleth attributes to the user object.       |
| MCR.User.Shibboleth.Mapper.DefaultRoles |                                                                               | A comma separated list of roles that will be assigned to the user .     |
| MCR.User.Shibboleth.Merger              | de.gbv.reposis.user.shibboleth.MCRDefaultConfigurableShibbolethUserMerger     | The class that merges the user object with the user object from the db. |
| MCR.User.Shibboleth.NewUserHandler      | de.gbv.reposis.user.shibboleth.MCRDefaultConfigurableShibbolethNewUserHandler | The class that handles the new user. Eg. send an email.                 |

The default mapper maps the following attributes:

| Shibboleth Attribute | User Attribute | 
|----------------------|----------------|
| displayName          | name           | 
| mail                 | email          | 

The user id is taken from the getRemoteUser() method of the request.

The default merger doesnt merge anything.

The default new user handler does nothing.

##### MCRConfigurableNewShibbolethUserMailer

The MCRConfigurableNewShibbolethUserMailer is a new user handler that sends an email to somebody when he logs in for the
first time.
The Mailer is configured in mycore.properties:

```properties
MCR.User.Shibboleth.NewUserHandler=de.gbv.reposis.user.shibboleth.MCRConfigurableNewShibbolethUserMailer
MCR.User.Shibboleth.NewUserHandler.MailTo=exampleReceiver@mail.com
MCR.User.Shibboleth.NewUserHandler.MailFrom=exampleSender@mail.com
MCR.User.Shibboleth.NewUserHandler.MailSubjectKey=shibboleth.newUser.mail.subject
MCR.User.Shibboleth.NewUserHandler.MailBodyKey=shibboleth.newUser.mail.body
MCR.User.Shibboleth.NewUserHandler.MailLocaleKey=de
MCR.User.Shibboleth.NewUserHandler.Bcc=false
```

| Property                                          | Default | Description                                                       |
|---------------------------------------------------|---------|-------------------------------------------------------------------|
| MCR.User.Shibboleth.NewUserHandler.MailTo         |         | The email address of the receiver. Can be a comma separated list. |
| MCR.User.Shibboleth.NewUserHandler.MailFrom       |         | The email address of the sender.                                  |
| MCR.User.Shibboleth.NewUserHandler.MailSubjectKey |         | The key which will be use to translate the subject.               |
| MCR.User.Shibboleth.NewUserHandler.MailBodyKey    |         | The key which will be use to translate the body.                  |
| MCR.User.Shibboleth.NewUserHandler.MailLocaleKey  |         | The locale which will be used for the translation                 |
| MCR.User.Shibboleth.NewUserHandler.Bcc            | true    | If true the receiver will be in the bcc field.                    |

The arguments for the translation are:

* {0} = the user id
* {1} = the label of the realm
* {2} = the email address of the user
* {3} = the name of the user

All classes are loaded with
the [configurable instace concept](https://www.mycore.de/documentation/basics/basics_configurable_instance/).

### Agreement Mailer

| Property                   | Description                                                                                                |
|----------------------------|------------------------------------------------------------------------------------------------------------|
| MCR.mir-module.MailSender  | The Mail Address from which the agreement will be sent                                                     |
| MCR.mir-module.EditorMail  | The Mail Address where the agreement will be sent to                                                       |
| MCR.Mail.Server            | The Mail Server which will be used to send the mail                                                        |
| MIR.Agreement.MailTemplate | The template which will be used for the Mail. Default Value: agreement_mail_template.xhtml                 |
| MIR.Agreement.File         | The file which will be attached to the mail. The file must be stored in the web folder `content/publish/`. |

There is also an editor specific property and an event handler which needs to be set in the mycore.properties:
```properties
MIR.EditorForms.CustomIncludes=%MIR.EditorForms.CustomIncludes%,xslStyle:editor/mir2xeditor:webapp:editor/editor-agreement-customization.xed
MCR.EventHandler.MCRObject.019j.Class=de.gbv.reposis.agreement.VZGMailAgreementEventHandler
```


The Mail template is a xhtml file which can be found in the reposis_common module.
The template has Variables which will be replaced with the actual values. The placeholders are Elements in the with the
Namespace `xmlns:plc="https://gbv.de/mail-placeholder"`.

The template has the following variables:

| Variable       | Description                                                                 |
|----------------|-----------------------------------------------------------------------------|
| `<plc:user />` | The name of the user                                                        |
| `<plc:link />` | The link to the accepted agreement                                          |


There are Message Properties which can be set in the message.properties file:

| Property                           | Description                                                                             | Default Value                                     |
|------------------------------------|-----------------------------------------------------------------------------------------|---------------------------------------------------|
| project.form.agreement.accept.pre  | The text which will be displayed before the link to the agreement in the editor         | Ich habe die                                      |
| project.form.agreement.accept.post | The text which will be displayed after the link to the agreement in the editor          | Einverständniserklärung                           |
| project.form.agreement.accept.link | The text which will be displayed as the link to the agreement in the editor             | gelesen und akzeptiere sie.                       |
| project.form.agreement             | The text which will be displayed as a legend for the agreement in the editor            | Einverständniserklärung                           |
| project.form.validation.agreement  | The text which will be displayed as a validation error if the agreement is not accepted | Sie müssen der Einverständniserklärung zustimmen. |

## Sitelinks
The Sitelinks resource provides sitelinks based on Solr and is optimized for **Google Scholar**.
It allows for hierarchical navigation through sitelinks, organized by publication year and month, using path parameters.
By default, the resource is disabled but can be activated and configured as follows.
```
# Activates the resource
MCR.Jersey.Resource.Packages=%MCR.Jersey.Resource.Packages%,de.gbv.reposis.sitelinks.resources
# Basic query for Solr
Sitelinks.Resource.BasicFilterQuery=worldReadable:true AND ((objectType:mods AND -state:*) OR (objectType:mods AND state:published))
# Pagination / page size of the sitelinks
Sitelinks.PageSize=100
```
The entry page is located at `/rsc/sitelinks/` and may need to be allowed in the `robots.txt` file.

## Development

You can add these to your ~/.mycore/(dev-)mir/.mycore.properties

```
MCR.Developer.Resource.Override=/path/to/reposis_common/src/main/resources
MCR.LayoutService.LastModifiedCheckPeriod=0
MCR.UseXSLTemplateCache=false
MCR.SASS.DeveloperMode=true
```
