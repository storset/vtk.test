/*
 * Lanczos process2
 * Credits: http://stackoverflow.com/questions/2303690/resizing-an-image-in-an-html5-canvas
 *
 */

// Web Worker
self.addEventListener('message', function (e) {
  var data = e.data;
  if (data) {
    data = process2(data);
    self.postMessage(data);
  }
}, false);

function process2(data) {
  var idx, idx2;
  for (var i = 0; i < data.dest.width; i++) {
    for (var j = 0; j < data.dest.height; j++) {
      idx = (j * data.dest.width + i) * 3;
      idx2 = (j * data.dest.width + i) * 4;
      data.src.data[idx2] = data.dest.data[idx]; 
      data.src.data[idx2 + 1] = data.dest.data[idx + 1];
      data.src.data[idx2 + 2] = data.dest.data[idx + 2];
    }
  }
  return data;
}