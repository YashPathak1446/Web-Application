-- Create the database
CREATE DATABASE IF NOT EXISTS moviedb;

-- Use database
USE moviedb;


CREATE TABLE IF NOT EXISTS movies
(
    id varchar(10) PRIMARY KEY,
    title varchar(100) NOT NULL,
    year integer NOT NULL,
    director varchar(100) NOT NULL
);


CREATE TABLE IF NOT EXISTS stars
(
    id varchar(10) PRIMARY KEY,
    name varchar(100) NOT NULL,
    birthYear integer
);


CREATE TABLE IF NOT EXISTS stars_in_movies
(
    starId varchar(10) NOT NULL,
    movieId varchar(10) NOT NULL,
    PRIMARY KEY (starId, movieId),
    FOREIGN KEY (starId) REFERENCES stars(id),
    FOREIGN KEY (movieId) REFERENCES movies(id)
);


CREATE TABLE IF NOT EXISTS genres
(
    id integer PRIMARY KEY AUTO_INCREMENT,
    name varchar(32) NOT NULL
);


CREATE TABLE IF NOT EXISTS genres_in_movies
(
    genreId integer NOT NULL,
    movieId varchar(10) NOT NULL,
    PRIMARY KEY (genreId, movieId),
    FOREIGN KEY (genreId) REFERENCES genres(id),
    FOREIGN KEY (movieId) REFERENCES movies(id)
);

CREATE TABLE IF NOT EXISTS creditcards
(
    id varchar(20) PRIMARY KEY,
    firstName varchar(50) NOT NULL,
    lastName varchar(50) NOT NULL,
    expiration date NOT NULL
);


CREATE TABLE IF NOT EXISTS customers
(
    id integer PRIMARY KEY AUTO_INCREMENT,
    firstName varchar(50) NOT NULL,
    lastName varchar(50) NOT NULL,
    ccId varchar(20) NOT NULL,
    address varchar(200) NOT NULL,
    email varchar(50) NOT NULL,
    password varchar(20) NOT NULL,
    FOREIGN KEY (ccId) REFERENCES creditcards(id)
);


CREATE TABLE IF NOT EXISTS sales
(
    id integer PRIMARY KEY AUTO_INCREMENT,
    customerId integer NOT NULL,
    movieId varchar(10) NOT NULL,
    saleDate date NOT NULL,
    FOREIGN KEY (customerId) REFERENCES customers(id),
    FOREIGN KEY (movieId) REFERENCES movies(id)
);


CREATE TABLE IF NOT EXISTS ratings
(
    movieId varchar(10) PRIMARY KEY,
    rating float NOT NULL,
    numVotes integer NOT NULL,
    FOREIGN KEY (movieId) REFERENCES movies(id)
);
