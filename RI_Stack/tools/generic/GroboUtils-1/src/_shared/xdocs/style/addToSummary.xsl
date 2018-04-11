<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>
    <xsl:output
        method="xml"
        indent="yes"
        standalone="yes"
        encoding="ISO-8859-1" />

<!--
Two inputs: summary.xml (generated) and input-summary.xml (per-project)

    summary.xml format:
        <set>
            <project-id name="name" dir="dir" />
            <type name="name">
                <subproject name="name" version="version">
                    ...
                </subproject>
            </type>
        </set>
    
    input-summary.xml format:
        <list>
            <category name="name">
                <project>
                ...
                </project>
            </category>
        </list>
-->
        
        
<xsl:param name="projectname">[error]</xsl:param>
<xsl:param name="projectversion">0.0</xsl:param>
<xsl:variable name="summary" select="document('input-summary.xml')"/>

    
    <!-- base document
    <xsl:template match="/" >
        <xsl:message terminate="no">
        Found root.
        </xsl:message>
        
        <xsl:apply-templates />
    </xsl:template >
    -->
    
    <xsl:template match="set" >
        <xsl:message terminate="no">
        Found list.
        </xsl:message>
        
        <set>
            <xsl:apply-templates />
            
            <xsl:for-each
                select="$summary//list/category[position()=1]/project[position()=1]" >
                <project-id>
                    <xsl:attribute name="name"><xsl:value-of
                    select="$projectname" /></xsl:attribute>
                    <xsl:attribute name="dir"><xsl:value-of
                    select="name/text()" /></xsl:attribute>
                </project-id>
                
            </xsl:for-each>
        </set>
    </xsl:template>
    
    
    <xsl:template match="project-id" >
        <xsl:message terminate="no">
        Found project-id '<xsl:value-of select="@name"/>'.
        </xsl:message>
        <xsl:if test="@name != $projectname" >
            <project-id>
                <xsl:copy-of select="./@*" />
            </project-id>
        </xsl:if>
    </xsl:template>

    
    
    <xsl:template match="type" >
        <xsl:variable name="cname" select="@name" />

        <xsl:message terminate="no">
        Found type '<xsl:value-of select="$cname"/>'.
        </xsl:message>
   
        <type>
        <xsl:copy-of select="./@*" />
        
        <xsl:apply-templates />
        
        
        <xsl:apply-templates select="$summary//list/category" >
            <xsl:with-param name="catname" select="$cname" />
        </xsl:apply-templates>
        </type>
    </xsl:template>
    
    
    <!-- input-summary.xml file -->
    <xsl:template match="category" >
        <xsl:param name="catname" select="'unknown'" />
        
        <xsl:message terminate="no">
        Found input summary category '<xsl:value-of select="@name" />'.
        (owning type is '<xsl:value-of select="$catname" />')
        </xsl:message>
        <xsl:if test="@name = $catname" >
        <!-- add this project to the list -->
            <xsl:apply-templates />
        </xsl:if>
    </xsl:template>
   
    
    <!-- summary.xml: Copy all the text -->
    <xsl:template match="subproject" >
        <!-- do not copy the project in question -->
        <xsl:if test="not( @name = string($projectname) )">
        <subproject>
            <xsl:copy-of select="./@*" />
            
            <xsl:apply-templates />
        </subproject>
        </xsl:if>
    </xsl:template>
    
    
    <!-- input-summary.xml: translate and copy -->
    <xsl:template match="project" >
        <subproject>
            <xsl:attribute name="name"><xsl:value-of
                select="$projectname"/></xsl:attribute>
            <xsl:attribute name="version"><xsl:value-of
                select="$projectversion"/></xsl:attribute>
    
            <xsl:apply-templates />
        </subproject>
    </xsl:template>

    <xsl:template match="@*|node()" name="CopyWithTemplates">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
