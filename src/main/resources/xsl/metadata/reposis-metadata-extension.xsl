<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mods="http://www.loc.gov/mods/v3"
  exclude-result-prefixes="fn mods">

  <xsl:import href="xslImport:modsmeta:metadata/reposis-metadata-extension.xsl"/>
  <xsl:import href="resource:xsl/metadata/reposis-metadata-utils.xsl"/>

  <xsl:template match="/">
    <xsl:variable name="codemeta"
      select="mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:extension[@type='codemeta-part']/fn:map"/>
    <xsl:if test="$codemeta">
      <div id="reposis-metadata-extension">
        <div class="mir_metadata" style="margin-top:-30px;">
          <hr class="my-3"/>
          <dl>
            <xsl:apply-templates select="$codemeta/fn:map[@key='developmentStatus']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:string[@key='version']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='applicationCategory']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='applicationSubCategory']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='programmingLanguage']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='operatingSystem']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='processorRequirement']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='memoryRequirements']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='storageRequirements']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='runtimePlatform']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='softwareRequirements']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='softwareSuggestions']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:array[@key='permissions']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:string[@key='codeRepository']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:string[@key='buildInstructions']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:string[@key='releaseNotes']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:string[@key='contIntegration']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:string[@key='issueTracker']" mode="codemeta"/>
            <xsl:apply-templates select="$codemeta/fn:string[@key='readme']" mode="codemeta"/>
          </dl>
        </div>
      </div>
      </xsl:if>
    <xsl:apply-imports/>
  </xsl:template>

  <xsl:template match="fn:map[@key='developmentStatus']" mode="codemeta">
    <xsl:variable name="status" select="fn:string[@key='@value']"/>
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'reposis.codemeta.developmentStatus'"/>
      <xsl:with-param name="value">
        <xsl:call-template name="get-classification-label">
          <xsl:with-param name="classification" select="$status"/>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='programmingLanguage']" mode="codemeta">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'reposis.codemeta.programmingLanguage'"/>
      <xsl:with-param name="value">
        <xsl:call-template name="concat">
          <xsl:with-param name="input" select="fn:map/fn:string[@key='name']"/>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='permissions']" mode="codemeta">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'reposis.codemeta.permission'"/>
      <xsl:with-param name="value">
        <xsl:call-template name="concat">
          <xsl:with-param name="input" select="fn:string"/>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='memoryRequirements']" mode="codemeta">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'reposis.codemeta.memoryRequirement'"/>
      <xsl:with-param name="value">
        <xsl:call-template name="concat">
          <xsl:with-param name="input" select="fn:map/fn:string[@key='@value']"/>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='storageRequirements']" mode="codemeta">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'reposis.codemeta.storageRequirement'"/>
      <xsl:with-param name="value">
        <xsl:call-template name="concat">
          <xsl:with-param name="input" select="fn:map/fn:string[@key='@value']"/>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='softwareRequirements' or @key='softwareSuggestions']" mode="codemeta">
    <xsl:variable name="links">
      <xsl:for-each select="fn:map">
        <xsl:choose>
          <xsl:when test="fn:string[@key='name'] and fn:string[@key='codeRepository']">
            <xsl:call-template name="build-link">
              <xsl:with-param name="url" select="fn:string[@key='codeRepository']"/>
              <xsl:with-param name="text" select="fn:string[@key='name']"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="fn:string[@key='codeRepository']">
            <xsl:call-template name="build-link">
              <xsl:with-param name="url" select="fn:string[@key='codeRepository']"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="fn:string[@key='name']"/>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="position() != last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable name="i18nSuffix">
      <xsl:choose>
        <xsl:when test="@key='softwareRequirements'">softwareRequirement</xsl:when>
        <xsl:otherwise>softwareSuggestion</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="concat('reposis.codemeta.', $i18nSuffix)"/>
      <xsl:with-param name="value" select="$links"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:string[@key='codeRepository' or @key='buildInstructions' or @key='releaseNotes' or @key='readme' or @key='issueTracker' or @key='contIntegration']" mode="codemeta">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="concat('reposis.codemeta.', @key)"/>
      <xsl:with-param name="value">
        <xsl:call-template name="build-link">
          <xsl:with-param name="url" select="."/>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:string" mode="codemeta">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="concat('reposis.codemeta.', @key)"/>
      <xsl:with-param name="value" select="."/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array" mode="codemeta">
    <xsl:choose>
      <xsl:when test="fn:map">
        <xsl:call-template name="print-field">
          <xsl:with-param name="i18n" select="concat('reposis.codemeta.', @key)"/>
          <xsl:with-param name="value">
            <xsl:call-template name="concat">
              <xsl:with-param name="input" select="fn:map/fn:string[@key='@value']"/>
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="print-field">
          <xsl:with-param name="i18n" select="concat('reposis.codemeta.', @key)"/>
          <xsl:with-param name="value">
            <xsl:call-template name="concat">
              <xsl:with-param name="input" select="fn:string"/>
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
