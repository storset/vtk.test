<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro addScripts collection>
  <#if cssURLs?exists>
    <@addScriptURLs "css" "common" cssURLs />
    <@addScriptURLs "css" listingType cssURLs />
  </#if>
  <#if jsURLs?exists>
    <@addScriptURLs "js" "common" jsURLs />
    <@addScriptURLs "js" listingType jsURLs />
  </#if>
</#macro>

<#macro addScriptURLs scriptType listingType urls>
  
  <#if urls[listingType]?exists>
    <#list urls[listingType] as commonUrl>
      <#if scriptType == "css">
        <link rel="stylesheet" href="${commonUrl}" />
      <#elseif scriptType == "js">
        <script type="text/javascript" src="${commonUrl}"></script>
      </#if>
    </#list>
  </#if>

</#macro>

<#macro displayCollection collectionListing>

  <#local resources=collectionListing.files />
  <#if (resources?size > 0)>
     <script type="text/javascript"><!--
       $(window).load(function() {
         if($("#right-main").length) {
           var cut = ".last-four";
         } else {
           var cut = ".last-five";
         }
	     $('ul.vrtx-image-listing').find(".vrtx-image-entry:not(" + cut + ")")
	       .css("marginRight", "18px !important;").end()
	       .masonry({singleMode: false});
	   });
     // -->
     </script>
     
     <#if collectionListing.title?exists && collectionListing.offset == 0>
      <h2>${collectionListing.title?html}</h2>
     </#if>
    
     <div class="vrtx-image-listing-container">
       <ul class="vrtx-image-listing">
       <#assign count = 1 />

       <#list resources as r>
         <#if count % 4 == 0 && count % 5 == 0>
           <li class="vrtx-image-entry last last-four last-five">
         <#elseif count % 4 == 0>
           <li class="vrtx-image-entry last last-four">
         <#elseif count % 5 == 0>
           <li class="vrtx-image-entry last-five">
         <#else>
           <li class="vrtx-image-entry">
         </#if>
         
         <#local resourceType = "" />
         <#if r.resourceType?exists>
            <#local resourceType = r.resourceType />
         </#if>
         
         <#local contentType = vrtx.propValue(r, 'contentType') />
         <div class="vrtx-image-container">
         
           <#if resourceType == "audio">
             <a href="${collectionListing.urls[r.URI]?html}">
               <img src="/vrtx/__vrtx/static-resources/themes/default/icons/audio-icon.png" alt="audio icon" />
             </a>
           <#elseif resourceType == "video">
               <#local introImgURI = vrtx.propValue(r, 'poster-image') />     	 
	           <#if introImgURI?exists && introImgURI != "">
	    			<#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
	    	  	<#else>
	    			<#local thumbnail =  vrtx.relativeLinkConstructor(r.URI, 'displayThumbnailService') />
	   		   	</#if>
	           	 
            	<a href="${collectionListing.urls[r.URI]?html}">
              		 	<img src="${thumbnail?html}" alt="video icon" />
             	</a>
            </#if>
         </div>
         
         <div class="vrtx-image-info">
           <div class="vrtx-image-title">
             <a class="vrtx-title" href="${collectionListing.urls[r.URI]?html}">${vrtx.propValue(r, "title", "", "")?html}</a>
		   </div>
		   
           <#list collectionListing.displayPropDefs as displayPropDef>
             <#assign val = "" />
             <#if displayPropDef.name = 'introduction'>
               <#assign val = vrtx.getIntroduction(r) />
             <#elseif displayPropDef.type = 'IMAGE_REF'>
             <#elseif displayPropDef.name = 'lastModified'>
               <#assign val = vrtx.propValue(r, displayPropDef.name, 'short') />
             <#elseif displayPropDef.name = 'duration'  >
                <#if r.getProperty(displayPropDef)?exists >                 
                    <#local property = r.getProperty(displayPropDef) />
                    <#if property?exists>
                        <div class="${displayPropDef.name}">                
                            <@vrtx.displayTime property.intValue />
                        </div>
                    </#if>
                </#if>
             <#else>
                <#assign val = vrtx.propValue(r, displayPropDef.name) />
             </#if> 
             <#if val?has_content>
               <div class="${displayPropDef.name}">
                 ${val}
               </div>            
             </#if>
           </#list>
         </div>
         </li>
         <#assign count = count +1 />
       </#list>
       </ul>
     </div>
  </#if>
</#macro>
