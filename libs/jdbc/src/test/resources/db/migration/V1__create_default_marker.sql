CREATE TABLE jdbc_default_marker (
    id INT PRIMARY KEY,
    marker_value VARCHAR(100) NOT NULL
);

INSERT INTO jdbc_default_marker (id, marker_value) VALUES (1, 'default-marker');
