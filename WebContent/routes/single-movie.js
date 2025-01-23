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

  let singleMovieTableBodyElement = jQuery("#movie_info");
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
        "<td>" + resultData[0]["rating"] + "</td>" +
    "</tr></tbody></table>");

  console.log("handleMovieResult: populating movie table from resultData");

  let movieGenresBodyElement = jQuery("#movie_genres");
  console.log(resultData[0]["genre_names"])
  let genres = resultData[0]["genre_names"].split(",");
  console.log(genres)
  for (let i = 0; i < genres.length; i++) {
    movieGenresBodyElement.append("<tr><td>" + genres[i] + "</td></tr>");
  }

  // Populate the movie table
  // Find the empty table body by id "movie_table_body"
  let movieStarsBodyElement = jQuery("#movie_stars");

  for (let i = 0; i < resultData.length; i++) {

    // Concatenate the html tags with resultData jsonObject
    let rowHTML = "";
    rowHTML += "<tr><td>" +
      // Add a link with id passed with GET url parameter
      '<a href="single-star.html?id=' + resultData[i]['star_id'] + '">'
      + resultData[i]["star_name"] +     // display star_name for the link text
      '</a>' +
      "</td></tr>";

    // Append the row created to the table body, which will refresh the page
    movieStarsBodyElement.append(rowHTML);
  }
}


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
  success: (resultData) => handleSingleMovieResult(resultData) // Setting callback function to handle data returned successfully by the StarsServlet
});
