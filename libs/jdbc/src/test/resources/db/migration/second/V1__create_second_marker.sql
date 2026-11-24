CREATE TABLE jdbc_second_marker (
    id INT PRIMARY KEY,
    marker_value VARCHAR(100) NOT NULL
);

INSERT INTO jdbc_second_marker (id, marker_value) VALUES (1, 'second-marker');
