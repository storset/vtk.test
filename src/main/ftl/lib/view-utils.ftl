
<#--
  - File: view-utils.ftl
  - 
  - Description: Utility macros for displaying content in views
  -   
  -->

<#import "vortikal.ftl" as vrtx />

<#--
 * Displays the introduction, if any, of a resource.
 * 
 * @param resource The resource to display introduction from
-->
<#macro displayIntroduction resource>
  <#local introduction = vrtx.propValue(resource, "introduction") />
  <#if introduction != "">
    <div class="vrtx-introduction">${introduction}</div>
  </#if>
</#macro>

<#--
 * Displays the image-property of a resource.
 * 
 * @param resource The resource to display image from
-->
<#macro displayImage resource>
  
  <#local imageRes = vrtx.propResource(resource, "picture") />
  <#local introductionImage = vrtx.propValue(resource, "picture") />
  <#local caption = vrtx.propValue(resource, "caption") />
  
  <#-- Flattened caption for alt-tag in image -->
  <#local captionFlattened>
        <@vrtx.flattenHtml value=caption escape=true />
  </#local>
  
  <#if introductionImage != "">
  
    <#if imageRes == "">
      <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
    <#else>
      <#local pixelWidth = imageRes.getValueByName("pixelWidth")?default("") />
      <#local authorName = imageRes.getValueByName("authorName")?default("") />
        
      <#local style="" />
      <#if pixelWidth != "">
         <#local style = "width:" + pixelWidth+ "px;" />
      </#if>
        
      <#if caption != ""><#-- Caption is set -->
        <div class="vrtx-introduction-image" <#if style?has_content>style="${style}"</#if>>
	         <img src="${introductionImage}" alt="${captionFlattened}" />
            <div class="vrtx-imagetext">
                 <div class="vrtx-imagedescription">${caption}</div>
                 <span class="vrtx-photo">
                    <#if authorName != ""><#-- Image authors is set -->
                      <span class="vrtx-photo-prefix"><@vrtx.msg code="article.photoprefix" />: </span>${authorName}
                    </#if>
                 </span>
           </div> 
       </div>
      <#else>
         <#if authorName != ""><#-- No caption but image author set -->
            <div class="vrtx-introduction-image" <#if style?has_content>style="${style}"</#if>>
            <img src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />   
              <div class="vrtx-imagetext">
                <span class="vrtx-photo">
                  <span class="vrtx-photo-prefix"><@vrtx.msg code="article.photoprefix" />: </span>${authorName}
                </span>
              </div>     
            </div>
         <#else><#-- No caption or image author set -->
            <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
         </#if>
	  </#if>
    </#if>
   </#if>
  
</#macro>


<#--
 * Shows the start- and end-date of an event, seperated by a "-".
 * If the two dates are identical, only the time of enddate is shown.
 * 
 * @param resource The resource to evaluate dates from
-->
<#macro displayTimeAndPlace resource title hideEndDate=false hideLocation=false hideNumberOfComments=false>
  
  <#local start = vrtx.propValue(resource, "start-date") />
  <#local startiso8601 = vrtx.propValue(resource, "start-date", "iso-8601") />
  <#local startshort = vrtx.propValue(resource, "start-date", "short") />
  <#local end = vrtx.propValue(resource, "end-date") />
  <#local endiso8601 = vrtx.propValue(resource, "end-date", "iso-8601") />
  <#local endshort = vrtx.propValue(resource, "end-date", "short") />
  <#local endhoursminutes = vrtx.propValue(resource, "end-date", "hours-minutes") />
  <#local location = vrtx.propValue(resource, "location") />
  <#local mapurl = vrtx.propValue(resource, "mapurl") />
  
  <#local isostarthour = "" />
  <#if startiso8601 != "" >
    <#local isostarthour = startiso8601?substring(11, 16) />
  </#if>
  
  <#local isoendhour = "" />
  <#if endiso8601 != "" >
    <#local isoendhour = endiso8601?substring(11, 16) />
  </#if>
  <#local locationMsgCode = "article.time" />
  <#if location != "" && !hideLocation><#t/>
    <#local locationMsgCode = "article.time-and-place" />
  </#if>
  <span class="time-and-place"><@vrtx.msg code=locationMsgCode />:</span>
  <span class="summary" style="display:none;">${title}</span>
  <#if start != "">
    <abbr class="dtstart" title="${startiso8601}">
    <#if isostarthour != "00:00">
      ${start}<#t/>
    <#else>
      ${startshort}<#t/>
    </#if>
    </abbr><#t/>
  </#if>
  <#if end != "" && !hideEndDate>
    <#if startshort == endshort>
      <#if isoendhour != "00:00">
        <#t /> - <abbr class="dtend" title="${endiso8601}">${endhoursminutes}</abbr><#rt />
      </#if>
    <#else>
      <#if start == "">
        (<@vrtx.msg code="event.ends" />) 
      <#else>
        - 
      </#if>
      <abbr class="dtend" title="${endiso8601}">
      <#if isoendhour != "00:00">
        ${end}<#t/>
      <#else>
        ${endshort}<#t/>
      </#if>
      </abbr><#t/>
    </#if>
  </#if>
  <#if location != "" && !hideLocation><#t/>,
    <span class="location">
    <#if mapurl == "">
      ${location}
    <#else>
      <a href="${mapurl?html}">${location}</a>
    </#if>
    </span>
  </#if>
  
  <#local constructor = "freemarker.template.utility.ObjectConstructor"?new() />
  <#local currentDate = constructor("java.util.Date") />
  <#local isValidStartDate = validateStartDate(resource, currentDate)?string == 'true' />
  
  <#local numberOfComments = vrtx.prop(resource, "numberOfComments") />
  <#if numberOfComments?has_content || isValidStartDate>	
    <div class="vrtx-number-of-comments-add-event-container">
  </#if>
  <#if !hideNumberOfComments >
    <#local locale = springMacroRequestContext.getLocale() />
    <@displayNumberOfComments resource locale />
  </#if>
  
  <#if isValidStartDate>
    <span class="vrtx-add-event">
      <a class="vrtx-ical" href='${resource.URI}?vrtx=ical'><@vrtx.msg code="event.add-to-calendar" /></a><a class="vrtx-ical-help" href="${vrtx.getMsg("event.add-to-calendar.help-url")}"></a>
    </span>
  </#if>
  
  <#if numberOfComments?has_content || isValidStartDate>
    </div>
  </#if>
	
</#macro>


<#--
 * Check the start date of an event to see if it's greater than the current date
 * Check if the event is "upcoming" and has a valid start date
 * 
 * @param event The resource to evaluate start date for
 * @param currentDate The date to compare the events start date to
-->
<#function validateStartDate event currentDate>
  <#local startDate = event.getPropertyByPrefix(nullArg, "start-date")?default("") />
  <#if !startDate?has_content>
    <#local startDate = event.getPropertyByPrefix('resource', "start-date")?default("") />
  </#if>
  <#if startDate != "">
    <#return startDate.getDateValue()?datetime &gt; currentDate?datetime />
  </#if>
  <#return "false"/>
</#function>

<#macro displayNumberOfComments resource locale  >
  <#local numberOfComments = vrtx.prop(resource, "numberOfComments") />
  <#if numberOfComments?has_content >
    <a href="${resource.URI}#comments" class="vrtx-number-of-comments">
    <#if numberOfComments.intValue?number &gt; 1>
      <@vrtx.localizeMessage code="viewCollectionListing.numberOfComments" default="" args=[numberOfComments.intValue] locale=locale />
    <#else>
      <@vrtx.localizeMessage code="viewCollectionListing.numberOfCommentsSingle" default="" args=[] locale=locale />
    </#if>
    </a>
  </#if>
</#macro>

<#macro displayMediaPlayer mediaRes media>

 <#if media != ""> 
  <div class="vrtx-media-ref">
    <#if mediaRes != "" && mediaRes.resourceType == 'audio'>
      <script type="text/javascript" language="JavaScript" src="${flashPlayer.jsURL?html}/"></script>
      <object type="application/x-shockwave-flash" data="${flashPlayer.flashURL?html}" id="audioplayer1" height="24" width="290">
        <param name="movie" value="${flashPlayer.flashURL?html}"/>
        <param name="FlashVars" value="playerID=1&amp;soundFile=${media}"/>
        <param name="quality" value="high"/>
        <param name="menu" value="false"/>
        <param name="wmode" value="transparent"/>
      </object>
    <#elseif (mediaRes != "" && mediaRes.contentType == 'video/quicktime')>
      <object id="videoplayer1" classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" width="320" height="255" codebase="http://www.apple.com/qtactivex/qtplugin.cab">
        <param name="src" value="${media}"/>
        <param name="autoplay" value="false"/>
        <param name="controller" value="true"/>
        <param name="loop" value="false"/>
        <param name="scale" value="aspect" />         
        <embed id="videoplayer1" src="${media}" width="320" height="255" autoplay="false" controller="true" loop="false" scale="aspect" pluginspage="http://www.apple.com/quicktime/download/">
        </embed>
      </object>
    <#else>
      <a class="vrtx-media" href="${media}"><@vrtx.msg code="article.media-file" /></a>
    </#if>
  </div>
 </#if>

</#macro>

<#macro displayPageThroughUrls pageThroughUrls page >
		<#if pageThroughUrls?exists && (pageThroughUrls?size > 1) >
			 <span class="vrtx-paging-wrapper"> 
		         <#if (page-2 > -1)  && pageThroughUrls[page-2]?exists >
			  		<a class="vrtx-previous" href="${pageThroughUrls[page-2]?html}"><@vrtx.msg code="viewCollectionListing.previous" /></a>
			   	 </#if> 
			     <#list pageThroughUrls as url>
			       	<a href="${url?html}" class="vrtx-page-number <#if (url_index+1) = page>vrtx-marked</#if>">${(url_index+1)}</a>
			     </#list>
			     <#if (pageThroughUrls?size > page) && pageThroughUrls[page-2]?exists > 
			        <a class="vrtx-next" href="${pageThroughUrls[page]?html}"><@vrtx.msg code="viewCollectionListing.next" /></a>
			     </#if>
		   	 </span> 
		</#if>
</#macro>
