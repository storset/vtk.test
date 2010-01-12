jQuery( function($) {
  $('.vrtx-gallery').addClass('default_galleria_style');
  $('.nav').css('display', 'none');
  $('ul.default_galleria_style').galleria( {
    history :false,
    clickNext :false,
    insert :undefined,
    onImage : function() {
      $('.nav').css('display', 'block');
    }
  });
});

//View on black / white background

function toWhiteBG() {
  $('body .vrtx-image-gallery').css( { 'color':'#555', 'background-color': '#fff' } );
  $('#vrtx-display-on-white').css('color', '#555'); 
  $('#vrtx-display-on-black').css('color', '#334488');
}
function toBlackBG() {
  $('body .vrtx-image-gallery').css( { 'color': '#eee', 'background-color': '#000' } );
  $('#vrtx-display-on-black').css('color', '#eee'); 
  $('#vrtx-display-on-white').css( 'color', '#334488');
}