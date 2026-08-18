<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:mcri18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="mcri18n">
  
  <xsl:output method="xml" encoding="UTF-8" />

  <xsl:param name="MCR.mir-module.EditorMail" />

  <xsl:template match="/contactMail">
    <xsl:if test="approvement='true'">
      <email>
        <from>
          <xsl:value-of select="concat(to_name, '&lt;', to_mail, '&gt;')" />
        </from>
        <to>
          <xsl:value-of select="$MCR.mir-module.EditorMail" />
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
