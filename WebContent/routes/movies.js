// /**
//  * This example is following frontend and backend separation.
//  *
//  * Before this .js is loaded, the html skeleton is created.
//  *
//  * This .js performs three steps:
//  *      1. Get parameter from request URL so it know which id to look for
//  *      2. Use jQuery to talk to backend API to get the json data.
//  *      3. Populate the data to correct html elements.
//  */
//
//
//
// /**
//  * Handles the data returned by the API, read the jsonObject and populate data into html elements
//  * @param resultData jsonObject
//  */
//
// function handleMovieResult(resultData) {
//     console.log("handleResult: populating movies info from resultData");
//
//     // // populate the star info h3
//     // // find the empty h3 body by id "star_info"
//     // let moviesInfoElement = jQuery("#movies_info");
//     //
//     // // append two html <p> created to the h3 body, which will refresh the page
//     // moviesInfoElement.append("<p>Movie Name: " + resultData[0]["movie_title"] + "</p>" +
//     //     "<p>Director Name: " + resultData[0]["movie_director"] + "</p>");
//
//     console.log("handleResult: populating movie table from resultData");
//
//     // Populate the star table
//     // Find the empty table body by id "movie_table_body"
//     let movieTableBodyElement = jQuery("#movie_table_body");
//
//     // Concatenate the html tags with resultData jsonObject to create table rows
//     for (let i = 0; i < Math.min(resultData.length); i++) {
//         let rowHTML = "";
//         rowHTML += "<tr>";
//         // hyperlink single-movies
//         rowHTML += "<th>" +
//             '<a href = "single-movie.html?id=' + resultData[i]['movie_id'] + '">'
//             + resultData[i]["movie_title"] + '</a>' +
//             "</th>";
//         rowHTML += "<th>" + resultData[i]["movie_year"] + "</th>";
//         rowHTML += "<th>" + resultData[i]["movie_director"] + "</th>";
//         rowHTML += "<th>" + resultData[i]["genre_name"] + "</th>";
//         // Split the json object id's and star names based on commas and store as a list
//         const starArray = resultData[i]['star_name'].split(", ");
//         const starId = resultData[i]['star_id'].split(", ");
//         rowHTML += "<th>";
//         for (let i = 0; i < starArray.length; i++) {
//             rowHTML += '<a href = "single-star.html?id=' + starId[i] + '">'
//             + starArray[i]+ '</a>' + ", ";
//         }
//         // remove the ", " separate at the end.
//         rowHTML = rowHTML.slice(0, rowHTML.length - 2);
//         rowHTML += "</th>";
//         rowHTML += "<th>" + resultData[i]["rating"] + "☆</th>";
//         rowHTML += "</tr>";
//
//
//         // Append the row created to the table body, which will refresh the page
//         movieTableBodyElement.append(rowHTML);
//     }
// }
//
// /**
//  * Once this .js is loaded, following scripts will be executed by the browser\
//  */
//
// // Makes the HTTP GET request and registers on success callback function handleResult
// jQuery.ajax({
//     dataType: "json",  // Setting return data type
//     method: "GET",// Setting request method
//     url: "../api/movies", // Setting request url, which is mapped by MoviesServlet in MoviesServlet.java
//     success: (resultData) => handleMovieResult(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
// });
