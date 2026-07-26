package main.java;

import java.util.HashSet;
import java.util.Set;

public class Movie {
    private String title;
    private String fid;
    private String movieId;
    private int year;
    private String director;
    private double price;
    private Set<String> genres;

    public Movie() {
        genres = new HashSet<>();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFID() { return fid; }
    public void setFID(String fid) { this.fid = fid; }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public Set<String> getGenres() { return genres; }
    public void addGenre(String genre) { genres.add(genre); }

    public String toString() {

        return "Title:" + getTitle() + ", " +
                "Director:" + getDirector() + ", " +
                "FID:" + getFID() + ", " +
                "Movie ID:" + getMovieId() + ", " +
                "Genre:" + getGenres() + ", " +
                "Year:" + getYear() + "." +
                "Price:" + getPrice();
    }
}
