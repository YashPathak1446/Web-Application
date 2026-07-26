let pageNumber = 1;
let moviesPerPage = 10;
let maxPages = 2;
let resultLength;
let selectedSort;
let pageLimit;
// let logout_button = $("#logout_button");

/**
 * Extract query parameters from the URL
 */
function getQueryParams() {
    const queryParams = new URLSearchParams(window.location.search);
    console.log("queryParams function: " + queryParams);
    return {
        search: queryParams.get("search") || "",
        // title: queryParams.get("title") || "",
        // year: queryParams.get("year") || "",
        // director: queryParams.get("director") || "",
        // star: queryParams.get("star") || "",
        genre: queryParams.get("genre") || "",
        prefix: queryParams.get("prefix") || "",
    };
}

/**
 * Populate the movie table with search results
 * @param resultData JSON data from the backend
 */
function handleSearchResults(resultData) {
    console.log("Populating movie table with search results:", resultData);

    const movieTableBody = $("#movie_table_body");
    movieTableBody.empty(); // Clear any existing rows
    for (let i = 0; i < Math.min(resultData.length); i++) {
        let rowHTML = "";
        rowHTML += "<tr>";
        // hyperlink single-movies
        rowHTML += "<th>" +
            '<a href = "single-movie.html?id=' + resultData[i]['movie_id'] + '">'
            + resultData[i]["movie_title"] + '</a>' +
            "</th>";
        rowHTML += "<th>" + resultData[i]["movie_year"] + "</th>";
        rowHTML += "<th>" + resultData[i]["movie_director"] + "</th>";

        // Comment this code out. It will add hyperlink to the genres page.
        // Split the json object genre id's and genre names based on commas, and store as a list
        if (resultData[i]['genre_name']) {
          const genreArray = resultData[i]['genre_name'].split(", ");
          const genreId = resultData[i]['genre_id'].split(", ");
          rowHTML += "<th>";
          for (let i = 0; i < genreArray.length; i++) {
            rowHTML += '<a href = "list.html?genre=' + genreId[i] + '">'
              + genreArray[i] + '</a>' + ", ";
          }
          // remove the ", " separate at the end.
          rowHTML = rowHTML.slice(0, rowHTML.length - 2);
          rowHTML += "</th>";
        }
        // rowHTML += "<th>" + resultData[i]["genre_name"] + "</th>";

        // Split the json object id's and star names based on commas and store as a list
        // const starArray = resultData[i]['star_name'].split(",");
        const starArray = resultData[i]['star_name'] != null ? resultData[i]['star_name'].split(",") : [];
        console.log(starArray)

        rowHTML += "<th>";
        if (starArray.length == 0) {
            rowHTML += '<div></div>';
        }
        for (let i = 0; i < starArray.length; i++) {
          const [starId, starName] = starArray[i].split(":")
            rowHTML += '<a href = "single-star.html?id=' + starId + '">'
                + starName+ '</a>' + ", ";
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
        movieTableBody.append(rowHTML);
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
  console.log("result length: ", resultLength)
  console.log("movies per page: ", moviesPerPage)
  maxPages = Math.ceil(resultLength * 1.0 / moviesPerPage);
  console.log("max pages: ", maxPages)
}

/**
 * Make an AJAX request to BrowseServlet with query parameters
 */
function fetchBrowseResults(queryParams) {
    // const queryParams = getQueryParams(); // Get all query parameters (including genre)
    $.ajax("../api/browse", {
        method: "GET",
        data: {
          sort: selectedSort,
          limit: pageLimit,
          page: pageNumber,
          ...queryParams}, // Send all parameters including genre
        dataType: "json",
        success: function(resultData) {
          console.log("receiving data: ", resultData);
          handleSearchResults(resultData[0]);
          if (resultData[1][0]) {
            $("#pageLength").val( resultData[1][0]["filter"]);
            $("#sort").val( resultData[1][0]["sort"]);
            //   pageNumber = resultData[1]["page"];
          }
        }, // Use the existing function to handle the result
        error: (xhr, status, error) => {
            console.error("Failed to fetch results:", status, error);
        }
    });
}

/**
 * Make an AJAX request to SearchServlet with query parameters
 */
function fetchSearchResults(queryParams) {
    // const queryParams = getQueryParams();
    $.ajax("../api/search", {
        method: "GET",
        data: {
          sort: selectedSort,
          limit: pageLimit,
          page: pageNumber,
          ...queryParams },
        dataType: "json",
        success: function(resultData) {
          handleSearchResults(resultData[0]);
          if (resultData[1][0]) {
            $("#pageLength").val(resultData[1][0]["filter"]);
            $("#sort").val(resultData[1][0]["sort"]);
            //   pageNumber = resultData[1]["page"];
          }
        },
        error: (xhr, status, error) => {
            console.error("Failed to fetch search results:", status, error);
        }
    });
}

/**
 * Fetch the correct results based on URL parameters
 */
function fetchResults() {
    const queryParams = getQueryParams();
    selectedSort = $('#sort').val();
    pageLimit = $('#pageLength').val();
    moviesPerPage = pageLimit;

    if (queryParams.genre || queryParams.prefix) {
      console.log("genre/prefix")
        // If genre parameter or prefix parameter is present, fetch movies by browse genre/prefix
        fetchBrowseResults(queryParams);
        jQuery.ajax({
          data: {
            genre: queryParams.genre,
            prefix: queryParams.prefix },
          dataType: "json",  // Setting return data type
          method: "GET",// Setting request method
          url: "../api/browseCount",
          success: function(resultData) {
            resultLength = resultData.length;
            calculatePages();
          }
        });
    }
    else {
      console.log("fetch search results")
        // Otherwise, fetch search results
        fetchSearchResults(queryParams);
      jQuery.ajax({
        data: {
        //   title: queryParams.title,
        //   year: queryParams.year,
        //   director: queryParams.director,
        //   star: queryParams.star
            search: queryParams.search
        },
        dataType: "json",  // Setting return data type
        method: "GET",// Setting request method
        url: "../api/searchCount",
        success: function(resultData) {
          resultLength = resultData.length;
          calculatePages();
        }
      });
    }
}

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

function onPageLoad() {
  const queryParams = getQueryParams();
  if (queryParams.genre || queryParams.prefix) {
    console.log("genre/prefix2");
    // If genre parameter or prefix parameter is present, fetch movies by browse genre/prefix
    jQuery.ajax({
      data: {
        genre: queryParams.genre,
        prefix: queryParams.prefix },
      method: "GET",
      dataType: "json",
      url:"../api/browse",
      success: function(resultData) {
        console.log("receiving data: ", resultData);
        handleSearchResults(resultData[0]);
        if (resultData[1][0]) {
          $("#pageLength").val( resultData[1][0]["filter"]);
          $("#sort").val( resultData[1][0]["sort"]);
          //   pageNumber = resultData[1]["page"];
        }
      }
    });
    jQuery.ajax({
      data: {
        genre: queryParams.genre,
        prefix: queryParams.prefix },
      dataType: "json",  // Setting return data type
      method: "GET",// Setting request method
      url: "../api/browseCount",
      success: function(resultData) {
        resultLength = resultData.length;
        calculatePages();
      }
    });
  }
  else {
    console.log("fetch search results")
    // Otherwise, fetch search results
    $.ajax("../api/search", {
      method: "GET",
      data: queryParams,
      dataType: "json",
      success: function(resultData) {
        handleSearchResults(resultData[0]);
        if (resultData[1][0]) {
          $("#pageLength").val(resultData[1][0]["filter"]);
          $("#sort").val(resultData[1][0]["sort"]);
          //   pageNumber = resultData[1]["page"];
        }
      },
      error: (xhr, status, error) => {
        console.error("Failed to fetch search results:", status, error);
      }
    });
    jQuery.ajax({
      data: {
      //   title: queryParams.title,
      //   year: queryParams.year,
      //   director: queryParams.director,
      //   star: queryParams.star
        search: queryParams.search
      },
      dataType: "json",  // Setting return data type
      method: "GET",// Setting request method
      url: "../api/searchCount",
      success: function(resultData) {
        resultLength = resultData.length;
        calculatePages();
      }
    });
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
/**function submitLogoutForm(logoutSubmitEvent){
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


logout_button.submit(submitLogoutForm); */

document.addEventListener('DOMContentLoaded', function() {
  onPageLoad();

  // Add a session event listener for every click
// in order to keep track of sessions and sort, pagination and limit:
//   document.body.addEventListener("click", function (event) {
//     const link_single_star = event.target.closest("a[href^='single-star.html']");
//     const link_single_movie = event.target.closest("a[href^='single-movie.html']");
//
//     // Check if either link is valid/similar. Then, check for the url, sort, page number, movie per page
//     if (link_single_star || link_single_movie) {
//       const currentUrl = window.location.href;  // Store current page URL
//       const selectedSort = document.getElementById('sort') ? document.getElementById('sort').value : '';  // Capture the current sort value
//       const pageNumber = 1; // Default to first page when navigating from single page
//       const moviesPerPage = document.getElementById('pageLength') ? document.getElementById('pageLength').value : '';  // Capture current movies per page setting
//
//       // Store in sessionStorage
//       sessionStorage.setItem("previousMovieListUrl", currentUrl);
//       sessionStorage.setItem("selectedSort", selectedSort);
//       sessionStorage.setItem("pageNumber", pageNumber);
//       sessionStorage.setItem("moviesPerPage", moviesPerPage);
//     }
//   });
//
//   document.getElementById('sortForm').addEventListener('submit', function (event) {
//     event.preventDefault();
//     fetchResults();
//   });

  document.getElementById('pageLengthForm').addEventListener('submit', function (event) {
    event.preventDefault();
    fetchResults();
  });

  const left = document.getElementById('prevPage');
  if (left) {
    left.addEventListener('click', function (event) {
      event.preventDefault();
      if (pageNumber > 1) {
        --pageNumber;
        fetchResults();
      }
    });
  }

  const right = document.getElementById('nextPage')
  if (right) {
    right.addEventListener('click', function (event) {
      event.preventDefault();
      if (pageNumber < maxPages) {
        ++pageNumber;
        console.log("page number: ", pageNumber);
        fetchResults();
      }
    });
  }
});

// Fetch results when the page loads
// $(document).ready(fetchResults);

window.addEventListener( "pageshow", function ( event ) {
  var historyTraversal = event.persisted ||
    ( typeof window.performance != "undefined" &&
      window.performance.navigation.type === 2 );
  if ( historyTraversal ) {
    // Handle page restore.
    window.location.reload();
  }
});

