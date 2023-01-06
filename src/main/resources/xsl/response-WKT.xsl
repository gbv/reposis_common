<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fn="http://www.w3.org/2005/xpath-functions"

                version="1.0"
>

    <xsl:output method="text" indent="yes" media-type="text/plain"/>
    <xsl:param name="WebApplicationBaseURL"/>
    <xsl:param name="MCR.GeoSearch.Solr.WKT.Field" />

    <xsl:template match="/response">
        <xsl:text>GEOMETRYCOLLECTION(</xsl:text>
        <xsl:apply-templates select="result/doc[count(arr[@name=$MCR.GeoSearch.Solr.WKT.Field]/str[string-length(normalize-space(string(text())))&gt;0])&gt;0]"/>
        <xsl:text>)</xsl:text>
    </xsl:template>

    <xsl:template match="doc">
        <xsl:for-each select="arr[@name=$MCR.GeoSearch.Solr.WKT.Field]/str[string-length(normalize-space(string(text())))&gt;0]">
            <xsl:value-of select="text()" />
            <xsl:if test="not(position()=last())">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:if test="not(position()=last())">
            <xsl:text>,</xsl:text>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>