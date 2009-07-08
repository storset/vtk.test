<#import "/lib/vortikal.ftl" as vrtx />

<#import "vrtx-types/vrtx-boolean.ftl" as vrtxBoolean />
<#import "vrtx-types/vrtx-file-ref.ftl" as vrtxFileRef />
<#import "vrtx-types/vrtx-html.ftl" as vrtxHtml />
<#import "vrtx-types/vrtx-image-ref.ftl" as vrtxImageRef />
<#import "vrtx-types/vrtx-radio.ftl" as vrtxRadio />
<#import "vrtx-types/vrtx-string.ftl" as vrtxString />

<#import "editor/fck.ftl" as fckEditor />
<html>
<head>
  <title>Edit structured resource</title>
 <@fckEditor.setup />
<#--
 <script language="javascript">
	$(document).ready(function() {
		
		setLang("<@vrtx.requestLanguage />");
		
		var fields1 = new Array("title","firstName","surname","postalAddress","visitingAddress","email","webPage","officeNumber");
		var fields2 = new Array("scientificInformation");
		
		showHide("getExternalPersonInfo", fields1);
		showHide("getExternalScientificInformation", fields2);
			
	    $("input[name='getExternalPersonInfo']").click(
	        function(){       	
	        	showHide("getExternalPersonInfo", fields1);
	        }
	    );
	    
	    $("input[name='getExternalScientificInformation']").click(
	        function(){   	
	        	showHide("getExternalScientificInformation", fields2);
	        }
	    );
	 
	});
	
	function showHide(name,fields){
        var checkSelect = $("input[name='" + name + "']:checked").val();
        if(checkSelect == "false"){
        	for(i in fields)
            	$("." + fields[i]).show();
        }else{
        	for(i in fields)
            	$("." + fields[i]).hide();
        }           
	}
	
	/* TODO: Remove */
	function setLang(lang){
		var lang_index = 0;
		if(lang == "en")
			lang_index = 1;
		
		 var elmTitles = new Array();
		 
		 elmTitles['username'] = new Array("Brukernavn","Username");
		 elmTitles['title'] = new Array("Tittel","Title");
		 elmTitles['firstName'] = new Array("Fornavn","First name");
		 elmTitles['surname'] = new Array("Etternavn","Surname");
		 elmTitles['postalAddress'] = new Array("Postadresse","Postal Address");
		 elmTitles['visitingAddress'] = new Array("Besøksadresse","Visisting Address");
		 elmTitles['webPage'] = new Array("Hjemmeside","Web page");
		 elmTitles['officeNumber'] = new Array("Kontornummer","Office number");
		 elmTitles['getExternalPersonInfo'] = new Array("Hent ekstern person data","Get external person info");
		 elmTitles['availableHours'] = new Array("Tilgjengelig i tidsrom","Available hours");
		 elmTitles['picture'] = new Array("Bilde","Picture");
		 elmTitles['tags'] = new Array("Emneord","Tags");
		 elmTitles['getExternalScientificInformation'] = new Array("Hent ekstern informasjon om vitenskaplige arbeider"
		 											,"Get external scientific information");
		 elmTitles['content'] = new Array("Innhold","Content");
		 elmTitles['scientificInformation'] = new Array("Vitenskaplig informasjon","Scientific information");
		 											
		for(key in elmTitles)
			$("label[for=" + key +"]").text(elmTitles[key][lang_index]);
	}
</script>
-->
<style type="text/css">
.vrtx-html, .vrtx-string, .vrtx-date, .vrtx-image-ref, .vrtx-checkbox, .vrtx-dropdownlist, .vrtx-radio {
	padding: 5px 10px;
	clear:left;
	float:left;
	overflow:auto;
}

.vrtx-html {
	width:98%!important;
}

#contents{
	overflow:auto;
	background-color:#efefef;
}

.submit {
	clear:left;
	display:block;
}

</style>

</head>
<body>

<form action="${form.URL?html}" method="POST">

<#list form.formElements as elem>
	<#switch elem.description.type>
	  <#case "string">
	 	<@vrtxString.printPropertyEditView 
	 		title=elem.description.name 
	 		inputFieldName=elem.description.name 
	 		value=elem.value 
	 		classes=elem.description.name />
	    <#break>
	  <#case "html">
	    <@vrtxHtml.printPropertyEditView 
	    	title=elem.description.name 
	    	inputFieldName=elem.description.name 
	    	value=elem.value 
	    	classes=elem.description.name />
	    <@fckEditor.insert elem.description.name true false />
	    <#break>
	  <#case "boolean">
	  	<@vrtxBoolean.printPropertyEditView 
	  		title=elem.description.name 
	  		inputFieldName=elem.description.name 
	  		value=elem.value />
	  	<#break>
	  <#case "image_ref">
	  	<@vrtxImageRef.printPropertyEditView 
	  		title=elem.description.name 
	  		inputFieldName=elem.description.name 
	  		value=elem.value 
	  		baseFolder=resourceContext.parentURI />
	  	<#break>
	  <#default>
	    ny type property ${elem.description.type}
	</#switch>

	

</#list>

<input type="submit" class="submit" />

</form>
</body>
</html>
