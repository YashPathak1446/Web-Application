document.getElementById("submit").addEventListener("click", function() {
  event.preventDefault();

  jQuery.ajax({
    cache: false,
    dataType: "json",
    method: "GET",
    url: "../api/dashboard",
    success: function(resultData) {
      console.log(resultData);
    },
    error: function(xhr, status, error) {
      alert("Error: please re-enter star information: ", error);
    }
  });
})
