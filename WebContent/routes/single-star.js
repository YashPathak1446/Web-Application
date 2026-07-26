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
let logout_button = $("#logout_button");

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

function handleResult(resultData) {
  console.log("handleResult: populating star info from resultData");

  // populate the star info h3
  // find the empty h3 body by id "star_info"
  let starInfoElement = jQuery("#star_info");

  // append two html <p> created to the h3 body, which will refresh the page
  starInfoElement.append("<h1>" +  resultData[0]["star_name"] + "</h1>" + "<h3>Date of Birth: " +  resultData[0]["star_dob"] + "</h3>");

  console.log("handleResult: populating movie table from resultData");

  // Populate the star table
  // Find the empty table body by id "#single_star_table_body"
  let singleStarTableBodyElement = jQuery("#single_star_table_body");

  // Concatenate the html tags with resultData jsonObject to create table rows
  for (let i = 0; i < Math.min(10, resultData.length); i++) {
    let rowHTML = "<tr><th>" +
        '<a href="single-movie.html?id=' + resultData[i]['movie_id'] + '">' + resultData[i]["movie_title"] +
      "</a></th></tr>";

    // Append the row created to the table body, which will refresh the page
    singleStarTableBodyElement.append(rowHTML);
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

/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
let starId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "../api/single-star?id=" + starId, // Setting request url, which is mapped by StarsServlet in Stars.java
    success: function(resultData) {
      handleResult(resultData[0]) // Setting callback function to handle data returned successfully by the SingleStarServlet
      if (resultData[1][0]) {
        console.log(resultData[1][0]["listPage"]);
        $("#backToMovies").attr('href', resultData[1][0]["listPage"]);
      }
    }
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

document.addEventListener("DOMContentLoaded", function () {
    const movieLinks = document.querySelectorAll(".movie_link");

    movieLinks.forEach(link => {
        link.addEventListener("click", function (event) {
            const currentUrl = window.location.href;
            const selectedSort = document.getElementById('sort').value; // Get the current sort value
            const pageNumber = 1; // Default to first page
            const moviesPerPage = document.getElementById('pageLength').value; // Get the current page length

            // Store in sessionStorage
            sessionStorage.setItem("previousMovieListUrl", currentUrl);
            sessionStorage.setItem("selectedSort", selectedSort);
            sessionStorage.setItem("pageNumber", pageNumber);
            sessionStorage.setItem("moviesPerPage", moviesPerPage);
        });
    });
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
