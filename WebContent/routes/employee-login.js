let login_form = $("#login_form");

/**
 * Handle the data returned by LoginServlet
 * @param resultDataString jsonObject
 */
function handleLoginResult(resultDataString) {
  console.log("result data string: ", resultDataString);

  let resultDataJson;
  try {
    resultDataJson = typeof resultDataString === "string" ? JSON.parse(resultDataString) : resultDataString;
  } catch (e) {
    console.error("Error parsing JSON response:", e);
    $("#login_error_message").text("Server error: Invalid response format");
    return;
  }

  console.log("Parsed result data: ", resultDataJson);
  console.log("handle login response", resultDataJson["status"]);

  if (resultDataJson["status"] === "success") {
    window.location.replace("../frontend/dashboard.html");
  } else {
    console.log("show error message");
    console.log(resultDataJson["message"]);
    $("#login_error_message").text(resultDataJson["message"]);
  }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitLoginForm(formSubmitEvent) {
  console.log("submit employee login form");
  /**
   * When users click the submit button, the browser will not direct
   * users to the url defined in HTML form. Instead, it will call this
   * event handler when the event is triggered.
   */
  formSubmitEvent.preventDefault();
  console.log("employee login form submitted");

  // Clear any previous error messages
  $("#login_error_message").text("");

  $.ajax(
      "../_dashboard/api/employee-login", {
        // Serialize the login form to the data sent by POST request
        data: login_form.serialize(),
        method: "POST",
        headers: {
          "X-Requested-With": "XMLHttpRequest"
        },
        success: (resultData) => handleLoginResult(resultData),
        error: (xhr, status, error) => {
          console.error("AJAX error:", status, error);
          try {
            // Try to parse error response as JSON
            const errorData = JSON.parse(xhr.responseText);
            $("#login_error_message").text(errorData.message || "Server error occurred");
          } catch (e) {
            // If we can't parse it, show a generic error
            $("#login_error_message").text("Server error occurred. Please try again later.");
          }
        }
      }
  );
}

// Bind the submit action of the form to a handler function
login_form.submit(submitLoginForm);

window.addEventListener("pageshow", function(event) {
  var historyTraversal = event.persisted ||
      (typeof window.performance != "undefined" &&
          window.performance.navigation.type === 2);
  if (historyTraversal) {
    // Handle page restore.
    window.location.reload();
  }
});