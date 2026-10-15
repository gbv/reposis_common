<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:output method="xml" encoding="UTF-8" />

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:template match="/contactMail">
    <xsl:if test="approvement = 'true'">
      <email>
        <from>
          <xsl:value-of select="concat(to_name, '&lt;', to_mail, '&gt;')" />
        </from>
        <to>
          <xsl:value-of select="mcrproperty:one('MCR.mir-module.EditorMail')" />
        </to>
        <bcc>
          <xsl:value-of select="concat(to_name, '&lt;', to_mail, '&gt;')" />
        </bcc>
        <subject>
          <xsl:value-of select="concat(mcri18n:translate('rep.contactMail.subjectPrefix'), ' ', subject)" />
        </subject>
        <body>
          <xsl:value-of select="body" />
        </body>
      </email>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
