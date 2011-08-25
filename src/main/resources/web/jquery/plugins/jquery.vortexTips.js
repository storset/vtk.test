﻿/*
 *  vortexTips plugin
 *  
 *  Based loosely on tinyTips v1.1 by Mike Merritt (se license.txt)
 *  Modified by �yvind Hatland (USIT)
 *  
 *  Changes
 *  -------
 *  
 *  * Delegate mouseover/mouseleave to affect added nodes dynamically
 *  * Independent/multiple tips in different contexts (by appendTo)
 *  * Configure different speeds for fadeIn, fadeOutPreDelay and fadeOut
 *  * Changed positioning 'algorithm'
 *  * Caching
 *
 */
(function ($) {
  $.fn.vortexTips = function (subSelector, appendTo, containerWidth, animInSpeed, animOutPreDelay, animOutSpeed, xOffset, yOffset) {

    var html = "<span class='tip " + appendTo.substring(1) + "'>&nbsp;</span>";
    var tip;
    var tipText;
    var fadeOutTimer;

    $(this).delegate(subSelector, "mouseover mouseleave", function (e) {
      if (e.type == "mouseover") {
        clearTimeout(fadeOutTimer); // remove fadeOutTimer
        $(appendTo).append(html);
        tip = $(".tip." + appendTo.substring(1));
        tip.hide();
        var link = $(this);
        var classes = link.parent().attr("class") + " " + link.parent().parent().attr("class");
        if(typeof classes !== "undefined") {
          tip.addClass(classes);
        }
        var title = link.attr('title');
        tip.html(title);
        tipText = link.attr('title');
        link.attr('title', '');
        var pos = link.position();
        var nPos = pos;
        nPos.top = pos.top + yOffset;
        nPos.left = pos.left + link.width() + xOffset;
        tip.css('position', 'absolute').css('z-index', '1000').css('width', containerWidth + 'px');
        tip.css(nPos).fadeIn(animInSpeed);
      } else if (e.type == "mouseleave") {
        $(this).attr('title', tipText);
        fadeOutTimer = setTimeout(function () {
          tip.fadeOut(animOutSpeed, function () {
            $(this).remove();
          });
        }, animOutPreDelay);
      }
    });
  }
})(jQuery);