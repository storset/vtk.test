<#ftl strip_whitespace=true>

<#--
  - File: article.ftl
  - 
  - Description: Article view
  - 
  - Required model data:
  -   resource
  -
  -->

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#function propValue propName resource=resource format="long">
<#local prop = resource.getPropertyByPrefix("", propName)?default("") />
<#if prop != "">
  <#local type = prop.definition.type />
  <#if type = 'DATE' || type = 'TIMESTAMP'>
    <#local locale = springMacroRequestContext.getLocale() />
    <#return prop.getFormattedValue(format, locale) />
  <#else>
    <#return prop.formattedValue />
  </#if>

</#if>
<#return "" />
</#function>

<#function propResource propName>
  <#local prop = resource.getPropertyByPrefix("", propName)?default("") />
  <#if prop != "">
    <#local def = prop.definition />
    <#local type = def.type />
    <#if type = 'IMAGE_REF'>
      <#local tmpResource = resource.getPropResource(def)?default("") />
      <#return tmpResource />
    </#if>
  </#if>
  <#return "" />
</#function>


<#assign title = propValue("userTitle") />
<#if title == "">
  <#assign title = vrtx.msg code="missingTitle" />
</#if>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${title}</title>

    <style type="text/css">

      /* Ingress */

      div.introduction {margin:0;padding:0;}

      /* Bilde uten bildetekst */

      img.introduction-image {
        float: right;
        margin: 0 0 0.5em 0.75em;
        border:1px solid #ddd; 
      }

      /* Bilde med bildetekst */

      div.introduction-image {
        border:1px solid #ddd; 
        float: right;
        margin: 0em 0em 0.5em 0.75em;
      }

      div.introduction-image img {
      }

      div.introduction-image div.text {
        overflow: hidden;
        padding: 0.5em; 
        background-color: #eee;
      }

      div.introduction-image div.text span.title{
        font-weight: bold;
      }

      object {
        margin-top: -0.5em;
        margin-bottom: 0.5em;
      }

      /* Byline */

      hr.byline {
        display:none;
      }

      /* Start, slutt og sted */

      abbr {
        text-decoration: none;
        border-bottom: 0;
      }

     /* Body */  

     div.bodyText {clear:left;} 
           
    </style>
</head>
  <body>
    <h1>${title}</h1>

    <#-- Image --> 

    <#assign imageRes = propResource("picture") />
    <#assign introductionImage = propValue("picture") />
    <#if introductionImage != "">
      <#if imageRes == "">
        <img class="introduction-image" src="${introductionImage}" alt="ingressbilde" />
      <#else>

        <#assign userTitle = propValue("userTitle", imageRes) />
        <#assign desc = imageRes.getValueByName("description")?default("") />

	<#if userTitle == "" && desc == "">  
          <img class="introduction-image" src="${introductionImage}" alt="ingressbilde" />
	<#else>
          <#assign pixelWidth = imageRes.getValueByName("pixelWidth")?default("") />
          <#if pixelWidth != "">
            <#assign style = "width:" + pixelWidth+ "px;" />
          </#if>
	 	 
          <div class="introduction-image" style="${style}">
	    <#if userTitle != "">
	      <img src="${introductionImage}" alt="${userTitle?html}" />
	    <#else>
	      <img src="${introductionImage}" alt="ingressbilde" />
	    </#if>
            <div class="text">
	      <#if userTitle != "">
		<span class="title">${userTitle?html}<#if desc != "">: </#if></span>
	      </#if>
	      <#if desc != "">
		<span class="description">${desc?html}</span>
	      </#if>
	    </div> 
	  </div>
	</#if>
      </#if>
    </#if>

    <#-- Ingress --> 

    <#assign introduction = propValue("introduction") />
    <#if introduction != "">
      <div class="introduction">
        ${introduction}
      </div>
    </#if>

    <#-- Media ref --> 

    <#assign mediaRes = propResource("media-ref") />

    <#if mediaRes != "" && mediaRes.resourceType == 'audio'>
      <#assign media = propValue("media-ref") />
      <script type="text/javascript" language="JavaScript" src="${mediaPlayerBase.url?html}/audio-player.js"></script>
      <object type="application/x-shockwave-flash" data="${mediaPlayerBase.url?html}/player.swf" id="audioplayer1" height="24" width="290">
	<param name="movie" value="${mediaPlayerBase.url?html}/player.swf"/>
	<param name="FlashVars" value="playerID=1&amp;soundFile=${media}"/>
	<param name="quality" value="high"/>
	<param name="menu" value="false"/>
	<param name="wmode" value="transparent"/>
      </object>
    </#if>

    <#-- Authors and published date --> 

    <#assign authors = propValue("authors") />
    <#assign published = propValue("published-date") />
    <#if authors != "" || published != "">
      <div class="byline">
        <#if authors != "" && published != "">
          <@vrtx.msg code="article.by" /> ${authors?html} <br />${published}
        <#elseif authors != "">
          <@vrtx.msg code="article.by" /> ${authors?html}
        <#elseif published != "">
          ${published}
	</#if>
      </div>
    </#if>

    <#-- Start-date, end-date and location --> 

    <#assign start = propValue("start-date") />
    <#assign startiso8601 = propValue("start-date", resource, "iso-8601") />
    <#assign end = propValue("end-date") />
    <#assign endiso8601 = propValue("end-date", resource, "iso-8601") />
    <#assign location = propValue("location") />

    <#if start != "" || end != "" || location != "">
      <div class="vevent">
        <span class="summary" style="display:none;">${title}</span>
	<#if start != ""><#-- @vrtx.msg code="article.starts" />: -->
	  <abbr class="dtstart" title="${startiso8601}">${start}</abbr>
	</#if>
	<#if end != ""><#-- @vrtx.msg code="article.ends" />: -->	  
	  - <abbr class="dtend" title="${endiso8601}">${end}</abbr></#if><#if location != "">,<#-- @vrtx.msg code="article.location" />: -->
	  <span class="location">${location}</span><br/></#if>
      </div>
    </#if>

    <div class="bodyText">
      ${resource.bodyAsString}
    </div>

    <#-- Keywords -->

    <#assign keywords = resource.getValueByName("keywords")?default("") />
    <#assign tagsProp = resource.getPropertyByName("keywords")?default("") />
    <#if keywords != "">
      <div class="keywords">
        ${tagsProp.definition.getLocalizedName(springMacroRequestContext.locale)}:
	<#list tagsProp.values as tag>
          <a href="/?vrtx=tags&amp;tag=${tag.stringValue?html}">${tag.stringValue?html}</a>
	</#list>
      </div>
    </#if>

  </body>
</html>


