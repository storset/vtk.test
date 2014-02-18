<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Media player library
  - 	
  -->

<#-- Minimum Flash Player version required: -->
<#assign flashPlayerVersion = "10.2.0" />

<#macro includeFlash>
  <script type="text/javascript"><!--
    if (typeof swfobject == 'undefined') {
      document.write("<scr" + "ipt src='/vrtx/__vrtx/static-resources/flash/SMP_2.0.2494-patched/10.2/lib/swfobject.js' type='text/javascript'><\/script>");
    }
  // -->
  </script>
</#macro>

<#macro genPlaceholder url dateStr isAudio=false showPlayButton=false>
  <#if isAudio>
    <#local imgSrc = "video-noflash.png" />
    <#local width = "500" />
    <#local height = "279" />
    <#local alt = vrtx.getMsg("article.media-file") />
  <#else>
    <#local imgSrc = "audio-icon.png" />
    <#local width = "151" />
    <#local height = "82" />
    <#local alt = vrtx.getMsg("article.audio-file") />
  </#if>
  <@genPlayButtonCSS showPlayButton />
  <div id="mediaspiller-${dateStr}"<#if showPlayButton> class="vrtx-media-player-no-flash"</#if>>
    <a class="vrtx-media" href="${url}">
	  <img src="<#if poster?exists>${poster?html}<#else>/vrtx/__vrtx/static-resources/themes/default/icons/${imgSrc}</#if>" width="${width}" height="${height}" alt="${alt}"/>
      <#if showPlayButton><a class="playbutton" href="${url}"></a></#if>
    </a>
  </div>
</#macro>

<#macro genPlayButtonCSS showPlayButton>
  <#if showPlayButton>
    <style type="text/css">
      .vrtx-media-player-no-flash, .vrtx-media-player-no-flash img { width: 507px; height: 282px; float: left; }
      .vrtx-media-player-no-flash { background-color: #000000; position: relative; }
      .vrtx-media-player-no-flash .playbutton { 
        position: absolute; top: 90px; left: 195px; width: 115px; height: 106px; display: block;
      }
      .vrtx-media-player-no-flash .playbutton,.vrtx-media-player-no-flash .playbutton:visited,.vrtx-media-player-no-flash .playbutton:active {
        background: url('/vrtx/__vrtx/static-resources/themes/default/icons/video-playbutton.png') no-repeat center center;
      }
      .vrtx-media-player-no-flash .playbutton:hover { background-image: url('/vrtx/__vrtx/static-resources/themes/default/icons/video-playbutton-hover.png'); }
    </style>
  </#if>
</#macro>