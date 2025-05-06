CREATE TABLE feuille1 (
    id INT PRIMARY KEY AUTO_INCREMENT,
    project_title VARCHAR(50),
    client_name VARCHAR(50)
) ENGINE=InnoDB;

CREATE TABLE feuille2 (
    id INT PRIMARY KEY AUTO_INCREMENT,
    project_title VARCHAR(50),
    task_title VARCHAR(50)
) ENGINE=InnoDB;

CREATE TABLE feuille3 (
    id INT PRIMARY KEY AUTO_INCREMENT,
    client_name VARCHAR(50),
    lead_title VARCHAR(50),
    type VARCHAR(50),
    produit VARCHAR(50),
    prix DECIMAL(15,2),
    quantite INT
) ENGINE=InnoDB;
