// /*
//  * This function is called by the library when it needs to lookup a query.
//  *
//  * The parameter query is the query string.
//  * The doneCallback is a callback function provided by the library, after you get the
//  *   suggestion list from AJAX, you need to call this function to let the library know.
//  */
// function handleLookup(query, doneCallback) {
//   console.log("autocomplete initiated")
//
//   // TODO: if you want to check past query results first, you can do it here
//
//   jQuery.ajax({
//     "method": "GET",
//     // generate the request url from the query.
//     // escape the query string to avoid errors caused by special characters
//     "url": "autocomplete?query=" + escape(query),
//     "success": function(data) {
//       handleLookupAjaxSuccess(data, query, doneCallback)
//     },
//     "error": function(errorData) {
//       console.log("lookup ajax error")
//       console.log(errorData)
//     }
//   })
// }
//
//
// /*
//  * This function is used to handle the ajax success callback function.
//  * It is called by our own code upon the success of the AJAX request
//  *
//  * data is the JSON data string you get from your Java Servlet
//  *
//  */
// function handleLookupAjaxSuccess(data, query, doneCallback) {
//   console.log("lookup ajax successful")
//
//   // parse the string into JSON
//   var jsonData = JSON.parse(data);
//   console.log(jsonData)
//
//   // TODO: if you want to cache the result into a global variable you can do it here
//
//   // call the callback function provided by the autocomplete library
//   // add "{suggestions: jsonData}" to satisfy the library response format according to
//   //   the "Response Format" section in documentation
//   doneCallback( { suggestions: jsonData } );
// }
//
//
// /*
//  * This function is the select suggestion handler function.
//  * When a suggestion is selected, this function is called by the library.
//  *
//  * You can redirect to the page you want using the suggestion data.
//  */
// function handleSelectSuggestion(suggestion) {
//   // TODO: jump to the specific result page based on the selected suggestion
//
//   console.log("you select " + suggestion["value"] + " with ID " + suggestion["data"]["heroID"])
// }
//
//
// /*
//  * This statement binds the autocomplete library with the input box element and
//  *   sets necessary parameters of the library.
//  *
//  * The library documentation can be find here:
//  *   https://github.com/devbridge/jQuery-Autocomplete
//  *   https://www.devbridge.com/sourcery/components/jquery-autocomplete/
//  *
//  */
// $('#autocomplete').autocomplete({
//   // documentation of the lookup function can be found under the "Custom lookup function" section
//   lookup: function (query, doneCallback) {
//     handleLookup(query, doneCallback)
//   },
//   onSelect: function(suggestion) {
//     handleSelectSuggestion(suggestion)
//   },
//   // set delay time
//   deferRequestBy: 300,
//   // there are some other parameters that you might want to use to satisfy all the requirements
//   // TODO: add other parameters, such as minimum characters
// });
//
// function handleNormalSearch(query) {
//   console.log("submit search form");
//   searchFormSubmitEvent.preventDefault();
//
//   // Serialize the search form into query parameters
//   const formData = new FormData(search_form[0]); // Use the DOM form element
//   const queryParams = new URLSearchParams(formData).toString();
//
//   window.location.href = `list.html?${queryParams}`;
// }
//
// // bind pressing enter key to a handler function
// $('#autocomplete').keypress(function(event) {
//   if (event.keyCode == 13) { // keyCode 13 is the enter key
//     handleNormalSearch($('#autocomplete').val())
//   }
// })
//
// search_form.submit(function(event) {
//     handleNormalSearch($('#autocomplete').val())
// })
