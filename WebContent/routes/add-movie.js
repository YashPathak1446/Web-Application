document.getElementById("submit").addEventListener("click", function() {
  event.preventDefault();
  const title = $('#title').val();
  const year = $('#year').val();
  const director = $('#director').val();
  const star = $('#star').val();
  const genre = $('#genre').val();
  const birthYear = $('#birthYear').val();

  jQuery.ajax({
    data: {
      birthYear: birthYear,
      title: title,
      type: "movie",
      year: year,
      director: director,
      star: star,
      genre: genre,
    },
    cache: false,
    dataType: "json",
    method: "GET",
    url: "../api/add-data",
    success: function(resultData) {
      console.log("data: ", resultData);
      let movieMessageElement = $("#message");
      movieMessageElement.empty();
      console.log("message: ", resultData["message"]);
      movieMessageElement.append(resultData["message"]);
    },
    error: function(xhr, status, error) {
      alert("Error: please re-enter movie information: ", error);
    }
  });
})
