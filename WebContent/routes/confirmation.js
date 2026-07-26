let logout_button = $("#logout_button");

function loadConfirmationPage() {
  // Load sale data from session storage
  // sale data maps movieid to sale id
  let saleData = JSON.parse(sessionStorage.getItem("saleData")) || {};
  console.log(saleData);
  console.log(typeof(saleData));
  // also get the rest of the information from session storage under cart data
  let cartData = JSON.parse(sessionStorage.getItem("shoppingCart")) || {};
  console.log("cartData", cartData);
  let confirmationBody = $("#confirmation_body");
  let grandTotal = 0;

  // clear previous contents in case of reload
  confirmationBody.empty();
  // loopp through sale data and generate HTML table rows for each purchase
  for (const movieId in saleData) {
    console.log("movieId", movieId);
    let movieStringId = String(movieId);
    // Check if movie id in the cartData
    if (cartData[movieStringId]){
      console.log("inside the if statement");
      let movie = cartData[movieId];
      let saleId = saleData[movieId];
      console.log(movie);
      console.log(saleId);
      let total = (parseFloat(movie.price) * movie.quantity).toFixed(2);
      grandTotal += parseFloat(total);

      // Generate HTML for each movie in the cart
      let rowHTML = `
                <tr>
                    <td>${saleId}</td>
                    <td><a href="single-movie.html?id=${movieId}">${movie.title}</a></td>
                    <td>${movie.quantity}</td>
                    <td>$${movie.price}</td>
                    <td>$${total}</td>
                </tr>
            `;
      confirmationBody.append(rowHTML);
    }
    // Display the grand total of the purchases
    $("#grand_total").text(grandTotal.toFixed(2));
  }
}
function handleLogoutResult(resultData){
  console.log("handle logout response");
  console.log(resultData);

  // If logout succeeds, it will redirect the user to login.html
  if (resultData.status === "success") {
    sessionStorage.clear();
    window.location.replace("../login.html");
  } else {
    console.error("Logout failed:", resultData);
  }
}

/**
 * Submit the form content with POST method
 * @param logoutSubmitEvent
 */
function submitLogoutForm(logoutSubmitEvent){
  console.log("submit logout event");
  /**
   * When users click the submit button, the browser will not direct
   * users to the url defined in HTML form. Instead, it will call this
   * event handler when the event is triggered.
   */
  logoutSubmitEvent.preventDefault();

  $.ajax(
      "../api/logout", {
        method: "POST",
        // Serialize the login form to the data sent by POST request
        data: logout_button.serialize(),
        success: handleLogoutResult,
        error: function (error) {
          console.error("Logout failed:", error);
        }
      });
}

logout_button.submit(submitLogoutForm);
// function modifyQuantity(movieId, change) {
//   let cart = JSON.parse(sessionStorage.getItem("shoppingCart")) || {};
//   if (cart[movieId]) {
//     cart[movieId].quantity += change;
//     if (cart[movieId].quantity <= 0) {
//       delete cart[movieId];
//     }
//     sessionStorage.setItem("shoppingCart", JSON.stringify(cart));
//     loadCart();
//   }
// }

function handleLogoutResult(resultData){
  console.log("handle logout response");
  console.log(resultData);

  // If logout succeeds, it will redirect the user to login.html
  if (resultData.status === "success") {
    sessionStorage.clear();
    window.location.replace("../login.html");
  } else {
    console.error("Logout failed:", resultData);
  }
}

/**
 * Submit the form content with POST method
 * @param logoutSubmitEvent
 */
function submitLogoutForm(logoutSubmitEvent){
  console.log("submit logout event");
  /**
   * When users click the submit button, the browser will not direct
   * users to the url defined in HTML form. Instead, it will call this
   * event handler when the event is triggered.
   */
  logoutSubmitEvent.preventDefault();

  $.ajax(
    "../api/logout", {
      method: "POST",
      // Serialize the login form to the data sent by POST request
      data: logout_button.serialize(),
      success: handleLogoutResult,
      error: function (error) {
        console.error("Logout failed:", error);
      }
    });
}

logout_button.submit(submitLogoutForm);

$(document).ready(() => {
  loadConfirmationPage();
});

window.addEventListener( "pageshow", function ( event ) {
  var historyTraversal = event.persisted ||
    ( typeof window.performance != "undefined" &&
      window.performance.navigation.type === 2 );
  if ( historyTraversal ) {
    // Handle page restore.
    window.location.reload();
  }
});
