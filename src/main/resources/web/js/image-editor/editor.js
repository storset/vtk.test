/* 
 * Vortex HTML5 Canvas image editor
 *
 */

var img, canvas, ctx, rw, rh, origw, origh, 
    ratio = 1,
    restorePoints = [],
    keepAspectRatio = true;
    scaledBeforeCrop = false,
    hasCropBeenInitialized = false;

$(function () {
  var imageEditor = $("#vrtx-image-editor-wrapper");
  if('getContext' in document.createElement('canvas') && imageEditor.length) {

    imageEditor.addClass("canvas-supported");
    var $canvas = imageEditor.find("#vrtx-image-editor");
    canvas = $canvas[0];
    ctx = canvas.getContext('2d');

    img = new Image();
    var path = location.href;
    img.src = path.substring(0, path.indexOf("?"));
    img.onload = function () {
      rw = origw = img.width;
      rh = origh = img.height;
      ratio = origw / origh;
      canvas.setAttribute('width', rw);
      canvas.setAttribute('height', rh);
      canvas.width = rw;
      canvas.height = rh;
      displayDimensions(rw, rh);
      saveRestorePoint();
      ctx.drawImage(img, 0, 0);
      $canvas.resizable({
        aspectRatio: keepAspectRatio,
        grid: [1, 1],
        stop: function (event, ui) {
          var newWidth = Math.round(ui.size.width);
          var newHeight = Math.round(ui.size.height);
          scale(newWidth, newHeight);
        },
        resize: function (event, ui) {
          displayDimensions(Math.round(ui.size.width), Math.round(ui.size.height));
        }
      });
    }

    $("#app-content").delegate("#vrtx-image-crop", "click", function(e) {
      if (hasCropBeenInitialized) {
        rw = origw = theSelection.w;
        rh = origh = theSelection.h;
        ratio = origw / origh;
        updateDimensions(rw, rh);
        ctx.drawImage(img, theSelection.x, theSelection.y, theSelection.w, theSelection.h,
                                        0,              0, theSelection.w, theSelection.h);
        resetCropPlugin();
        $(this).val("Start beskjæring...");
        $("#vrtx-image-editor").resizable("enable");
        saveRestorePoint();
        renderRestorePoint();
        hasCropBeenInitialized = false;
      } else {
        if (scaledBeforeCrop) {
         saveRestorePoint();
         img.src = restorePoints[restorePoints.length - 1];
         img.onload = function () {
           ctx.drawImage(img, 0, 0);
           $(this).val("Beskjær bilde");
           $("#vrtx-image-editor").resizable("disable");
           init();
           scaledBeforeCrop = false;
         }
        } else {
          $(this).val("Beskjær bilde");
          $("#vrtx-image-editor").resizable("disable");
          init();
        }
        hasCropBeenInitialized = true;
      }
      e.stopPropagation();
      e.preventDefault();
    });

    $("#app-content").delegate("#resource-width, #resource-height", "change", function(e) {
      var w = parseInt($.trim($("#resource-width").val()));
      var h = parseInt($.trim($("#resource-height").val()));
      if(!w.isNaN && !h.isNaN) {
        if(w !== rw) {
          if(keepAspectRatio) {
            h = ratio <= 1 ? w * ratio : w / ratio;
            h = Math.round(h);
          }
          $("#resource-height").val(h)
        } else if(h !== rh) {
          if(keepAspectRatio) {
            w = ratio <= 1 ? h / ratio : h * ratio;
            w = Math.round(w);
          }
          $("#resource-width").val(w)
        }
        scale(w, h);
      }
    });

    $("#app-content").delegate("#resource-width", "keydown", function(e) {
      if (e.which == 38 || e.which == 40) {
        var w = parseInt($.trim($("#resource-width").val()));
        var h = parseInt($.trim($("#resource-height").val()));
        if(!w.isNaN && !h.isNaN) {
          if(e.which == 38) {
            w++;
          } else {
            if(w > 2) {
              w--;
            }
          }
          if(keepAspectRatio) {
            h = ratio <= 1 ? w * ratio : w / ratio;
            h = Math.round(h);
          }
          $("#resource-width").val(w);
          $("#resource-height").val(h);
          scale(w, h);
        }
      }
    });

    $("#app-content").delegate("#resource-height", "keydown", function(e) {
      if (e.which == 38 || e.which == 40) {
        var w = parseInt($.trim($("#resource-width").val()));
        var h = parseInt($.trim($("#resource-height").val()));
        if(!w.isNaN && !h.isNaN) {
          if(e.which == 38) {
            h++;
          } else {
            if(h > 2) {
              h--;
            }
          }
          if(keepAspectRatio) {
            w = ratio <= 1 ? h / ratio : h * ratio;
            w = Math.round(w);
          }
          $("#resource-width").val(w);
          $("#resource-height").val(h);
          scale(w, h);
        }
      }
    });

  }
});

function scale(newWidth, newHeight) {
  if(newWidth < origw) { // Downscaling with Lanczos3
    rw = newWidth;
    rh = newHeight;
    updateDimensions(rw, rh);
    $("#vrtx-image-editor-preview").addClass("loading");
    $("#vrtx-image-crop").attr("disabled", "disabled");
    new thumbnailer(canvas, ctx, img, rw, 3);
  } else { // Upscaling (I think with nearest neighbour 
           //            TODO: should be bicubic or bilinear)
    rw = newWidth;
    rh = newHeight;
    updateDimensions(rw, rh);
    ctx.drawImage(img, 0, 0, rw, rh); 
  }
  scaledBeforeCrop = true; 
}

function resetCropPlugin() {
  $("#vrtx-image-editor").unbind("mousemove").unbind("mousedown").unbind("mouseup");
  iMouseX, iMouseY = 1;
  theSelection;
}

function updateDimensions(w, h) {
  canvas.setAttribute('width', w);
  canvas.setAttribute('height', h);
  canvas.width = w;
  canvas.height = h;
  $(".ui-wrapper").css({
    "width": w,
    "height": h
  });
  $("#vrtx-image-editor").css({
    "width": w,
    "height": h
  });
  displayDimensions(w, h);
}

function displayDimensions(w, h) {
  if ($("#vrtx-image-dimensions-crop").length) {
    $("#resource-width").val(w);
    $("#resource-height").val(h);
  } else {
    var dimensionHtml = '<div id="vrtx-image-dimensions-crop">'
                      + '<div class="property-label">Bredde</div>'
                      + '<div class="vrtx-textfield"><input id="resource-width" type="text" value="' + w + '" size="6" /></div>'
                      + '<div class="property-label">Høyde</div>'
                      + '<div class="vrtx-textfield"><input id="resource-height" type="text" value="' + h + '" size="6" /></div>'
                      + '<div id="vrtx-image-crop-button"><div class="vrtx-button">'
                      + '<input type="button" id="vrtx-image-crop" value="Start beskjæring..." /></div></div>'
                      + '</div>';  
    $(dimensionHtml).insertBefore("#vrtx-image-editor-preview");
  }
}

/* Save restore points 
 * Credits: http://hyankov.wordpress.com/2010/12/26/how-to-implement-html5-canvas-undo-function/
 * TODO: Undo/redo functionality
 */
function saveRestorePoint() {
  var imgSrc = canvas.toDataURL("image/png");
  restorePoints.push(imgSrc);
}

function renderRestorePoint() {
  img.src = restorePoints[restorePoints.length - 1];
  img.onload = function () {
    ctx.drawImage(img, 0, 0);
  }
}

function save() {
  var sImg = Canvas2Image.saveAsJPEG(canvas, true);
  sImg.id = "canvas-image";
  $("<p id='save-info'>Høyreklikk og velg \"Lagre bilde som...\"</p>").insertBefore(".ui-wrapper");
  $(".ui-wrapper").addClass("canvas-image");
  canvas.parentNode.replaceChild(sImg, canvas);
}

function reset() {
  var sImg = document.getElementById("canvas-image");
  $("#save-info").remove();
  $(".ui-wrapper").removeClass("canvas-image");
  sImg.parentNode.replaceChild(canvas, sImg);
}

/* Thumbnailer / Lanczos algorithm for downscaling
 * Credits: http://stackoverflow.com/questions/2303690/resizing-an-image-in-an-html5-canvas
 * TODO: optimize and use Web Workers if available
 * Modified slighly by USIT
 */

/* elem: Canvas element
 * ctx: Canvas 2D context 
 * img: Image element
 * sx: Scaled width
 * lobes: kernel radius (e.g 3)
 */
function thumbnailer(elem, ctx, img, sx, lobes) {
  this.canvas = elem;
  elem.width = img.width;
  elem.height = img.height;
  elem.style.display = "none";
  this.ctx = ctx;
  this.ctx.drawImage(img, 0, 0);
  this.img = img;
  this.src = this.ctx.getImageData(0, 0, img.width, img.height);
  this.dest = {
    width: sx,
    height: Math.round(img.height * sx / img.width),
  };
  this.dest.data = new Array(this.dest.width * this.dest.height * 3);
  this.lanczos = lanczosCreate(lobes);
  this.ratio = img.width / sx;
  this.rcp_ratio = 2 / this.ratio;
  this.range2 = Math.ceil(this.ratio * lobes / 2);
  this.cacheLanc = {};
  this.center = {};
  this.icenter = {};
  setTimeout(this.process1, 0, this, 0);
}

thumbnailer.prototype.process1 = function (self, u) {
  self.center.x = (u + 0.5) * self.ratio;
  self.icenter.x = Math.floor(self.center.x);
  for (var v = 0; v < self.dest.height; v++) {
    self.center.y = (v + 0.5) * self.ratio;
    self.icenter.y = Math.floor(self.center.y);
    var a, r, g, b;
    a = r = g = b = 0;
    for (var i = self.icenter.x - self.range2; i <= self.icenter.x + self.range2; i++) {
      if (i < 0 || i >= self.src.width) continue;
      var f_x = Math.floor(1000 * Math.abs(i - self.center.x));
      if (!self.cacheLanc[f_x]) self.cacheLanc[f_x] = {};
      for (var j = self.icenter.y - self.range2; j <= self.icenter.y + self.range2; j++) {
        if (j < 0 || j >= self.src.height) continue;
        var f_y = Math.floor(1000 * Math.abs(j - self.center.y));
        if (self.cacheLanc[f_x][f_y] == undefined) self.cacheLanc[f_x][f_y] = self.lanczos(Math.sqrt(Math.pow(f_x * self.rcp_ratio, 2) + Math.pow(f_y * self.rcp_ratio, 2)) / 1000);
        weight = self.cacheLanc[f_x][f_y];
        if (weight > 0) {
          var idx = (j * self.src.width + i) * 4;
          a += weight;
          r += weight * self.src.data[idx];
          g += weight * self.src.data[idx + 1];
          b += weight * self.src.data[idx + 2];
        }
      }
    }
    var idx = (v * self.dest.width + u) * 3;
    self.dest.data[idx] = r / a;
    self.dest.data[idx + 1] = g / a;
    self.dest.data[idx + 2] = b / a;
  }

  if (++u < self.dest.width) setTimeout(self.process1, 0, self, u);
  else setTimeout(self.process2, 0, self);
};

thumbnailer.prototype.process2 = function (self) {
  self.canvas.width = self.dest.width;
  self.canvas.height = self.dest.height;
  self.ctx.drawImage(self.img, 0, 0);
  self.src = self.ctx.getImageData(0, 0, self.dest.width, self.dest.height);
  var idx, idx2;
  for (var i = 0; i < self.dest.width; i++) {
    for (var j = 0; j < self.dest.height; j++) {
      idx = (j * self.dest.width + i) * 3;
      idx2 = (j * self.dest.width + i) * 4;
      self.src.data[idx2] = self.dest.data[idx];
      self.src.data[idx2 + 1] = self.dest.data[idx + 1];
      self.src.data[idx2 + 2] = self.dest.data[idx + 2];
    }
  }
  self.ctx.putImageData(self.src, 0, 0);
  $("#vrtx-image-editor-preview").removeClass("loading");
  $("#vrtx-image-crop").removeAttr("disabled");
  self.canvas.style.display = "block";
}

// Returns a function that calculates lanczos weight
function lanczosCreate(lobes) {
  return function (x) {
    if (x > lobes) return 0;
    x *= Math.PI;
    if (Math.abs(x) < 1e-16) return 1
    var xx = x / lobes;
    return Math.sin(x) * Math.sin(xx) / x / xx;
  }
}

/*
 * Crop plugin
 * Credits: http://www.script-tutorials.com/demos/197/index.html
 * TODO: optimize
 * Modified slightly by USIT
 */

var iMouseX, iMouseY = 1;
var theSelection;

// Define Selection constructor
function Selection(x, y, w, h) {
  this.x = x; // initial positions
  this.y = y;
  this.w = w; // and size
  this.h = h;
  this.px = x; // extra variables to dragging calculations
  this.py = y;
  this.csize = 6; // resize cubes size
  this.csizeh = 10; // resize cubes size (on hover)
  this.bHow = [false, false, false, false]; // hover statuses
  this.iCSize = [this.csize, this.csize, this.csize, this.csize]; // resize cubes sizes
  this.bDrag = [false, false, false, false]; // drag statuses
  this.bDragAll = false; // drag whole selection
}

// Define Selection draw method
Selection.prototype.draw = function () {
  ctx.strokeStyle = '#000';
  ctx.lineWidth = 2;
  ctx.strokeRect(this.x, this.y, this.w, this.h);
  // draw part of original image
  if (this.w > 0 && this.h > 0) {
    ctx.drawImage(img, this.x, this.y, this.w, this.h, this.x, this.y, this.w, this.h);
  }
  // draw resize cubes
  ctx.fillStyle = '#fff';
  ctx.fillRect(this.x - this.iCSize[0], this.y - this.iCSize[0], this.iCSize[0] * 2, this.iCSize[0] * 2);
  ctx.fillRect(this.x + this.w - this.iCSize[1], this.y - this.iCSize[1], this.iCSize[1] * 2, this.iCSize[1] * 2);
  ctx.fillRect(this.x + this.w - this.iCSize[2], this.y + this.h - this.iCSize[2], this.iCSize[2] * 2, this.iCSize[2] * 2);
  ctx.fillRect(this.x - this.iCSize[3], this.y + this.h - this.iCSize[3], this.iCSize[3] * 2, this.iCSize[3] * 2);
}

function drawScene() { // Main drawScene function
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height); // clear canvas
  // draw source image
  ctx.drawImage(img, 0, 0, ctx.canvas.width, ctx.canvas.height);
  // and make it darker
  ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
  ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
  // draw selection
  theSelection.draw();
}

function init() {
  // create initial selection
  theSelection = new Selection(40, 40, $(canvas).width() - 40, $(canvas).height() - 40);
  $('#vrtx-image-editor').bind("mousemove", function (e) { // binding mouse move event
    var canvasOffset = $(canvas).offset();
    iMouseX = Math.floor(e.pageX - canvasOffset.left);
    iMouseY = Math.floor(e.pageY - canvasOffset.top);
    // in case of drag of whole selector
    if (theSelection.bDragAll) {
      theSelection.x = iMouseX - theSelection.px;
      theSelection.y = iMouseY - theSelection.py;
    }
    for (i = 0; i < 4; i++) {
      theSelection.bHow[i] = false;
      theSelection.iCSize[i] = theSelection.csize;
    }
    // hovering over resize cubes
    if (iMouseX > theSelection.x - theSelection.csizeh 
     && iMouseX < theSelection.x + theSelection.csizeh
     && iMouseY > theSelection.y - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.csizeh) {
      theSelection.bHow[0] = true;
      theSelection.iCSize[0] = theSelection.csizeh;
    }
    if (iMouseX > theSelection.x + theSelection.w - theSelection.csizeh 
     && iMouseX < theSelection.x + theSelection.w + theSelection.csizeh
     && iMouseY > theSelection.y - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.csizeh) {
      theSelection.bHow[1] = true;
      theSelection.iCSize[1] = theSelection.csizeh;
    }
    if (iMouseX > theSelection.x + theSelection.w - theSelection.csizeh
     && iMouseX < theSelection.x + theSelection.w + theSelection.csizeh
     && iMouseY > theSelection.y + theSelection.h - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.h + theSelection.csizeh) {
      theSelection.bHow[2] = true;
      theSelection.iCSize[2] = theSelection.csizeh;
    }
    if (iMouseX > theSelection.x - theSelection.csizeh
     && iMouseX < theSelection.x + theSelection.csizeh
     && iMouseY > theSelection.y + theSelection.h - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.h + theSelection.csizeh) {
      theSelection.bHow[3] = true;
      theSelection.iCSize[3] = theSelection.csizeh;
    }
    // in case of dragging of resize cubes
    var iFW, iFH;
    if (theSelection.bDrag[0]) {
      var iFX = iMouseX - theSelection.px;
      var iFY = iMouseY - theSelection.py;
      iFW = theSelection.w + theSelection.x - iFX;
      iFH = theSelection.h + theSelection.y - iFY;
    }
    if (theSelection.bDrag[1]) {
      var iFX = theSelection.x;
      var iFY = iMouseY - theSelection.py;
      iFW = iMouseX - theSelection.px - iFX;
      iFH = theSelection.h + theSelection.y - iFY;
    }
    if (theSelection.bDrag[2]) {
      var iFX = theSelection.x;
      var iFY = theSelection.y;
      iFW = iMouseX - theSelection.px - iFX;
      iFH = iMouseY - theSelection.py - iFY;
    }
    if (theSelection.bDrag[3]) {
      var iFX = iMouseX - theSelection.px;
      var iFY = theSelection.y;
      iFW = theSelection.w + theSelection.x - iFX;
      iFH = iMouseY - theSelection.py - iFY;
    }
    if (iFW > theSelection.csizeh * 2 && iFH > theSelection.csizeh * 2) {
      theSelection.w = iFW;
      theSelection.h = iFH;
      theSelection.x = iFX;
      theSelection.y = iFY;
    }
    drawScene();
  });
  $('#vrtx-image-editor').bind("mousedown", function (e) { // binding mousedown event
    var canvasOffset = $(canvas).offset();
    iMouseX = Math.floor(e.pageX - canvasOffset.left);
    iMouseY = Math.floor(e.pageY - canvasOffset.top);
    theSelection.px = iMouseX - theSelection.x;
    theSelection.py = iMouseY - theSelection.y;
    if (theSelection.bHow[0]) {
      theSelection.px = iMouseX - theSelection.x;
      theSelection.py = iMouseY - theSelection.y;
    }
    if (theSelection.bHow[1]) {
      theSelection.px = iMouseX - theSelection.x - theSelection.w;
      theSelection.py = iMouseY - theSelection.y;
    }
    if (theSelection.bHow[2]) {
      theSelection.px = iMouseX - theSelection.x - theSelection.w;
      theSelection.py = iMouseY - theSelection.y - theSelection.h;
    }
    if (theSelection.bHow[3]) {
      theSelection.px = iMouseX - theSelection.x;
      theSelection.py = iMouseY - theSelection.y - theSelection.h;
    }
    if (iMouseX > theSelection.x + theSelection.csizeh
     && iMouseX < theSelection.x + theSelection.w - theSelection.csizeh
     && iMouseY > theSelection.y + theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.h - theSelection.csizeh) {
      theSelection.bDragAll = true;
    }
    for (i = 0; i < 4; i++) {
      if (theSelection.bHow[i]) {
        theSelection.bDrag[i] = true;
      }
    }
  });
  $('#vrtx-image-editor').bind("mouseup", function (e) { // binding mouseup event
    theSelection.bDragAll = false;
    for (i = 0; i < 4; i++) {
      theSelection.bDrag[i] = false;
    }
    theSelection.px = 0;
    theSelection.py = 0;
  });
  drawScene();
}

/* Filters */

function bw() {
  var imgd = ctx.getImageData(0, 0, cw, ch);
  var pix = imgd.data;
  for (var i = 0, n = pix.length; i < n; i += 4) {
    var grayscale = pix[i] * .3 + pix[i + 1] * .59 + pix[i + 2] * .11;
    pix[i] = grayscale; // red
    pix[i + 1] = grayscale; // green
    pix[i + 2] = grayscale; // blue
  }
  ctx.putImageData(imgd, 0, 0);
  saveRestorePoint();
  renderRestorePoint();
}

function invert() {
  var imgd = ctx.getImageData(0, 0, cw, ch);
  var pix = imgd.data;
  for (var i = 0, n = pix.length; i < n; i += 4) {
    pix[i] = 255 - pix[i]; // green
    pix[i + 1] = 255 - pix[i + 1]; // green
    pix[i + 2] = 255 - pix[i + 2]; // blue
  }
  ctx.putImageData(imgd, 0, 0);
  saveRestorePoint();
  renderRestorePoint();
}

/* ^ Filters */

/* ^ Vortex HTML5 Canvas image editor */
