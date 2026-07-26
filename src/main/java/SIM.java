package main.java;

public class SIM {
    private String fid;
    private String stageName;
    private String starId;
    private String movieId;
//    private String movieName;
//    private Integer movieYear;
//    private String movieDirector;

    public SIM() { }

    public String getFid() { return fid; }
    public void setFid(String fid) {  this.fid = fid; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public String getStarId() { return starId; }
    public void setStarId(String starId) { this.starId = starId; }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

//    public String getMovieName() { return movieName; }
//    public void setMovieName(String movieName) { this.movieName = movieName; }
//
//    public Integer getMovieYear() { return movieYear; }
//    public void setMovieYear(Integer movieYear) { this.movieYear = movieYear; }
//
//    public String getMovieDirector() { return movieDirector; }
//    public void setMovieDirector(String movieDirector) { this.movieDirector = movieDirector; }

    public String toString() {

        return "Name: " + getStageName() + ", " +
                "Star ID: " + getStarId() + ", " +
                "Movie ID: " + getMovieId();
    }
}
