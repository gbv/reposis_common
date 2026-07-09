<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="fn mods">

  <xsl:import href="xslImport:modsmeta:metadata/rep-codemeta-metadata.xsl" />
  <xsl:import href="resource:xsl/metadata/rep-metadata-utils.xsl" />

  <xsl:template match="/">
    <xsl:variable name="codemeta" select="
      mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:extension[@type='codemeta-part']/fn:map
    " />
    <xsl:if test="$codemeta">
      <div id="rep-codemeta-metadata">
        <div class="mir_metadata" style="margin-top:-30px;">
          <hr class="my-3" />
          <dl>
            <xsl:apply-templates select="$codemeta/fn:map[@key='developmentStatus']" />
            <xsl:apply-templates select="$codemeta/fn:string[@key='version']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='applicationCategory']"  />
            <xsl:apply-templates select="$codemeta/fn:array[@key='applicationSubCategory']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='programmingLanguage']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='operatingSystem']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='processorRequirements']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='memoryRequirements']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='storageRequirements']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='runtimePlatform']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='softwareRequirements']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='softwareSuggestions']" />
            <xsl:apply-templates select="$codemeta/fn:array[@key='permissions']" />
            <xsl:apply-templates select="$codemeta/fn:string[@key='codeRepository']" />
            <xsl:apply-templates select="$codemeta/fn:string[@key='buildInstructions']" />
            <xsl:apply-templates select="$codemeta/fn:string[@key='releaseNotes']" />
            <xsl:apply-templates select="$codemeta/fn:string[@key='contIntegration']" />
            <xsl:apply-templates select="$codemeta/fn:string[@key='issueTracker']" />
            <xsl:apply-templates select="$codemeta/fn:string[@key='readme']" />
          </dl>
        </div>
      </div>
      </xsl:if>
    <xsl:apply-imports/>
  </xsl:template>

  <xsl:template match="fn:map[@key='developmentStatus']">
    <xsl:variable name="status" select="fn:string[@key='@value']" />
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'rep.metadata.software.developmentStatus'" />
      <xsl:with-param name="value">
        <xsl:call-template name="get-classification-label">
          <xsl:with-param name="classification" select="$status" />
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='programmingLanguage']">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'rep.metadata.software.programmingLanguage'" />
      <xsl:with-param name="value">
        <xsl:call-template name="concat">
          <xsl:with-param name="input" select="fn:map/fn:string[@key='name']" />
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='permissions']">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'rep.metadata.software.permission'" />
      <xsl:with-param name="value">
        <xsl:call-template name="concat">
          <xsl:with-param name="input" select="fn:string" />
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='memoryRequirements']">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'rep.metadata.software.memoryRequirement'" />
      <xsl:with-param name="value">
        <xsl:call-template name="concat">
          <xsl:with-param name="input" select="fn:map/fn:string[@key='@value']" />
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='storageRequirements']">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="'rep.metadata.software.storageRequirement'" />
      <xsl:with-param name="value">
        <xsl:call-template name="concat">
          <xsl:with-param name="input" select="fn:map/fn:string[@key='@value']" />
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array[@key='softwareRequirements' or @key='softwareSuggestions']">
    <xsl:variable name="links">
      <xsl:for-each select="fn:map">
        <xsl:choose>
          <xsl:when test="fn:string[@key='name'] and fn:string[@key='codeRepository']">
            <xsl:call-template name="build-link">
              <xsl:with-param name="url" select="fn:string[@key='codeRepository']" />
              <xsl:with-param name="text" select="fn:string[@key='name']" />
            </xsl:call-template>
          </xsl:when>
          <xsl:when test="fn:string[@key='codeRepository']">
            <xsl:call-template name="build-link">
              <xsl:with-param name="url" select="fn:string[@key='codeRepository']" />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="fn:string[@key='name']" />
          </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="position() != last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable name="i18n-suffix">
      <xsl:choose>
        <xsl:when test="@key='softwareRequirements'">softwareRequirement</xsl:when>
        <xsl:otherwise>softwareSuggestion</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="concat('rep.metadata.software.', $i18n-suffix)" />
      <xsl:with-param name="value" select="$links" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:string[
    @key='codeRepository'
    or @key='buildInstructions'
    or @key='releaseNotes'
    or @key='readme'
    or @key='issueTracker'
    or @key='contIntegration']">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="concat('rep.metadata.software.', @key)" />
      <xsl:with-param name="value">
        <xsl:call-template name="build-link">
          <xsl:with-param name="url" select="." />
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:string">
    <xsl:call-template name="print-field">
      <xsl:with-param name="i18n" select="concat('rep.metadata.software.', @key)" />
      <xsl:with-param name="value" select="." />
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="fn:array">
    <xsl:choose>
      <xsl:when test="fn:map">
        <xsl:call-template name="print-field">
          <xsl:with-param name="i18n" select="concat('rep.metadata.software.', @key)" />
          <xsl:with-param name="value">
            <xsl:call-template name="concat">
              <xsl:with-param name="input" select="fn:map/fn:string[@key='@value']" />
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="print-field">
          <xsl:with-param name="i18n" select="concat('rep.metadata.software.', @key)" />
          <xsl:with-param name="value">
            <xsl:call-template name="concat">
              <xsl:with-param name="input" select="fn:string" />
            </xsl:call-template>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
