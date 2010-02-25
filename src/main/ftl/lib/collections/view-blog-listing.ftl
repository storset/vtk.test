<#import "/layouts/tag-cloud.ftl" as tagCloud />

<#macro displayBlogs blogListing collection>
	<#assign introduction = vrtx.getIntroduction(collection) />
	<div class="container">
		<div class="main-article-listing">
		<#if page == 1>
		<div class="vrtx-introduction">
	         ${introduction}
	    </div>
		</#if>
		<@articles.displayArticles page=page collectionListings=searchComponents hideNumberOfComments=hideNumberOfComments displayMoreURLs=true />
		</div>
		<div class="additional-information">
			<h3><@vrtx.msg code="decorating.tags" /></h3>
			<@tagCloud.crateTagCloud />
			<@listComments />
		</div>
	</div>
</#macro>

<#macro listComments >
<#if (comments?size > 0) >
	<div class="vrtx-recent-comments">
		<a class="comments-title"><@vrtx.msg code="commenting.comments.recent" /></h3>
		<ul class="items">
	    <#list comments as comment >
	    	<#local url = urlMap[comment.URI] + '#comment-' + comment.ID />
	    	<#local title = resourceMap[comment.URI].title />
	        <li class="comment">
	          <a class="item-title" href="${url?html}">
	          	${comment.author.description?html} <@vrtx.msg code="commenting.comments.on" default="about" /> "${title?html}"
	          </a>
	          <span class="published-date"><@vrtx.date value=comment.time format='long' /></span>
	          <div class="item-description">
	          	${comment.content}
	          </div>
	        </li>
	    </#list>
	    </ul>
	    <a href="${moreCommentsUrl}" class="more-url"><@vrtx.msg code="commenting.comments.more" /></a>
	</div>
</#if>
</#macro>