// let logout_button = $("#logout_button");

/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
  // Get request URL
  let url = window.location.href;
  // Encode target parameter name to url encoding
  target = target.replace(/[\[\]]/g, "\\$&");

  // Ues regular expression to find matched parameter value
  let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
    results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return '';

  // Return the decoded parameter value
  return decodeURIComponent(results[2].replace(/\+/g, " "));
}


/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleSingleMovieResult(resultData) {
  console.log("handleMovieResult: populating movie data from resultData");
  console.log("result data", resultData);

  let singleMovieTableBodyElement = jQuery("#movie_info");
  // add to cart functionality
  let movieId = getParameterByName('id');
  let movieTitle = resultData[0]["movie_title"];
  let moviePrice = resultData[0]["movie_price"];

  let addToCartButton = $("#header-add-to-cart");
  addToCartButton.data("id", movieId);
  addToCartButton.data("title", movieTitle);
  addToCartButton.data("price", moviePrice);
  addToCartButton.prop("disabled", false); // Enable button

  singleMovieTableBodyElement.append(
    "<h1 id='singleMovieTitle'>" + resultData[0]["movie_title"] + "</h1>" +
    "<table><thead><tr>" +
        "<th>Year</th>" +
        "<th>Director</th>" +
        "<th>Rating</th>" +
    "</tr></thead>" +
    "<tbody><tr>" +
        "<td>" + resultData[0]["movie_year"] + "</td>" +
        "<td>" + resultData[0]["movie_director"] + "</td>" +
        "<td>" + resultData[0]["rating"] + "</td>");

  console.log("handleMovieResult: populating movie table from resultData");

  let movieGenresBodyElement = jQuery("#movie_genres");
  console.log(resultData[0])

  let pastGenres = [];
  let genreList = resultData[0]['genres'].split(',');

  // let genres = resultData[0]["genre"].split(",");
  // console.log(genres)
  for (let i = 0; i < genreList.length; i++) {
    let genreInfo = genreList[i].split(":");
    let id = genreInfo[0];
    if (pastGenres.indexOf(id) === -1) {
      let genre = genreInfo[1]
      movieGenresBodyElement.append("<tr><td>" + '<a href="list.html?genre=' + id + '">' + genre + '</a>' + "</td></tr>");
      pastGenres.push(id);
    }
  }

  // Populate the movie table
  // Find the empty table body by id "movie_table_body"
  let movieStarsBodyElement = jQuery("#movie_stars");
  console.log(resultData)
  // let stars = resultData[0]['stars'].split(",");
  /// console.log(stars)
  let starInfo = resultData[0]['stars'].split(",");
  for (let i = 0; i < starInfo.length; i++) {
    let starData = starInfo[i].split(":");
    let id = starData[0]
    let name = starData[1]
    console.log("id: ", id)
    console.log("name: ", name)
    // Concatenate the html tags with resultData jsonObject
    let rowHTML = "";
    rowHTML += "<tr><td>" +
        // Add a link with id passed with GET url parameter
        '<a href="single-star.html?id=' + id + '">'
        + name +     // display star_name for the link text
        '</a>' +
        "</td></tr>";

    // Append the row created to the table body, which will refresh the page
    movieStarsBodyElement.append(rowHTML);
  }
}
$(document).on("click", "#header-add-to-cart", function () {
  let movieId = $(this).data("id");
  let movieTitle = $(this).data("title");
  let moviePrice = $(this).data("price");

  console.log("Add to Cart clicked:", movieId, movieTitle, moviePrice);

  if (!movieId || !movieTitle || !moviePrice) {
    console.error("Error: Missing movie data.");
    alert("Error: No movie selected.");
    return;
  }

  addToCart(movieId, movieTitle, moviePrice);
});

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
/**
function submitLogoutForm(logoutSubmitEvent){
  console.log("submit logout event");

   * When users click the submit button, the browser will not direct
   * users to the url defined in HTML form. Instead, it will call this
   * event handler when the event is triggered.
   
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

logout_button.submit(submitLogoutForm);*/

/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Get movieId from URL
let movieId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleStarResult
jQuery.ajax({
  dataType: "json",
  method: "GET",
  url: "../api/single-movie?id=" + movieId, // Setting request url, which is mapped by StarsServlet in Stars.java
  success: function(resultData) {
    handleSingleMovieResult(resultData[0]);
    if (resultData[1][0]) {
      console.log(resultData[1][0]["listPage"]);
      $("#backToMovies").attr('href', resultData[1][0]["listPage"]);
    }
  } // Setting callback function to handle data returned successfully by the StarsServlet
});

document.addEventListener("DOMContentLoaded", function () {
  const backButton = document.getElementById("backToMovies");
  const previousUrl = sessionStorage.getItem("previousMovieListUrl");

  console.log("Previous Movie List URL:", previousUrl); // Debugging

  if (previousUrl) {
    backButton.href = previousUrl;  // Set the back button URL
  } else {
    backButton.href = "list.html"; // Default fallback
  }
});


/**
 * Adds a movie to the shopping cart stored in sessionStorage.
 * If the movie is already in the cart, increases its quantity.
 *
 * @param {string} movieId - The unique movie identifier.
 * @param {string} movieTitle - The title of the movie.
 * @param {number|string} moviePrice - The price of the movie.
 */
function addToCart(movieId, movieTitle, moviePrice) {
  let cart = JSON.parse(sessionStorage.getItem("shoppingCart")) || {};

  if (cart[movieId]) {
    cart[movieId].quantity += 1;
  } else {
    cart[movieId] = {
      title: movieTitle,
      price: moviePrice,
      quantity: 1
    };
  }

  sessionStorage.setItem("shoppingCart", JSON.stringify(cart));
  console.log("Cart updated:", cart);
  alert(`${movieTitle} added to cart!`);
}

window.addEventListener( "pageshow", function ( event ) {
  var historyTraversal = event.persisted ||
    ( typeof window.performance != "undefined" &&
      window.performance.navigation.type === 2 );
  if ( historyTraversal ) {
    // Handle page restore.
    window.location.reload();
  }
});
