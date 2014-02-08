
var encrypt = function(event) {

  /* Disable our "submit" button */
  $('#submit').prop('disabled', true);

  /* Get our un-encrypted password */
  var unencrypted = $('#password').val();

  /* Clear out the values from the form */
  $('#password').val('');

  /* Start getting the key */
  $.get('key', function(data) {
    /* Got the public key, use it with RSA */
    var rsa = new RSAKey();
    rsa.setPublic(data.modulus, data.exponent);
    
    /* Calculate our encrypted password */
    var encrypted = rsa.encrypt(unencrypted);

    /* Post the encrypted password back */
    $.post('pass', { password: encrypted}, function(data) {
      /* Get our result */
      var result = data.result;
      
      /* If "complete" disable our form */
      if (result == 'complete') {
        $('.content').addClass('done');
        return; /* Don't re-enable the button */
      } else if (result != 'continue') {
        console.warn('Unexpected result from server', data);
      }

      /* Re-enable our button */
      $('#submit').prop('disabled', false);

    /* If something is wrong, re-enable our button */
    }, 'json').fail(function(event) {
      console.warn('Unable to send password', event);
      $('#submit').prop('disabled', false);
    })

    
  /* If something is wrong, re-enable our button */
  }, 'json').fail(function(event) {
    console.warn('Unable to get key', event);
    $('#submit').prop('disabled', false);
  });
}

$(document).ready(function() {

  /* Encrypt and send data on button click or ENTER */
  $('#submit').on('click', encrypt);
  $('#password').on('keypress',function(event) {
    if ((event.which == 13) && (!$('#submit').prop('disabled')))
      encrypt(event);
  });
});
