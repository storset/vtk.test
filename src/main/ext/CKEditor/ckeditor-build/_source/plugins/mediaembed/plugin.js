/*
* @example An iframe-based dialog with custom button handling logics.
*/

var props = {
  "url" : "",
  "width" : 256,
  "height" : 192,
  "autoplay" : "false",
  "contentType" : ""
};

( function() {
    CKEDITOR.plugins.add( 'MediaEmbed',
    {
        requires: [ 'iframedialog' ],
        init: function( editor )
        {
           var me = this;
           CKEDITOR.dialog.add( 'MediaEmbedDialog', function (editor)
           {	
              return {
                 title : 'Embed Media Dialog',
                 minWidth : 550,
                 minHeight : 200,
                 contents :
                       [
                          {
                             id : 'iframe',
                             label : 'Embed Media',
                             expand : true,
                             elements :
                                   [
                                      {
						               type : 'html',
						               id : 'pageMediaEmbed',
						               label : 'Embed Media',
						               style : 'width : 100%',
						               html : '<iframe src="'+me.path.toLowerCase()+'dialogs/mediaembed_'+editor.config.language+'.html" frameborder="0" name="iframeMediaEmbed" id="iframeMediaEmbed" allowtransparency="1" style="width:100%;height:250px;margin:0;padding:0;"></iframe>'
						              }
                                   ]
                          }
                       ],
                  onShow : function() {
            	    var check = setInterval(function() { // check each 50ms if iframe content is loaded
            	      var theIframe = $("iframe#iframeMediaEmbed");
            	      if(theIframe) {
            	        var contents = theIframe.contents();
            	        if(contents.find("#chkAutoplay").attr("checked") != "undefined") {
            	          // Put values in dialog
            	          contents.find("#txtUrl").val(props.url);
            	          contents.find("#txtWidth").val(props.width);
            	          contents.find("#txtHeight").val(props.height);
            	          contents.find("#txtContentType").val(props.contentType);
                	      if(props.autoplay == "true") {
                	        contents.find("#chkAutoplay").attr("checked", true);  	
                	      } else {
                	        contents.find("#chkAutoplay").attr("checked", false);  
                	      }
                	      // Restore init values
                	      props.url = "";
                	      props.width = 507;
                	      props.height = 322;
                	      props.autoplay = "false";
                	      props.contentType = "";
                	      // Clear loop
            	          clearInterval(check);
            	        }
            	      }
            	    }, 50);
                  },
                  onOk : function() {
                	  
                	  
                	  var editor = this.getParentEditor();
                	  
					  var theIframe = $("iframe#iframeMediaEmbed");
              	        var contents = theIframe.contents();
					    var url = contents.find("#txtUrl").val();
						if(url != "") {
							  var content = "${include:media-player url=["+url+"]";			    
							  var contentType = contents.find("#txtContentType").val();
							  if(contentType.length > 0) {
							    content = content + " content-type=["+contentType+"]";
							  }
							  var width = contents.find("#txtWidth").val();
							  if(width.length > 0) {
							    content = content + " width=["+width+"]";
							  }
							  var height = contents.find("#txtHeight").val();
							  if(height.length > 0) {
							    content = content + " height=["+height+"]";
						      }
							  var autoplay = contents.find("#chkAutoplay");
							  if(autoplay.attr("checked") == true) {
							    content = content + " autoplay=[true]";
							  }
							  var align = contents.find("#txtAlign").val();
				
							  if(content.length>0) {
							    content = content + "}";
							  }			
							
							  var divClassType = '';
							  if(contentType.length > 0 && contentType == "audio/mp3") {  
							    divClassType = 'vrtx-media-player-audio';
							  } else if (getExtension(url) == "mp3") {
							    divClassType = 'vrtx-media-player-audio';
							  } else {
							    divClassType ='vrtx-media-player';
							  }
							  						  
							// Existing
	                    	if(editor.getSelection().getStartElement()) {
	                    	  selected = editor.getSelection().getStartElement();
	                    	  selected.removeAttribute("class");
	                    	  if(align != "" && divClassType != "") {
	                    	    selected.addClass(divClassType);
	                    	    selected.addClass(align);
	                    	  } else {
	                    	    selected.addClass(divClassType); 
	                    	  }
	  						  editor.getSelection().getStartElement().setHtml(content);
	                      	} else { // New
	                      	  var divClasses = "";
	                      	  if(align != "" && divClassType != "") {
	                            divClasses = divClassType+' '+align;
	                          } else {
	                          	divClasses = divClassType;	  
	                          }
	                      	  editor.insertHtml('<div class="'+divClasses+'">'+content+'</div>');	
	                      	}
							  
							} else {
							  alert("Du må spesifisere en URL");
							  return false;	
							}
                 }
              };
           } );

            editor.addCommand( 'MediaEmbed', new CKEDITOR.dialogCommand( 'MediaEmbedDialog' ) );

            editor.ui.addButton( 'MediaEmbed',
            {
                label: 'Embed Media',
                command: 'MediaEmbed',
                icon: this.path.toLowerCase() + 'images/icon.gif'
            } );
            
            editor.on( 'doubleclick', function( evt ){
            	        var data = evt.data;
        				var element = data.element;

        				var HTML = element.$.innerHTML;
        				if(HTML.indexOf("include:media-player") == -1) {
        				  return null;
        				}
        				
        				extractMediaPlayerProps(HTML);
        				data.dialog = 'MediaEmbedDialog';
        			});
            
            
            if (editor.addMenuItem) {    
            	  // A group menu is required
            	  // order, as second parameter, is not required
            	  editor.addMenuGroup('MediaEmbed');
            	 
            	  // Create a menu item
            	  editor.addMenuItem('MediaEmbedDialog', {
            	    label: 'Mediaegenskaper',
            	    command: 'MediaEmbed',
            	    group: 'MediaEmbed',
            	    icon: this.path.toLowerCase() + 'images/icon.gif'
            	  });
            	}
            	  
            	if (editor.contextMenu) {
            	  editor.contextMenu.addListener(function(element, selection) {
            		var HTML = element.$.innerHTML;
            		if(HTML.indexOf("include:media-player") == -1) {
            		  return null;	
            		}

					extractMediaPlayerProps(HTML);
					return {
						MediaEmbedDialog: CKEDITOR.TRISTATE_ON
					};
                });
              }
    }
    });
} )();

/** Get the file extension  */
function getExtension(url) {
    var ext = url.match(/\.(avi|asf|fla|flv|mov|mp3|mp4|m4v|mpg|mpeg|mpv|qt|swf|wma|wmv)$/i);
    if (ext != null && ext.length && ext.length > 0) {
	ext = ext[1];
    } else {
	if (url.contains('youtube.com/')) {
	    ext = 'swf';
	} else {
	    ext = '';
	}
    }
    return ext;
}

function extractMediaPlayerProps(HTML) {
    var regexp = [];
    var HTMLOrig = HTML;
  		
    for(var name in props) {
      if(name != "contentType") {
        regexp = new RegExp('(?:' + name + '=\\[)(.*?)(?=\\])'); // non-capturing group for prop=. TODO: positive lookbehind (non-capturing)
      } else {
    	regexp = new RegExp('(?:content\\-type=\\[)(.*?)(?=\\])'); // non-capturing group for prop=. TODO: positive lookbehind (non-capturing)
      }
      
  	  var prop = regexp.exec(HTML);
  	  if(prop != null) {
 		if(prop.length = 2) {
 		  props[name] = prop[1]; // get the capturing group 
 		}
  	  }
  	  HTML = HTMLOrig; //TODO: is it possible to avoid this?
    }
 }