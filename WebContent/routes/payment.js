// let logout_button = $("#logout_button");
//
// function handleLogoutResult(resultData){
//   console.log("handle logout response");
//   console.log(resultData);
//
//   // If logout succeeds, it will redirect the user to login.html
//   if (resultData.status === "success") {
//     sessionStorage.clear();
//     window.location.replace("../login.html");
//   } else {
//     console.error("Logout failed:", resultData);
//   }
// }
//
// /**
//  * Submit the form content with POST method
//  * @param logoutSubmitEvent
//  */
// function submitLogoutForm(logoutSubmitEvent){
//   console.log("submit logout event");
//   /**
//    * When users click the submit button, the browser will not direct
//    * users to the url defined in HTML form. Instead, it will call this
//    * event handler when the event is triggered.
//    */
//   logoutSubmitEvent.preventDefault();
//
//   $.ajax(
//     "../api/logout", {
//       method: "POST",
//       // Serialize the login form to the data sent by POST request
//       data: logout_button.serialize(),
//       success: handleLogoutResult,
//       error: function (error) {
//         console.error("Logout failed:", error);
//       }
//     });
// }
//
// logout_button.submit(submitLogoutForm);

document.getElementById("submit").addEventListener("click", function() {
  event.preventDefault();
  const first = $('#first').val();
  const last = $('#last').val();
  const number = $('#number').val();
  const expiration = $('#expiration').val();
  const cartData = sessionStorage.getItem("shoppingCart");
  console.log("cart data: " , cartData);

  jQuery.ajax({
    data: {
      first: first,
      last: last,
      number: number,
      expiration: expiration,
      shoppingCart: cartData
    },
    cache: false,
    dataType: "json",  // Setting return data type
    method: "POST",// Setting request method
    url: "../api/payment", // Setting request url, which is mapped by MoviesServlet in MoviesServlet.java
    success: function(resultData) {
      sessionStorage.setItem("saleData", JSON.stringify(resultData[1]));
      window.location.href='../frontend/confirmation.html';
      $("#paymentSubmit").attr('href', '../frontend/confirmation.html');
    },// Setting callback function to handle data returned successfully by the SingleStarServlet
    error: function(xhr, status, error) {
      console.error("Error status: ", status);
      console.error("Response text: ", xhr.responseText);
      alert("Error: please re-enter your credit card information");
    }
  });
})

window.addEventListener( "pageshow", function ( event ) {
  var historyTraversal = event.persisted ||
    ( typeof window.performance != "undefined" &&
      window.performance.navigation.type === 2 );
  if ( historyTraversal ) {
    // Handle page restore.
    window.location.reload();
  }
});
