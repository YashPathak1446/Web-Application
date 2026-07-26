document.getElementById("submit").addEventListener("click", function() {
  event.preventDefault();
  const star = $('#star').val();
  const birthYear = $('#birthYear').val();
  console.log(star);
  console.log(birthYear);

  jQuery.ajax({
    data: {
      name: star,
      birthYear: birthYear,
      type: "star"
    },
    cache: false,
    dataType: "json",
    method: "GET",
    url: "../api/add-data",
    success: function(resultData) {
      console.log(resultData);
      let starMessageElement = jQuery("#message");
      starMessageElement.empty();
      console.log(resultData["message"]);
      starMessageElement.append(resultData["message"]);
    },
    error: function(xhr, status, error) {
      alert("Error: please re-enter star information: ", error);
    }
  });
})
