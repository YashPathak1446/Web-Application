let cart = $("#cart");
// Search form parameters
let search_form = $("#search_form");
// logout button
let logout_button = $("#logout_button");
// title_links
let title_links = $("#title_links");
// genre_links
let genre_links =$("#genre_links");
let selectedIndex = -1;
let autocompleteResponse = {};
let autocompleteStartTime;
let autocompleteEndTime;


/**
 * Submit the form content with POST method
 * @param searchFormSubmitEvent
 */
function submitSearchForm(searchFormSubmitEvent) {
    // Handle search form submission
    console.log("submit search form");

    // Prevent default form submission
    searchFormSubmitEvent.preventDefault();

    // Serialize the search form into query parameters
    const formData = new FormData(search_form[0]); // Use the DOM form element
    const queryParams = new URLSearchParams(formData).toString();

    // Redirect to the list.html page with query parameters
    window.location.href = `list.html?${queryParams}`;
}

function handleLogoutResult(resultData){
    console.log("handle logout response");

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

/**
 * fetchTitleLinks function
 */
function fetchTitleLinks() {
    //const letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
    //     const numbers = "0123456789*".split("");
    //     let titlesHtml = "";
    //     letters.forEach((letter) => {
    //         titlesHtml += `<a href="list.html?prefix=${encodeURIComponent(letter)}" class="title_links">${letter}</a> `;
    //     });
    //     titlesHtml += '<br>';
    //     numbers.forEach((num) => {
    //         titlesHtml += `<a href="list.html?prefix=${encodeURIComponent(num)}" class="title_links">${num}</a> `;
    //     });
    const characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789*".split("");
    let titlesHtml = "";
    characters.forEach((char) => {
        titlesHtml += `<a href="list.html?prefix=${encodeURIComponent(char)}" class="title-link">${char}</a> `;
    });

    title_links.html(titlesHtml);
}

// Function to fetch all genres from the backend and display them
function fetchGenres(resultData) {

    // Build the genre links dynamically
    let genresHtml = "";

    resultData.forEach((genre) => {
        genresHtml += `<a href="list.html?genre=${encodeURIComponent(genre.id)}" class="genre_links">${genre.name}</a> `;
    });

    // Append the genre links to the #genre_links div
    $("#genre_links").html(genresHtml);

    // Store state when a genre is clicked
    document.querySelectorAll(".genre_links").forEach(link => {
        link.addEventListener("click", function (event) {
            sessionStorage.setItem("previousMovieListUrl", this.getAttribute("data-url"));
        });
    });
}

/**
 * Handle the data returned by MainServlet
 * @param resultDataString jsonObject, consists of session info
 */
function handleSessionData(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);

    // show the session information
    $("#sessionID").text("Session ID: " + resultDataJson["sessionID"]);
    $("#lastAccessTime").text("Last access time: " + resultDataJson["lastAccessTime"]);

    // show cart information
    handleCartArray(resultDataJson["previousItems"]);
}

/**
 * Handle the items in item list
 * @param resultArray jsonObject, needs to be parsed to html
 */
function handleCartArray(resultArray) {
    let item_list = $("#item_list");
    let res = "<ul>";
    for (let i = 0; i < resultArray.length; i++) {
        res += "<li>" + resultArray[i] + "</li>";
    }
    res += "</ul>";

    // clear the old array and show the new array in the frontend
    item_list.html("");
    item_list.append(res);
}

/**
 * Submit form content with POST method
 * @param cartEvent
 */
function handleCartInfo(cartEvent) {
    console.log("submit cart form");
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    cartEvent.preventDefault();

    $.ajax("../api/main", {
        method: "POST",
        data: cart.serialize(),
        success: resultDataString => {
            let resultDataJson = JSON.parse(resultDataString);
            handleCartArray(resultDataJson["previousItems"]);
        }
    });

    // clear input form
    cart[0].reset();
}

// Bind the submit action of the search to a handler function
search_form.submit(submitSearchForm);

// Bind the submit action of the logout to a handler function
logout_button.submit(submitLogoutForm);

// Get response for main/session data handling
$.ajax("../api/main", {
    method: "GET",
    success: handleSessionData
});

// Makes the HTTP GET request and registers on success callback function fetchGenres
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "../api/genres", // Setting request url, which is mapped by GenresServlet in GenresServlet.java
    success: (resultData) => fetchGenres(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
});

// Makes the HTTP GET request and registers on successful callback fetchTitleLinks
$(document).ready(() => {
    console.log("Document ready! Calling fetchTitleLinks...");
    fetchTitleLinks(); // Populate the A-Z, 0-9, and * links
});

// Bind the submit action of the form to a event handler function
cart.submit(handleCartInfo);

window.addEventListener( "pageshow", function ( event ) {
  var historyTraversal = event.persisted ||
    ( typeof window.performance != "undefined" &&
      window.performance.navigation.type === 2 );
  if ( historyTraversal ) {
    // Handle page restore.
    window.location.reload();
  }
});
























/*
 * This function is called by the library when it needs to lookup a query.
 *
 * The parameter query is the query string.
 * The doneCallback is a callback function provided by the library, after you get the
 *   suggestion list from AJAX, you need to call this function to let the library know.
 */
function handleLookup(query, doneCallback) {
  // TODO: if you want to check past query results first, you can do it here

  jQuery.ajax({
    "method": "GET",
    // generate the request url from the query.
    // escape the query string to avoid errors caused by special characters
    "url": "../api/autocomplete?query=" + escape(query),
    "success": function(data) {
      handleLookupAjaxSuccess(JSON.parse(data), query, doneCallback)
    },
    "error": function(errorData) {
      console.log("lookup ajax error: ", errorData);
    }
  })
}

function handleLookupAjaxSuccess(data, query, doneCallback) {
  var jsonData = data; // JSON.parse(data);
  // TODO: if you want to cache the result into a global variable you can do it here
  autocompleteResponse = jsonData
  doneCallback( { suggestions: jsonData } );

  // add data to dropdown
  $('#dropdown').empty();
  selectedIndex = -1;
  if (jsonData && jsonData.length > 0) {
    jsonData.forEach(function (movie) {
      const movieTitle = movie["data"]["movie_title"]
      const movieId = movie["data"]["movie_id"]
      $('#dropdown').append('<div><a href="../frontend/single-movie.html?id=' + movieId + '">' + movieTitle + '</a></div>');
    });
    $('#dropdown').show();
    console.log("Used suggestion list: ", autocompleteResponse)
  } else {
    $('#dropdown').hide();
  }
}

/*
 * This function is the select suggestion handler function.
 * When a suggestion is selected, this function is called by the library.
 */
function handleSelectSuggestion(suggestion) {
  window.location.href = "../frontend/single-movie.html?id=" + suggestion["data"]["movie_id"];
}

$('#autocomplete').on('keydown', function(event) {
  const dropdown = $('#dropdown');
  if (!dropdown.is(':visible')) return; // Only handle when dropdown is visible
  const items = dropdown.find('div');

  if (event.keyCode === 38 ) {
    event.preventDefault();
    event.stopPropagation();
    if (selectedIndex > 0) {
      selectedIndex--;
      updateHighlightedItem(items);
    }
  }
  else if (event.keyCode === 40) {
    event.preventDefault();
    event.stopPropagation();
    if (selectedIndex < items.length - 1) {
      selectedIndex++;
      updateHighlightedItem(items);
    }
  }
});

$('#autocomplete').autocomplete({
    // documentation of the lookup function can be found under the "Custom lookup function" section
    lookup: function (query, doneCallback) {
      let autocompleteQueries = JSON.parse(sessionStorage.getItem("autocompleteQueries"))

      // if in cache, use cache. otherwise, query backend
      if (autocompleteQueries == null)
        autocompleteQueries = {}
      if (!(query in autocompleteQueries)) {
        console.log("Autocomplete sending AJAX request to server")
        handleLookup(query, function(responseData) {
          autocompleteQueries[query] = autocompleteResponse;
          sessionStorage.setItem("autocompleteQueries", JSON.stringify(autocompleteQueries))
          doneCallback(responseData);
        });
      }
      else {
        console.log("Autocomplete using cached results")
        handleLookupAjaxSuccess(autocompleteQueries[query], query, doneCallback)
      }
    },
    onSearchStart: function() {
      autocompleteStartTime = Date.now();
      console.log("Autocomplete search initiated")
    },
    onSearchComplete: function() {
      autocompleteEndTime = Date.now()
      console.log("Total autocomplete search time: ", autocompleteEndTime - autocompleteStartTime)
    },
    onSelect: function (suggestion) {
      handleSelectSuggestion(suggestion)
    },
    deferRequestBy: 300,
    minChars: 3,
    appendTo: '<div style="display:none"></div>',
    preventBadQueries: false,
    triggerSelectOnValidInput: false,
});

$(document).on('keydown.customNav', function(event) {
  const dropdown = $('#dropdown');
  if (!dropdown.is(':visible')) return; // Only handle when dropdown is visible

  const items = dropdown.find('div');

  if (event.keyCode === 40) { // Down arrow
    event.preventDefault();
    event.stopPropagation(); // Stop event from reaching autocomplete
    if (selectedIndex < items.length - 1) {
      selectedIndex++;
      updateHighlightedItem(items);
    }
  }
  else if (event.keyCode === 38) { // Up arrow
    event.preventDefault();
    event.stopPropagation();
    if (selectedIndex > 0) {
      selectedIndex--;
      updateHighlightedItem(items);
    }
  }
});

  function updateHighlightedItem(items) {
    items.removeClass('highlighted');
    if (selectedIndex >= 0) {
      items.eq(selectedIndex).addClass('highlighted');
    }
  }
