<#ftl strip_whitespace=true>

<#--
  - File: view-collection-listing.ftl
  - 
  - Description: A HTML page that displays a collection listing.
  - 
  - Required model data:
  -   resourceContext
  -   collectionListing:
  -     collections
  -     files
  -     urls
  -     sortURLs
  -     sortProperty
  -  
  - Optional model data:
  -
  -->


<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/dump.ftl" as dumper>
<#import "/lib/view-utils.ftl" as viewutils />

<#import "/lib/collections/view-collection-listing.ftl" as coll />
<#import "/lib/collections/view-article-listing.ftl" as articles />
<#import "/lib/collections/view-event-listing.ftl" as events />
<#import "/lib/collections/view-project-listing.ftl" as projects />
<#import "/lib/collections/view-person-listing.ftl" as persons />
<#import "/lib/collections/view-image-listing.ftl" as images />
<#import "/lib/collections/view-blog-listing.ftl" as blogs />

<#assign resource = collection />

<#assign title = vrtx.propValue(resource, "title", "flattened") />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

  <#if collection.resourceType = 'image-listing'>
    <@images.addScripts collection />
  <#else>
    <#if cssURLs?exists>
      <#list cssURLs as cssURL>
        <link rel="stylesheet" href="${cssURL}" />
      </#list>
    </#if>
    <#if printCssURLs?exists>
      <#list printCssURLs as cssURL>
        <link rel="stylesheet" href="${cssURL}" media="print" />
      </#list>
    </#if>
    <#if jsURLs?exists>
      <#list jsURLs as jsURL>
        <script type="text/javascript" src="${jsURL}"></script>
      </#list>
    </#if>
  </#if>

  <#if alternativeRepresentations?exists && !(hideAlternativeRepresentation?exists && hideAlternativeRepresentation)>
    <#list alternativeRepresentations as alt>
      <link rel="alternate" type="${alt.contentType?html}" title="${alt.title?html}" href="${alt.url?html}" />
    </#list>
  </#if>

  <title>${title?html}
    <#if page?has_content>
      <#if "${page}" != "1"> - <@vrtx.msg code="viewCollectionListing.page" /> ${page}</#if>
    </#if>
  </title>
  
  <#if page?has_content>
    <#if "${page}" != "1"><meta name="robots" content="noindex, follow"/> </#if>
  </#if>
  
</head>
<body id="vrtx-${resource.resourceType}">
  
  <#assign page = page?default(1) />

  <#assign isBlogListing = resource.resourceType = 'blog-listing' />
  <#assign displayType = vrtx.propValue(resource, 'display-type', '', 'el') />
  <#assign isEventCalendarListing = (displayType?has_content && displayType = 'calendar') />
  
  <#if !isEventCalendarListing>
    <h1>${title}
      <#if page?has_content>
        <#if "${page}" != "1"> - <@vrtx.msg code="viewCollectionListing.page" /> ${page}</#if>
      </#if>
    </h1>
    <#if page == 1>
      <#-- Image -->
      <@viewutils.displayImage resource />
      <#-- Introduction -->
      <#assign introduction = vrtx.getIntroduction(resource) />
      <#if introduction?has_content && !isBlogListing>
        <div class="vrtx-introduction">
          ${introduction}
        </div>
      </#if>
    </#if>
  </#if>

     <#-- List collections: -->
     <#if page == 1>
	     <#if subCollections?size &gt; 0>
	       <#if subCollections?size &gt; 15>
	          <#assign splitList = ((subCollections?size/4)+0.75)?int />
	          <#assign interval = splitList />
	       <#elseif subCollections?size &gt; 8>
	          <#assign splitList = ((subCollections?size/3)+0.5)?int />
	          <#assign interval = splitList />
	       <#elseif subCollections?size &gt; 3>
	          <#assign splitList = ((subCollections?size/2)+0.5)?int />
	          <#assign interval = splitList />
	       <#else>
	         <#assign splitList = -1 />
	       </#if>
	       <div id="vrtx-collections" class="vrtx-collections">
	         <h2><@vrtx.msg code="viewCollectionListing.subareas" default="Subareas"/></h2>
	         <table>
	           <tr>
	             <td>
	               <ul>
	                 <#list subCollections as c>
	                   <#if c_index = splitList>
	                     </ul></td>
	                     <td><ul>
	                     <#assign splitList = splitList + interval />
	                   </#if>
	                   <#assign navigationTitle = vrtx.propValue(c.resource, "navigationTitle")?html />
	                   <#if navigationTitle?exists && navigationTitle != "">
	                     <li><a href="${c.URL.pathRepresentation?html}">${navigationTitle}</a></li>
	                   <#else>
	                     <li><a href="${c.URL.pathRepresentation?html}">${vrtx.propValue(c.resource, "title")?html}</a></li>
	                   </#if>
	                 </#list>
	               </ul>
	             </td>
	           </tr>
	         </table>
	       </div>
	     </#if>
     </#if>
     
     <#-- XXX: "additional content" (for person listing) -->
     <#assign additionalContent = vrtx.propValue(resource, "additionalContent", "", "pl") />
     <#if additionalContent?has_content>
       <div class="vrtx-additional-content">
         <@vrtx.invokeComponentRefs additionalContent />
       </div>
     </#if>
     
     <#-- List resources: -->
     <#if collection.resourceType = 'event-listing'>
       <@events.displayEvents collection=collection hideNumberOfComments=hideNumberOfComments displayMoreURLs=true />
     <#elseif searchComponents?has_content>
       <#if collection.resourceType = 'article-listing'>
         <@articles.displayArticles page=page collectionListings=searchComponents hideNumberOfComments=hideNumberOfComments displayMoreURLs=true />
       <#else>
         <#list searchComponents as searchComponent>
           <#if collection.resourceType = 'person-listing'>
             <@persons.displayPersons searchComponent title />
           <#elseif collection.resourceType = 'project-listing'>
             <@projects.displayProjects searchComponent />
           <#elseif collection.resourceType = 'image-listing'>
             <@images.displayImages searchComponent collection />
           <#elseif collection.resourceType = 'blog-listing'>
              <@blogs.displayBlogs searchComponent collection />
           <#else>
             <@coll.displayCollection collectionListing=searchComponent />
           </#if>
         </#list>
       </#if>
     </#if>
	 <div class="vrtx-paging-feed-wrapper">
		<#-- Previous/next URLs: -->
		<#if pageThroughUrls?exists >
			<@viewutils.displayPageThroughUrls pageThroughUrls page />
		</#if>
        <#-- XXX: display first link with content type = atom: -->
        <#if alternativeRepresentations?exists && !(hideAlternativeRepresentation?exists && hideAlternativeRepresentation)>
	        <#list alternativeRepresentations as alt>
	          <#if alt.contentType = 'application/atom+xml'>
	            <div class="vrtx-feed-link">
	              <a id="vrtx-feed-link" href="${alt.url?html}"><@vrtx.msg code="viewCollectionListing.feed.fromThis" /></a>
	            </div>
	            <#break />
	          </#if>
	        </#list>
	    </#if>
     </div>
  </body>
</html>
