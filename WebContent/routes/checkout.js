let logout_button = $("#logout_button");

// Called for Checkout Page
function loadCart() {
    let cart = JSON.parse(sessionStorage.getItem("shoppingCart")) || {};
    let cartBody = $("#cart_body");
    let cartTotal = 0;

    cartBody.empty();

    Object.keys(cart).forEach(movieId => {
        let movie = cart[movieId];
        let total = (movie.price * movie.quantity).toFixed(2);
        cartTotal += parseFloat(total);

        let rowHTML = `
            <tr>
                <td>${movie.title}</td>
                <td>$${movie.price}</td>
                <td>
                    <button class="decrease-qty" data-id="${movieId}">-</button>
                    ${movie.quantity}
                    <button class="increase-qty" data-id="${movieId}">+</button>
                </td>
                <td>$${total}</td>
                <td><button class="remove-item" data-id="${movieId}">Remove</button></td>
            </tr>
        `;
        cartBody.append(rowHTML);
    });

    $("#cart_total").text(cartTotal.toFixed(2));

    // Attach event listeners for modifying cart
    $(".increase-qty").click(function () {
        let movieId = $(this).data("id");
        modifyQuantity(movieId, 1);
    });

    $(".decrease-qty").click(function () {
        let movieId = $(this).data("id");
        modifyQuantity(movieId, -1);
    });

    $(".remove-item").click(function () {
        let movieId = $(this).data("id");
        removeFromCart(movieId);
    });
}

function modifyQuantity(movieId, change) {
    let cart = JSON.parse(sessionStorage.getItem("shoppingCart")) || {};
    if (cart[movieId]) {
        cart[movieId].quantity += change;
        if (cart[movieId].quantity <= 0) {
            delete cart[movieId];
        }
        sessionStorage.setItem("shoppingCart", JSON.stringify(cart));
        loadCart();
    }
}

function removeFromCart(movieId) {
    let cart = JSON.parse(sessionStorage.getItem("shoppingCart")) || {};
    delete cart[movieId];
    sessionStorage.setItem("shoppingCart", JSON.stringify(cart));
    loadCart();
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

$(document).ready(loadCart);

window.addEventListener( "pageshow", function ( event ) {
  var historyTraversal = event.persisted ||
    ( typeof window.performance != "undefined" &&
      window.performance.navigation.type === 2 );
  if ( historyTraversal ) {
    // Handle page restore.
    window.location.reload();
  }
});
