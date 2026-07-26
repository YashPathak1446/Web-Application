DELIMITER $$

DROP PROCEDURE IF EXISTS add_movie;

CREATE PROCEDURE add_movie(
    IN movie_title VARCHAR(100),
    IN movie_year INT,
    IN movie_director VARCHAR(100),
    IN star_name VARCHAR(100),
    IN star_birthYear INT,
    IN genre_name VARCHAR(32)
)
BEGIN
    DECLARE movie_id VARCHAR(10);
    DECLARE star_id VARCHAR(10);
    DECLARE genre_id INT;
    DECLARE max_movie_id VARCHAR(10);
    DECLARE max_star_id VARCHAR(10);
    DECLARE message VARCHAR(100);

    SELECT m.id INTO movie_id
    FROM movies AS m
    WHERE m.title = movie_title AND m.year = movie_year AND m.director = movie_director
        LIMIT 1;

    IF movie_id IS NOT NULL THEN
        SET message = CONCAT('ERROR: duplicate movie: ', movie_id);
    ELSE
        SELECT MAX(movies.id) INTO max_movie_id FROM movies;
        IF max_movie_id IS NULL THEN
            SET max_movie_id = 'tt0000001';
        END IF;

        SET movie_id = CONCAT('tt', LPAD(SUBSTRING(max_movie_id, 3) + 1, 7, '0'));
        INSERT INTO movies (id, title, year, director)
        VALUES (movie_id, movie_title, movie_year, movie_director);
        SET message = CONCAT("Success! movieId: ", movie_id);


        IF star_name IS NOT NULL THEN
            SELECT s.id INTO star_id FROM stars AS s WHERE s.name = star_name  and s.birthYear = star_birthYear LIMIT 1;
            IF star_id IS NULL THEN
                SELECT MAX(stars.id) INTO max_star_id FROM stars;

                IF max_star_id IS NULL THEN
                    SET max_star_id = 'nm0000000';
                END IF;

                SET star_id = CONCAT('nm', LPAD(CAST(SUBSTRING(max_star_id, 3) AS UNSIGNED) + 1, 7, '0'));
                INSERT INTO stars (id, name, birthYear)
                VALUES (star_id, star_name, star_birthYear);
            END IF;

            INSERT INTO stars_in_movies (starId, movieId)
            VALUES (star_id, movie_id)
            ON DUPLICATE KEY UPDATE starId = star_id, movieId = movie_id;
            SET message = CONCAT("Success! movieId: ", movie_id, ", starID: ", star_id);
        END IF;

        IF genre_name IS NOT NULL THEN
            SELECT genres.id INTO genre_id FROM genres WHERE name = genre_name LIMIT 1;

            IF genre_id IS NULL THEN
                INSERT INTO genres (name) VALUES (genre_name);
                SET genre_id = LAST_INSERT_ID();
            END IF;
            IF star_name IS NOT NULL THEN
                SET message = CONCAT("Success! movieId: ", movie_id, ", starID: ", star_id, ", genreId: ", genre_id);
            ELSE
                SET message = CONCAT("Success! movieId: ", movie_id, ", genreId: ", genre_id);
            END IF;

            INSERT INTO genres_in_movies (genreId, movieId)
            VALUES (genre_id, movie_id)
                ON DUPLICATE KEY UPDATE genreId = genre_id, movieId = movie_id;

        END IF;
    END IF;
    SELECT message AS message;
END $$

DELIMITER ;
