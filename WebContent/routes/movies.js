/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs three steps:
 *      1. Get parameter from request URL so it know which id to look for
 *      2. Use jQuery to talk to backend API to get the json data.
 *      3. Populate the data to correct html elements.
 */
let pageNumber = 1;
let moviesPerPage = 10;
let maxPages = 2;
let resultLength;
// let logout_button = $("#logout_button");

function fetchData() {
  const selectedSort = $('#sort').val();
  const pageLimit = $('#pageLength').val();
  moviesPerPage = pageLimit;

  console.log(pageNumber)

  jQuery.ajax({
    data: {
      sort: selectedSort,
      limit: pageLimit,
      page: pageNumber },
    dataType: "json",
    method: "GET",
    url: "../api/movies",
    success: (resultData) => handleMovieResult(resultData[0])
  });

  calculatePages();
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleMovieResult(resultData) {
  console.log("handleResult: populating movies info from resultData");
  console.log(resultData)

  // Populate the star table
  // Find the empty table body by id "movie_table_body"
  let movieTableBodyElement = jQuery("#movie_table_body");
  movieTableBodyElement.empty();
  movieTableBodyElement.innerHTML = "";

  // Concatenate the html tags with resultData jsonObject to create table rows
  for (let i = 0; i < Math.min(resultData.length); i++) {
    let rowHTML = "";
    rowHTML += "<tr>";
    // hyperlink single-movies
    rowHTML += "<th>" +
      '<a href = "../frontend/single-movie.html?id=' + resultData[i]['movie_id'] + '">'
      + resultData[i]["movie_title"] + '</a>' +
      "</th>";
    rowHTML += "<th>" + resultData[i]["movie_year"] + "</th>";
    rowHTML += "<th>" + resultData[i]["movie_director"] + "</th>";
    // rowHTML += "<th>" + resultData[i]["genre_name"] + "</th>";
    // Comment this code out. It will add hyperlink to the genres page.
    // Split the json object genre id's and genre names based on commas, and store as a list
    const genreArray = resultData[i]['genre_name'].split(", ");
    const genreId = resultData[i]['genre_id'].split(", ");
    rowHTML += "<th>";
    for (let i = 0; i < genreArray.length; i++) {
      rowHTML += '<a href =list.html?genre=' + genreId[i] + '>'
        + genreArray[i] + '</a>' + ", ";
    }
    // remove the ", " separate at the end.
    rowHTML = rowHTML.slice(0, rowHTML.length - 2);
    rowHTML += "</th>";
    // console.log(resultData[i]['star_name']);
    const starArray = resultData[i]['star_name'] != null ? resultData[i]['star_name'].split(",") : [];
    console.log(starArray)

    rowHTML += "<th>";
    if (starArray.length == 0) {
      rowHTML += '<div></div>';
    }
    for (let i = 0; i < starArray.length; i++) {
      const starInfo = starArray[i].split(":");
      const starId = starInfo[0];
      const starName = starInfo[1];

      rowHTML += '<a href = "../frontend/single-star.html?id=' + starId + '">'
        + starName + '</a>' + ", ";
    }
    // remove the ", " separate at the end.
    rowHTML = rowHTML.slice(0, rowHTML.length - 2);
    rowHTML += "</th>";
    rowHTML += "<th>" + resultData[i]["rating"] + "☆</th>";

    // Shoppping cart data display
    // Generate a random price if not provided by backend
    let moviePrice = resultData[i]["movie_price"]

    // Add a button to add to cart
    rowHTML += `<th>
                        <button class="add-to-cart" data-id="${resultData[i]['movie_id']}" 
                        data-title="${resultData[i]['movie_title']}" data-price="${moviePrice}" id = "cart_link">
                            Add to Cart
                        </button>
                    </th>`;
    rowHTML += "</tr>";
    movieTableBodyElement.append(rowHTML);
  }
  // Attach event listeners to the new "Add to Cart" button.
  $(".add-to-cart").click(function () {
    let movieId = $(this).data("id");
    let movieTitle = $(this).data("title");
    let moviePrice = $(this).data("price");
    addToCart(movieId, movieTitle, moviePrice);
  });
}

function calculatePages() {
  maxPages = Math.ceil(resultLength * 1.0 / moviesPerPage);
  console.log("max pages: ", maxPages)
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
/**
function submitLogoutForm(logoutSubmitEvent){
  console.log("submit logout event");
  
   * When users click the submit button, the browser will not direct
   * users to the url defined in HTML form. Instead, it will call this
   * event handler when the event is triggered.
   
  logoutSubmitEvent.preventDefault();
  console.log("make api call");

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

jQuery.ajax({
  dataType: "json",  // Setting return data type
  method: "GET",// Setting request method
  url: "../api/moviesCount", // Setting request url, which is mapped by MoviesServlet in MoviesServlet.java
  success: function(resultData) {
    resultLength = resultData.length;
    calculatePages();
  }  // Setting callback function to handle data returned successfully by the SingleStarServlet
});

/**
 * Handles adding a movie to the shopping cart in session storage.
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
  alert(movieTitle + " added to cart!");
}


document.addEventListener('DOMContentLoaded', function() {

  jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "../api/movies",
    success: function(resultData) {
      handleMovieResult(resultData[0]);
      if (resultData[1][0]) {
        $("#pageLength").val( resultData[1][0]["filter"]);
        $("#sort").val( resultData[1][0]["sort"]);
        //   pageNumber = resultData[1]["page"];
      }
    }
  });

  document.getElementById('pageLengthForm').addEventListener('submit', function (event) {
    event.preventDefault();
    fetchData();
  });

  const left = document.getElementById('prevPage');
  if (left) {
    left.addEventListener('click', function (event) {
      event.preventDefault();
      if (pageNumber > 1) {
        --pageNumber;
        fetchData();
      }
    });
  }

  const right = document.getElementById('nextPage')
  if (right) {
    right.addEventListener('click', function (event) {
      event.preventDefault();
      if (pageNumber < maxPages)
        ++pageNumber;
        console.log("page number: ", pageNumber);
        fetchData();
    });
  }
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
