// Vortex Simple Gallery jQuery plugin v0.1b
// w/ paging, centered thumbnail navigation and fade effect
// by Øyvind Hatland - UiO / USIT

(function ($) {
  $.fn.vrtxSGallery = function (wrapper, container, options) {
	  
	  //cache
	  var images = new Array();
	  
	  //animation settings
	  settings = jQuery.extend({
		fadeInOutTime : 250,
		fadedOutOpacity: 0
	  }, options);

	  //Unobtrusive JavaScript
	  $(container + "-pure-css").addClass(container.substring(1));
	  $(container + "-nav-pure-css").addClass(container.substring(1) + "-nav");
		  
	  //paging (relative to li a.active)
	  addPagingClickEvent("next", wrapper);
	  addPagingClickEvent("prev", wrapper);  
	  
	  function addPagingClickEvent(navClass, wrapper) {
	    $(wrapper + " " + " a." + navClass).click(function(h) {
	      if(navClass == "next") {
			  if($(wrapper + " ul li a.active").parent().next().length != 0) {
			    $(wrapper + " ul li a.active").parent().next().find("a").click();
			  } else {
				$(wrapper + " ul li:first a").click();
			  }
	      } else {
	    	  if($(wrapper + " ul li a.active").parent().prev().length != 0) {
	    		$(wrapper + " ul li a.active").parent().prev().find("a").click();
	          } else {
	    		$(wrapper + " ul li:last a").click();   
	          }
	      }
		  h.preventDefault(); 
		});
	    
	    //Fading of transparent block and prev / next icon
	    $(wrapper + " " + " a." + navClass).stop().fadeTo("0", 0);
	    $(wrapper + " " + " a." + navClass + " span").stop().fadeTo("0", 0);
	    
	    $(wrapper + " " + " a." + navClass).hover(
	    function () {
		  $(this).stop().fadeTo("250", 1);
		  $("span", this).stop().fadeTo("250", 0.2);
		}, 
		function () {
		  $(this).stop().fadeTo("250", 0);
		  $("span", this).stop().fadeTo("250", 0);
		});
	  }
	  
	  initFirstImage();
		  
	  return this.each(function (i) {
		  
		   //center thumbnails images
		  
		   //.. first horizontal
		   var imgWidth = $("img", this).width();
		   if(imgWidth > $(this).width()) {
		      var leftAdjust = -(imgWidth - $(this).width()) / 2;
			  $("img", this).css("marginLeft", leftAdjust + "px");
		   }
		   
		   //.. then vertical
		   var imgHeight = $("img", this).height();
		   if(imgHeight > $(this).height()) {
			  var topAdjust = -(imgHeight - $(this).height()) / 2;
			  $("img", this).css("marginTop", leftAdjust + "px"); 
		   }
		   
		   //generate image
		   var img = new Image();
		   var src = $("img", this).attr("src").split("?")[0]; 
		   var alt = $("img", this).attr("alt");
		   img.src = src; img.alt = alt;
		   
	       //generate link
	       link = document.createElement("a"); 
	       link.setAttribute("href", $(this).attr("href"));
	       link.setAttribute("class", container.substring(1) + "-link");
	       // IE
	       link.setAttribute("className", container.substring(1) + "-link");
	      
	       //append img inside link
	       $(link).append(img);
	      
	       //cache
	       images[i] = link;
	       
	       //Fading of transparent block and prev / next icon
    	   $(this).hover(
		    function () {
		    	if(!$(this).hasClass("active")) {
				 $("img", this).stop().fadeTo("250", 1);
		    	}
		    }, 
		    function () {
		    	if(!$(this).hasClass("active")) {
				 $("img", this).stop().fadeTo("250", 0.6);
		    	}
	       });

		   $(this).click(function(e) {

		  	  //replace link and image (w/ fade effect down to fadedOutOpacity) + stop() current animation.
		      if(settings.fadeInOutTime > 0) {
			      $(wrapper + " " + container).stop().fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function() {
			    	  //done fade out -> remove
			    	  $("a" + container + "-link", this).remove();
			    	  //append image
			    	  $(this).append(images[i]); 
			    	  //fade in
			    	  $(this).fadeTo(settings.fadeInOutTime, 1); 
			      });
		      } else {
		    	  $("a" + container + "-link", wrapper + " " + container).remove();
		    	  $(wrapper + " " + container).append(images[i]);   
		      }

		      //remove active classes
		      jQuery(wrapper + " ul li a").each(function(j) {
		    	if(jQuery(this).hasClass("active")) {
		    	   jQuery(this).removeClass("active");
		    	   jQuery("img", this).stop().fadeTo("250", 0.6);
		    	} else {
		    	   jQuery("img", this).stop().fadeTo("0", 0.6);
		    	}
		      });
		      
		      var imgHeight = $(wrapper + " " + container + " img").height();
		      $(wrapper + " " + container + "-nav a").css("top", (imgHeight / 2) - 15);
			  $(wrapper + " " + container + "-nav span").css("height", imgHeight);
			  $(wrapper + " " + container + "-nav span").css("top", -(imgHeight / 2) + 20);
		     
		      //add new active class
		  	  $(this).addClass("active");
		  	  $("img", this).stop().fadeTo("0", 1);

		  	  //add description
		  	  $(wrapper + " " + container + "-description").remove();
		  	  $("<div class='" + container.substring(1) + "-description'>" 
		  			  + $(wrapper + " " + container + " img").attr("alt") + "</div>").insertAfter(wrapper + " " + container);
		  	  
		      //prevent default event action
			  e.preventDefault(); 
	      });
		   
		 
	 });
	  
	//TODO: refactor with above code
	function initFirstImage() {
	  //choose first image in <li>
	  $(wrapper + " ul li:first a").addClass("active");
		
	  //change image
	  var img = new Image();
	  var src = $(wrapper + " ul li:first a img").attr("src").split("?")[0]; 
	  var alt = $(wrapper + " ul li:first a img").attr("alt");
	  img.src = src; img.alt = alt;
	  
      //change link
      link = document.createElement("a"); 
      link.setAttribute("href", $(wrapper + " ul li:first a").attr("href"));
      link.setAttribute("class", container.substring(1) + "-link");
      // IE
      link.setAttribute("className", container.substring(1) + "-link");
      
      //append img inside link
      $(link).append(img);
      
      $("a" + container + "-link", wrapper + " " + container).remove();
      $(wrapper + " " + container).append(link);
      
      $("<div class='" + container.substring(1) + "-description'>" 
  			  + $(wrapper + " " + container + " img").attr("alt") + "</div>").insertAfter(wrapper + " " + container);
	  
      var imgHeight = $(wrapper + " " + container + " img").height();
	  $(wrapper + " " + container + "-nav a").css("top", (imgHeight / 2) - 15);
	  $(wrapper + " " + container + "-nav span").css("height", imgHeight);
	  $(wrapper + " " + container + "-nav span").css("top", -(imgHeight / 2) + 20);
	  
	  jQuery(wrapper + " ul li a").each(function(j) {
		if(jQuery(this).hasClass("active")) {
		} else {
		   jQuery("img", this).stop().fadeTo("0", 0.6);
		}
	  });
	}
  };
})(jQuery);