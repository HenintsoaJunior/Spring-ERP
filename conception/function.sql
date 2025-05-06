---------------------------------------------------USERS--------------------------------------------
DROP PROCEDURE IF EXISTS insert_users;
DELIMITER $$
CREATE PROCEDURE insert_users(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE lang_code VARCHAR(3);
    DECLARE fixed_password VARCHAR(255) DEFAULT '$2y$10$NFXnR/Jr5AaYljEfpgLyQ.KrKhWawY./dEpwWcO7sVfYdo07iqNBu';
    
    SET @old_sql_mode = @@sql_mode;
    SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
    
    WHILE i < total DO
        SET lang_code = ELT(FLOOR(1 + RAND() * 5), 'EN', 'FR', 'ES', 'DE', 'IT');
        
        INSERT INTO users (
            external_id,
            name,
            email,
            password,
            address,
            primary_number,
            secondary_number,
            image_path,
            remember_token,
            language,
            created_at,
            updated_at
        )
        SELECT
            MD5(RAND()),
            CONCAT('User_', i + seq),
            CONCAT('user_', LEFT(UUID(), 8), '_', i + seq, '@example.com'),
            fixed_password, 
            CONCAT('Address_', i + seq),
            CONCAT('12345678', i + seq),
            CONCAT('87654321', i + seq),
            NULL,
            MD5(RAND()),
            lang_code,
            NOW(),
            NOW()
        FROM (
            SELECT n.seq
            FROM (
                SELECT 0 AS seq UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL
                SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL
                SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
            ) n
            WHERE n.seq < LEAST(batch_size, total - i)
        ) seq_table;
        
        SET i = i + (SELECT ROW_COUNT());
    END WHILE;
    
    SET @@sql_mode = @old_sql_mode;
END$$
DELIMITER ;

CALL insert_users(10);
-------------------------------------------------------departments----------------------------------------------------

DROP PROCEDURE IF EXISTS insert_departments;
DELIMITER $$

CREATE PROCEDURE insert_departments(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE rows_affected INT;
    
    -- Label for the WHILE loop
    main_loop: BEGIN
    
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        -- Create temporary table to track used department names
        DROP TEMPORARY TABLE IF EXISTS temp_used_dept_names;
        CREATE TEMPORARY TABLE temp_used_dept_names (
            name VARCHAR(255),
            PRIMARY KEY (name)
        );
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            INSERT INTO departments (
                external_id, 
                name, 
                description, 
                created_at, 
                updated_at
            )
            SELECT 
                UUID(),
                CONCAT('Departement_', i + seq),
                ELT(FLOOR(1 + RAND() * 5), 
                    'Administration', 
                    'Finance', 
                    'Ressources Humaines', 
                    'Informatique', 
                    'Logistique'),
                NOW(),
                NOW()
            FROM (
                SELECT n.seq 
                FROM (
                    SELECT 0 AS seq UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL 
                    SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL 
                    SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
                ) n
                WHERE n.seq < LEAST(batch_size, total - i)
            ) seq_table
            WHERE NOT EXISTS (
                SELECT 1 
                FROM temp_used_dept_names t 
                WHERE t.name = CONCAT('Departement_', i + seq_table.seq)
            )
            LIMIT 1; -- Limit to one per iteration for uniqueness control
            
            SET rows_affected = ROW_COUNT();
            
            IF rows_affected > 0 THEN
                -- Track the used name
                INSERT IGNORE INTO temp_used_dept_names (name)
                SELECT name 
                FROM departments 
                ORDER BY created_at DESC 
                LIMIT 1;
                
                SET i = i + rows_affected;
            END IF;
            
            -- If no new unique names can be inserted, exit the loop
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        -- Cleanup
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_dept_names;
    END main_loop;
END$$

DELIMITER ;

CALL insert_departments(10);
-------------------------------------------------------departments users----------------------------------------------------

DROP PROCEDURE IF EXISTS insert_department_user;
DELIMITER $$

CREATE PROCEDURE insert_department_user(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_users INT;
    DECLARE max_depts INT;
    DECLARE user_id INT;
    DECLARE dept_id INT;
    DECLARE rows_affected INT;
    
    -- Label for the WHILE loop
    main_loop: BEGIN
    
        -- Get counts of available records
        SELECT COUNT(*) INTO max_users FROM users;
        SELECT COUNT(*) INTO max_depts FROM departments;
        
        -- Check if required tables have data
        IF max_users = 0 OR max_depts = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Users or departments table is empty';
        END IF;
        
        -- Adjust total if it exceeds available unique users
        IF total > max_users THEN
            SET total = max_users;
        END IF;
        
        -- Create temporary table to track used user_ids within this execution
        DROP TEMPORARY TABLE IF EXISTS temp_used_users;
        CREATE TEMPORARY TABLE temp_used_users (
            user_id INT,
            PRIMARY KEY (user_id)
        );
        
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            -- Select a user_id that isn't in department_user or temp_used_users
            SELECT 
                u.id,
                d.id
            INTO 
                user_id,
                dept_id
            FROM users u
            LEFT JOIN department_user du ON du.user_id = u.id
            LEFT JOIN temp_used_users tuu ON tuu.user_id = u.id
            CROSS JOIN (
                SELECT id FROM departments ORDER BY RAND() LIMIT 1
            ) d
            WHERE du.user_id IS NULL  -- Exclude users already in department_user
            AND tuu.user_id IS NULL   -- Exclude users already used in this run
            ORDER BY RAND()
            LIMIT 1;
            
            -- If we found an unused user_id
            IF user_id IS NOT NULL THEN
                -- Insert into temp table first to track within this run
                INSERT IGNORE INTO temp_used_users (user_id)
                VALUES (user_id);
                
                SET rows_affected = ROW_COUNT();
                
                IF rows_affected > 0 THEN
                    -- Insert the department-user relationship
                    INSERT INTO department_user (
                        user_id,
                        department_id,
                        created_at,
                        updated_at
                    ) VALUES (
                        user_id,
                        dept_id,
                        NOW(),
                        NOW()
                    );
                    
                    SET i = i + 1;
                    SET user_id = NULL; -- Reset for next iteration
                END IF;
            END IF;
            
            -- If no new unique users are found, exit the loop
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        -- Cleanup
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_users;
    END main_loop;
END$$

DELIMITER ;

CALL insert_department_user(10);


---------------------------------------------------------------COMMENTS------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_comments;
DELIMITER $$

CREATE PROCEDURE insert_random_comments(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_users INT;
    DECLARE rand_user INT;
    DECLARE unique_id VARCHAR(250);
    
    SELECT COUNT(*) INTO max_users FROM users;
    
    SET @old_sql_mode = @@sql_mode;
    SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
    
    WHILE i < total DO
        SET rand_user = FLOOR(RAND() * max_users);
        
        SET unique_id = CONCAT(
            UNIX_TIMESTAMP(NOW()),
            '_',
            LPAD(i, 10, '0'),
            '_',
            FLOOR(RAND() * 10000)
        );
        
        INSERT IGNORE INTO comments (
            id,
            external_id,
            description,
            source_type,
            source_id,
            deleted_at,
            created_at,
            updated_at,
            user_id
        )
        SELECT 
            NULL,
            unique_id,
            CONCAT('Comment ', i + 1),
            'random',
            CONCAT('SRC_', FLOOR(RAND() * 1000)),
            NULL,
            NOW(),
            NOW(),
            u.id
        FROM 
            (SELECT id FROM users LIMIT 1 OFFSET rand_user) u
        WHERE NOT EXISTS (
            SELECT 1 
            FROM comments c 
            WHERE c.external_id = unique_id
        );
        
        IF ROW_COUNT() > 0 THEN
            SET i = i + 1;
        END IF;
    END WHILE;
    
    SET @@sql_mode = @old_sql_mode;
END$$

DELIMITER ;

CALL insert_random_comments(10);



------------------------------------------------------MAILS --------------------------------
DROP PROCEDURE IF EXISTS insert_random_mails;
DELIMITER $$

CREATE PROCEDURE insert_random_mails(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_users INT;
    DECLARE rand_user INT;
    DECLARE unique_email VARCHAR(50);
    
    SELECT COUNT(*) INTO max_users FROM users;
    
    SET @old_sql_mode = @@sql_mode;
    SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
    
    WHILE i < total DO
        SET rand_user = FLOOR(RAND() * max_users);
        
        SET unique_email = CONCAT('mail_', LEFT(UUID(), 8), '_', i, '@example.com');
        
        INSERT IGNORE INTO mails (
            id,
            subject,
            body,
            template,
            email,
            send_at,
            sent_at,
            created_at,
            updated_at,
            user_id
        )
        SELECT 
            NULL, 
            CONCAT('Subject_', i),
            CONCAT('Body_', i),
            ELT(FLOOR(1 + RAND() * 3), 'template1', 'template2', 'template3'),
            unique_email,
            DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 7) DAY),
            NULL,
            NOW(),
            NOW(),
            u.id
        FROM (
            SELECT id 
            FROM users 
            LIMIT 1 OFFSET rand_user
        ) u;
        
        IF ROW_COUNT() > 0 THEN
            SET i = i + 1;
        END IF;
    END WHILE;
    
    SET @@sql_mode = @old_sql_mode;
END$$

DELIMITER ;

CALL insert_random_mails(10);


----------------------------------------------------------ABSENCE--------------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_absences;
DELIMITER $$

CREATE PROCEDURE insert_random_absences(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_users INT;
    DECLARE user_id INT;
    DECLARE unique_ext_id VARCHAR(250);
    DECLARE rows_affected INT;
    
    -- Label for the WHILE loop
    main_loop: BEGIN
    
        -- Get count of available users
        SELECT COUNT(*) INTO max_users FROM users;
        
        -- Check if users table has data
        IF max_users = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Users table is empty';
        END IF;
        
        -- Adjust total if it exceeds available unique users
        IF total > max_users THEN
            SET total = max_users;
        END IF;
        
        -- Create temporary table to track used user_ids
        DROP TEMPORARY TABLE IF EXISTS temp_used_users;
        CREATE TEMPORARY TABLE temp_used_users (
            user_id INT,
            PRIMARY KEY (user_id)
        );
        
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            -- Select an unused user_id
            SELECT 
                u.id
            INTO 
                user_id
            FROM users u
            LEFT JOIN temp_used_users tuu ON tuu.user_id = u.id
            WHERE tuu.user_id IS NULL
            ORDER BY RAND()
            LIMIT 1;
            
            -- If we found an unused user_id
            IF user_id IS NOT NULL THEN
                SET unique_ext_id = CONCAT('ABS_', LEFT(UUID(), 8), '_', i);
                
                -- Insert into temp table first
                INSERT IGNORE INTO temp_used_users (user_id)
                VALUES (user_id);
                
                SET rows_affected = ROW_COUNT();
                
                IF rows_affected > 0 THEN
                    -- Insert the absence with the unique user_id
                    INSERT INTO absences (
                        external_id,
                        reason,
                        comment,
                        start_at,
                        end_at,
                        medical_certificate,
                        created_at,
                        updated_at,
                        user_id
                    ) VALUES (
                        unique_ext_id,
                        ELT(FLOOR(1 + RAND() * 4), 'Maladie', 'Congé', 'Personnel', 'Formation'),
                        CONCAT('Comment_', i),
                        DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 7) DAY),
                        DATE_ADD(DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 7) DAY), INTERVAL FLOOR(RAND() * 5) DAY),
                        FLOOR(RAND() * 2),
                        NOW(),
                        NOW(),
                        user_id
                    );
                    
                    SET i = i + 1;
                    SET user_id = NULL; -- Reset for next iteration
                END IF;
            END IF;
            
            -- If no new unique users are found, exit the loop
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        -- Cleanup
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_users;
    END main_loop;
END$$

DELIMITER ;

CALL insert_random_absences(10);

------------------------------------------------------INDUSTRIES ----------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_industries;
DELIMITER $$

CREATE PROCEDURE insert_random_industries(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE unique_ext_id VARCHAR(250);
    
    SET @old_sql_mode = @@sql_mode;
    SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
    
    WHILE i < total DO
        SET unique_ext_id = CONCAT('IND_', LEFT(UUID(), 8), '_', i);
        
        INSERT IGNORE INTO industries (
            id,
            external_id,
            name
        )
        VALUES (
            NULL,
            unique_ext_id,
            ELT(FLOOR(1 + RAND() * 5), 
                'Technologie', 
                'Finance', 
                'Santé', 
                'Éducation', 
                'Manufacture')
        );
        
        IF ROW_COUNT() > 0 THEN
            SET i = i + 1;
        END IF;
    END WHILE;
    
    SET @@sql_mode = @old_sql_mode;
END$$

DELIMITER ;


CALL insert_random_industries(10);


-----------------------------------------------------------------CLIENT--------------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_clients;
DELIMITER $$

CREATE PROCEDURE insert_random_clients(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_users INT;
    DECLARE max_industries INT;
    DECLARE max_clients INT;
    DECLARE rand_user INT;
    DECLARE rand_industry INT;
    DECLARE unique_ext_id VARCHAR(250);
    
    -- Create temporary table to track used combinations
    DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
    CREATE TEMPORARY TABLE temp_used_clients (
        user_id INT,
        industry_id INT,
        PRIMARY KEY (user_id, industry_id)
    );
    
    SELECT COUNT(*) INTO max_users FROM users;
    SELECT COUNT(*) INTO max_industries FROM industries;
    SELECT COUNT(*) INTO max_clients FROM clients;
    
    -- Check if required tables have data
    IF max_users = 0 OR max_industries = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Users or industries table is empty';
    END IF;
    
    -- If total requested exceeds possible unique combinations, adjust to max possible
    IF total > (max_users * max_industries) THEN
        SET total = max_users * max_industries;
    END IF;
    
    SET @old_sql_mode = @@sql_mode;
    SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
    
    WHILE i < total DO
        SET rand_user = FLOOR(RAND() * max_users);
        SET rand_industry = FLOOR(RAND() * max_industries);
        SET unique_ext_id = UUID();
        
        INSERT INTO clients (
            id,
            external_id,
            address,
            zipcode,
            city,
            company_name,
            vat,
            company_type,
            client_number,
            deleted_at,
            created_at,
            updated_at,
            user_id,
            industry_id
        )
        SELECT 
            NULL,
            unique_ext_id,
            CONCAT('Address_', i),
            CONCAT('ZIP_', LPAD(FLOOR(RAND() * 99999), 5, '0')),
            ELT(FLOOR(1 + RAND() * 5), 'Paris', 'Berlin', 'Madrid', 'Rome', 'London'),
            CONCAT('Company_', i),
            CONCAT('VAT_', LEFT(MD5(RAND()), 10)),
            ELT(FLOOR(1 + RAND() * 3), 'SARL', 'SA', 'SAS'),
            i + 1000,
            NULL,
            NOW(),
            NOW(),
            u.id AS user_id,
            ind.id AS industry_id
        FROM (
            SELECT id 
            FROM users 
            LIMIT 1 OFFSET rand_user
        ) u
        CROSS JOIN (
            SELECT id 
            FROM industries 
            LIMIT 1 OFFSET rand_industry
        ) ind
        LEFT JOIN temp_used_clients tuc 
            ON tuc.user_id = u.id 
            AND tuc.industry_id = ind.id
        WHERE tuc.user_id IS NULL 
            AND tuc.industry_id IS NULL
        LIMIT 1;
        
        -- Track the used combination
        IF ROW_COUNT() > 0 THEN
            INSERT INTO temp_used_clients (user_id, industry_id)
            SELECT u.id, ind.id
            FROM (
                SELECT id 
                FROM users 
                LIMIT 1 OFFSET rand_user
            ) u
            CROSS JOIN (
                SELECT id 
                FROM industries 
                LIMIT 1 OFFSET rand_industry
            ) ind;
            
            SET i = i + 1;
        END IF;
    END WHILE;
    
    -- Cleanup
    DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
    SET @@sql_mode = @old_sql_mode;
END$$

DELIMITER ;

CALL insert_random_clients(10);
----------------------------------------------------------CONTACTS-------------------------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_contacts;
DELIMITER $$

CREATE PROCEDURE insert_random_contacts(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_clients INT;
    DECLARE used_clients INT;
    
    -- Create temporary table to track used client_ids
    DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
    CREATE TEMPORARY TABLE temp_used_clients (
        client_id INT PRIMARY KEY
    );
    
    SELECT COUNT(*) INTO max_clients FROM clients;
    
    IF max_clients = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Clients table is empty';
    END IF;
    
    -- If total requested exceeds available clients, adjust to max possible
    IF total > max_clients THEN
        SET total = max_clients;
    END IF;
    
    SET @old_sql_mode = @@sql_mode;
    SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
    
    WHILE i < total DO
        -- Insert into contacts using an unused client_id
        INSERT INTO contacts (
            id,
            external_id,
            name,
            email,
            primary_number,
            secondary_number,
            is_primary,
            deleted_at,
            created_at,
            updated_at,
            client_id
        )
        SELECT 
            NULL,
            CONCAT('CNT_', LEFT(UUID(), 8), '_', i),
            CONCAT('Contact_', i),
            CONCAT('contact_', LEFT(UUID(), 8), '_', i, '@example.com'),
            CONCAT('123-', LPAD(i, 6, '0')),
            CONCAT('456-', LPAD(i, 6, '0')),
            1,
            NULL,
            NOW(),
            NOW(),
            c.id
        FROM clients c
        LEFT JOIN temp_used_clients tuc ON c.id = tuc.client_id
        WHERE tuc.client_id IS NULL
        LIMIT 1;
        
        -- Track the used client_id
        INSERT INTO temp_used_clients (client_id)
        SELECT LAST_INSERT_ID() WHERE ROW_COUNT() > 0;
        
        IF ROW_COUNT() > 0 THEN
            SET i = i + 1;
        END IF;
    END WHILE;
    
    -- Cleanup
    DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
    SET @@sql_mode = @old_sql_mode;
END$$

DELIMITER ;

CALL insert_random_contacts(10);

-----------------------------------------------------------Appointments--------------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_appointments;
DELIMITER $$

CREATE PROCEDURE insert_random_appointments(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_clients INT;
    DECLARE max_users INT;
    DECLARE client_id INT;
    DECLARE user_id INT;
    DECLARE unique_ext_id VARCHAR(250);
    DECLARE start_date DATETIME;
    DECLARE source_type VARCHAR(20);
    DECLARE color VARCHAR(10);
    DECLARE rows_affected INT;
    
    -- Label for the WHILE loop
    main_loop: BEGIN
    
        -- Get counts of available records
        SELECT COUNT(*) INTO max_clients FROM clients;
        SELECT COUNT(*) INTO max_users FROM users;
        
        -- Check if required tables have data
        IF max_clients = 0 OR max_users = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Clients or users table is empty';
        END IF;
        
        -- Adjust total if it exceeds possible combinations
        IF total > (max_clients * max_users) THEN
            SET total = max_clients * max_users;
        END IF;
        
        -- Create temporary table to track used combinations
        DROP TEMPORARY TABLE IF EXISTS temp_used_combinations;
        CREATE TEMPORARY TABLE temp_used_combinations (
            client_id INT,
            user_id INT,
            PRIMARY KEY (client_id, user_id)
        );
        
        SET autocommit = 0;
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        START TRANSACTION;
        
        WHILE i < total DO
            SET start_date = DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 7) DAY);
            SET rows_affected = 0;
            
            -- Select an unused combination using LEFT JOIN instead of NOT IN
            SELECT 
                c.id,
                u.id
            INTO 
                client_id,
                user_id
            FROM clients c
            CROSS JOIN users u
            LEFT JOIN temp_used_combinations tuc 
                ON tuc.client_id = c.id 
                AND tuc.user_id = u.id
            WHERE tuc.client_id IS NULL
            ORDER BY RAND()
            LIMIT 1;
            
            -- Insert the combination if we found one
            IF client_id IS NOT NULL AND user_id IS NOT NULL THEN
                INSERT IGNORE INTO temp_used_combinations (client_id, user_id)
                VALUES (client_id, user_id);
                
                SET rows_affected = ROW_COUNT();
            END IF;
            
            IF rows_affected > 0 THEN
                -- Insert the appointment with the unique combination
                INSERT INTO appointments (
                    external_id,
                    title,
                    description,
                    source_type,
                    source_id,
                    color,
                    start_at,
                    end_at,
                    created_at,
                    updated_at,
                    deleted_at,
                    client_id,
                    user_id
                ) VALUES (
                    CONCAT('APP_', LEFT(UUID(), 8), '_', i),
                    CONCAT('Meeting_', i),
                    CONCAT('Desc_', i),
                    ELT(FLOOR(1 + RAND() * 3), 'Meeting', 'Call', 'Event'),
                    CONCAT('SRC_', LEFT(MD5(RAND()), 8)),
                    ELT(FLOOR(1 + RAND() * 4), '#FF0000', '#00FF00', '#0000FF', '#FFFF00'),
                    start_date,
                    DATE_ADD(start_date, INTERVAL FLOOR(RAND() * 2) HOUR),
                    NOW(),
                    NOW(),
                    NULL,
                    client_id,
                    user_id
                );
                
                SET i = i + 1;
                SET client_id = NULL;
                SET user_id = NULL;
                
                IF i % 100 = 0 THEN
                    COMMIT;
                    START TRANSACTION;
                END IF;
            END IF;
            
            -- If no new unique combinations are found, exit the loop
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        COMMIT;
        
        -- Cleanup
        SET autocommit = 1;
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_combinations;
    END main_loop;
END$$

DELIMITER ;

CALL insert_random_appointments(10);

--------------------------------------------------------LEADS--------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_leads;
DELIMITER $$

CREATE PROCEDURE insert_random_leads(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_users INT;
    DECLARE max_clients INT;
    DECLARE max_statuses INT;
    DECLARE user_created_id INT;
    DECLARE user_assigned_id INT;
    DECLARE client_id INT;
    DECLARE status_id INT;
    DECLARE unique_ext_id VARCHAR(50);
    DECLARE rows_affected INT;
    
    main_loop: BEGIN
    
        SELECT COUNT(*) INTO max_users FROM users;
        SELECT COUNT(*) INTO max_clients FROM clients;
        SELECT COUNT(*) INTO max_statuses FROM statuses;
        
        -- Check if required tables have data
        IF max_users = 0 OR max_clients = 0 OR max_statuses = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Users, clients, or statuses table is empty';
        END IF;
        
        -- Adjust total if it exceeds possible combinations (using client_id as unique constraint)
        IF total > max_clients THEN
            SET total = max_clients;
        END IF;
        
        -- Create temporary table to track used client_ids
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
        CREATE TEMPORARY TABLE temp_used_clients (
            client_id INT,
            PRIMARY KEY (client_id)
        );
        
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            -- Select an unused client_id and random other IDs
            SELECT 
                c.id,
                uc.id,
                ua.id,
                s.id
            INTO 
                client_id,
                user_created_id,
                user_assigned_id,
                status_id
            FROM clients c
            LEFT JOIN temp_used_clients tuc ON tuc.client_id = c.id
            CROSS JOIN (
                SELECT id FROM users ORDER BY RAND() LIMIT 1
            ) uc
            CROSS JOIN (
                SELECT id FROM users ORDER BY RAND() LIMIT 1
            ) ua
            CROSS JOIN (
                SELECT id FROM statuses ORDER BY RAND() LIMIT 1
            ) s
            WHERE tuc.client_id IS NULL
            ORDER BY RAND()
            LIMIT 1;
            
            -- If we found an unused combination
            IF client_id IS NOT NULL THEN
                SET unique_ext_id = CONCAT('LEAD_', LEFT(UUID(), 8), '_', i);
                
                -- Insert into temp table first
                INSERT IGNORE INTO temp_used_clients (client_id)
                VALUES (client_id);
                
                SET rows_affected = ROW_COUNT();
                
                IF rows_affected > 0 THEN
                    -- Insert the lead with the unique combination
                    INSERT INTO leads (
                        external_id,
                        title,
                        description,
                        qualified,
                        result,
                        deadline,
                        deleted_at,
                        created_at,
                        updated_at,
                        user_created_id,
                        user_assigned_id,
                        client_id,
                        status_id
                    ) VALUES (
                        unique_ext_id,
                        CONCAT('Lead_', i),
                        CONCAT('Description_', i),
                        FLOOR(RAND() * 2),
                        ELT(FLOOR(1 + RAND() * 3), 'Pending', 'Success', 'Failed'),
                        DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
                        NULL,
                        NOW(),
                        NOW(),
                        user_created_id,
                        user_assigned_id,
                        client_id,
                        status_id
                    );
                    
                    SET i = i + 1;
                    SET client_id = NULL; -- Reset for next iteration
                END IF;
            END IF;
            
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
    END main_loop;
END$$

DELIMITER ;

CALL insert_random_leads(10);

-----------------------------------------------------PROJECTS--------------------------------
DROP PROCEDURE IF EXISTS insert_random_projects;
DELIMITER $$

CREATE PROCEDURE insert_random_projects(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_users INT;
    DECLARE max_clients INT;
    DECLARE max_statuses INT;
    DECLARE user_created_id INT;
    DECLARE user_assigned_id INT;
    DECLARE client_id INT;
    DECLARE status_id INT;
    DECLARE unique_ext_id VARCHAR(250);
    DECLARE rows_affected INT;
    
    -- Label for the WHILE loop
    main_loop: BEGIN
    
        -- Get counts of available records
        SELECT COUNT(*) INTO max_users FROM users;
        SELECT COUNT(*) INTO max_clients FROM clients;
        SELECT COUNT(*) INTO max_statuses FROM statuses;
        
        -- Check if required tables have data
        IF max_users = 0 OR max_clients = 0 OR max_statuses = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Users, clients, or statuses table is empty';
        END IF;
        
        -- Adjust total if it exceeds possible combinations (using client_id as unique constraint)
        IF total > max_clients THEN
            SET total = max_clients;
        END IF;
        
        -- Create temporary table to track used client_ids
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
        CREATE TEMPORARY TABLE temp_used_clients (
            client_id INT,
            PRIMARY KEY (client_id)
        );
        
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            -- Select an unused client_id and random other IDs
            SELECT 
                c.id,
                uc.id,
                ua.id,
                s.id
            INTO 
                client_id,
                user_created_id,
                user_assigned_id,
                status_id
            FROM clients c
            LEFT JOIN temp_used_clients tuc ON tuc.client_id = c.id
            CROSS JOIN (
                SELECT id FROM users ORDER BY RAND() LIMIT 1
            ) uc
            CROSS JOIN (
                SELECT id FROM users ORDER BY RAND() LIMIT 1
            ) ua
            CROSS JOIN (
                SELECT id FROM statuses ORDER BY RAND() LIMIT 1
            ) s
            WHERE tuc.client_id IS NULL
            ORDER BY RAND()
            LIMIT 1;
            
            -- If we found an unused combination
            IF client_id IS NOT NULL THEN
                SET unique_ext_id = CONCAT('PROJ_', LEFT(UUID(), 8), '_', i);
                
                -- Insert into temp table first
                INSERT IGNORE INTO temp_used_clients (client_id)
                VALUES (client_id);
                
                SET rows_affected = ROW_COUNT();
                
                IF rows_affected > 0 THEN
                    -- Insert the project with the unique combination
                    INSERT INTO projects (
                        external_id,
                        title,
                        description,
                        deadline,
                        deleted_at,
                        created_at,
                        updated_at,
                        user_created_id,
                        user_assigned_id,
                        client_id,
                        status_id
                    ) VALUES (
                        unique_ext_id,
                        CONCAT('Project_', i),
                        CONCAT('Description_', i),
                        DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 60) DAY),
                        NULL,
                        NOW(),
                        NOW(),
                        user_created_id,
                        user_assigned_id,
                        client_id,
                        status_id
                    );
                    
                    SET i = i + 1;
                    SET client_id = NULL; -- Reset for next iteration
                END IF;
            END IF;
            
            -- If no new unique clients are found, exit the loop
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        -- Cleanup
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
    END main_loop;
END$$

DELIMITER ;

CALL insert_random_projects(10);



-----------------------------------------------------TASK------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_tasks;
DELIMITER $$

CREATE PROCEDURE insert_random_tasks(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_projects INT;
    DECLARE max_clients INT;
    DECLARE max_users INT;
    DECLARE max_statuses INT;
    DECLARE project_id INT;
    DECLARE client_id INT;
    DECLARE user_created_id INT;
    DECLARE user_assigned_id INT;
    DECLARE status_id INT;
    DECLARE unique_ext_id VARCHAR(250);
    DECLARE rows_affected INT;
    
    -- Label for the WHILE loop
    main_loop: BEGIN
    
        -- Get counts of available records
        SELECT COUNT(*) INTO max_projects FROM projects;
        SELECT COUNT(*) INTO max_clients FROM clients;
        SELECT COUNT(*) INTO max_users FROM users;
        SELECT COUNT(*) INTO max_statuses FROM statuses;
        
        -- Check if required tables have data
        IF max_projects = 0 OR max_clients = 0 OR max_users = 0 OR max_statuses = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Projects, clients, users, or statuses table is empty';
        END IF;
        
        -- Adjust total if it exceeds possible unique clients
        IF total > max_clients THEN
            SET total = max_clients;
        END IF;
        
        -- Create temporary table to track used client_ids
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
        CREATE TEMPORARY TABLE temp_used_clients (
            client_id INT,
            PRIMARY KEY (client_id)
        );
        
        SET autocommit = 0;
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        SET @old_foreign_key_checks = @@foreign_key_checks;
        SET foreign_key_checks = 0;
        
        START TRANSACTION;
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            -- Select an unused client_id and random other IDs
            SELECT 
                p.id,
                c.id,
                uc.id,
                ua.id,
                s.id
            INTO 
                project_id,
                client_id,
                user_created_id,
                user_assigned_id,
                status_id
            FROM clients c
            LEFT JOIN temp_used_clients tuc ON tuc.client_id = c.id
            CROSS JOIN (
                SELECT id FROM projects ORDER BY RAND() LIMIT 1
            ) p
            CROSS JOIN (
                SELECT id FROM users ORDER BY RAND() LIMIT 1
            ) uc
            CROSS JOIN (
                SELECT id FROM users ORDER BY RAND() LIMIT 1
            ) ua
            CROSS JOIN (
                SELECT id FROM statuses ORDER BY RAND() LIMIT 1
            ) s
            WHERE tuc.client_id IS NULL
            ORDER BY RAND()
            LIMIT 1;
            
            -- If we found an unused client_id
            IF client_id IS NOT NULL THEN
                SET unique_ext_id = CONCAT('TASK_', LEFT(UUID(), 8), '_', i);
                
                -- Insert into temp table first
                INSERT IGNORE INTO temp_used_clients (client_id)
                VALUES (client_id);
                
                SET rows_affected = ROW_COUNT();
                
                IF rows_affected > 0 THEN
                    -- Insert the task with the unique client_id
                    INSERT INTO tasks (
                        external_id,
                        title,
                        description,
                        deadline,
                        created_at,
                        updated_at,
                        project_id,
                        client_id,
                        user_created_id,
                        user_assigned_id,
                        status_id
                    ) VALUES (
                        unique_ext_id,
                        CONCAT('Task_', i),
                        CONCAT('Description_', i),
                        DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 14) DAY),
                        NOW(),
                        NOW(),
                        project_id,
                        client_id,
                        user_created_id,
                        user_assigned_id,
                        status_id
                    );
                    
                    SET i = i + 1;
                    SET client_id = NULL; -- Reset for next iteration
                END IF;
            END IF;
            
            -- If no new unique clients are found, exit the loop
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        COMMIT;
        
        -- Cleanup
        SET autocommit = 1;
        SET @@sql_mode = @old_sql_mode;
        SET foreign_key_checks = @old_foreign_key_checks;
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
    END main_loop;
END$$

DELIMITER ;

CALL insert_random_tasks(10);

-------------------------------------------------------OFFERS----------------------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_offers;
DELIMITER $$

CREATE PROCEDURE insert_random_offers(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_clients INT;
    DECLARE client_id INT;
    DECLARE unique_ext_id VARCHAR(250);
    DECLARE rows_affected INT;
    
    -- Label for the WHILE loop
    main_loop: BEGIN
    
        -- Get count of available clients
        SELECT COUNT(*) INTO max_clients FROM clients;
        
        -- Check if clients table has data
        IF max_clients = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Clients table is empty';
        END IF;
        
        -- Adjust total if it exceeds available unique clients
        IF total > max_clients THEN
            SET total = max_clients;
        END IF;
        
        -- Create temporary table to track used client_ids
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
        CREATE TEMPORARY TABLE temp_used_clients (
            client_id INT,
            PRIMARY KEY (client_id)
        );
        
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            -- Select an unused client_id
            SELECT 
                c.id
            INTO 
                client_id
            FROM clients c
            LEFT JOIN temp_used_clients tuc ON tuc.client_id = c.id
            WHERE tuc.client_id IS NULL
            ORDER BY RAND()
            LIMIT 1;
            
            -- If we found an unused client_id
            IF client_id IS NOT NULL THEN
                SET unique_ext_id = CONCAT('OFFER_', LEFT(UUID(), 8), '_', i);
                
                -- Insert into temp table first
                INSERT IGNORE INTO temp_used_clients (client_id)
                VALUES (client_id);
                
                SET rows_affected = ROW_COUNT();
                
                IF rows_affected > 0 THEN
                    -- Insert the offer with the unique client_id
                    INSERT INTO offers (
                        external_id,
                        sent_at,
                        source_type,
                        source_id,
                        status,
                        deleted_at,
                        created_at,
                        updated_at,
                        client_id
                    ) VALUES (
                        unique_ext_id,
                        DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 7) DAY),
                        ELT(FLOOR(1 + RAND() * 3), 'Email', 'Phone', 'Meeting'),
                        CONCAT('SRC_', LEFT(MD5(RAND()), 8)),
                        ELT(FLOOR(1 + RAND() * 3), 'Pending', 'Accepted', 'Rejected'),
                        NULL,
                        NOW(),
                        NOW(),
                        client_id
                    );
                    
                    SET i = i + 1;
                    SET client_id = NULL; -- Reset for next iteration
                END IF;
            END IF;
            
            -- If no new unique clients are found, exit the loop
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        -- Cleanup
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
    END main_loop;
END$$

DELIMITER ;

CALL insert_random_offers(10);
-------------------------------------------------------invoices----------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_invoices;
DELIMITER $$

CREATE PROCEDURE insert_random_invoices(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_clients INT;
    DECLARE max_offers INT;
    DECLARE client_id INT;
    DECLARE offer_id INT;
    DECLARE unique_ext_id VARCHAR(250);
    DECLARE unique_invoice_num INT;
    DECLARE rows_affected INT;
    
    -- Label for the WHILE loop
    main_loop: BEGIN
    
        -- Get counts of available records
        SELECT COUNT(*) INTO max_clients FROM clients;
        SELECT COUNT(*) INTO max_offers FROM offers;
        
        -- Check if required tables have data
        IF max_clients = 0 OR max_offers = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Clients or offers table is empty';
        END IF;
        
        -- Adjust total if it exceeds available unique clients
        IF total > max_clients THEN
            SET total = max_clients;
        END IF;
        
        -- Create temporary table to track used client_ids
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
        CREATE TEMPORARY TABLE temp_used_clients (
            client_id INT,
            PRIMARY KEY (client_id)
        );
        
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            -- Select an unused client_id and a random offer_id
            SELECT 
                c.id,
                o.id
            INTO 
                client_id,
                offer_id
            FROM clients c
            LEFT JOIN temp_used_clients tuc ON tuc.client_id = c.id
            CROSS JOIN (
                SELECT id FROM offers ORDER BY RAND() LIMIT 1
            ) o
            WHERE tuc.client_id IS NULL
            ORDER BY RAND()
            LIMIT 1;
            
            -- If we found an unused client_id
            IF client_id IS NOT NULL THEN
                SET unique_ext_id = CONCAT('INV_', LEFT(UUID(), 8), '_', i);
                SET unique_invoice_num = 10000 + i;
                
                -- Insert into temp table first
                INSERT IGNORE INTO temp_used_clients (client_id)
                VALUES (client_id);
                
                SET rows_affected = ROW_COUNT();
                
                IF rows_affected > 0 THEN
                    -- Insert the invoice with the unique client_id
                    INSERT INTO invoices (
                        external_id,
                        status,
                        invoice_number,
                        sent_at,
                        due_at,
                        integration_invoice_id,
                        integration_type,
                        source_type,
                        source_id,
                        deleted_at,
                        created_at,
                        updated_at,
                        client_id,
                        offer_id
                    ) VALUES (
                        unique_ext_id,
                        ELT(FLOOR(1 + RAND() * 7), 'draft', 'closed', 'sent', 'unpaid', 'partial_paid', 'paid', 'overpaid'),
                        unique_invoice_num,
                        DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 7) DAY),
                        DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
                        CONCAT('INT_', LEFT(MD5(RAND()), 8)),
                        ELT(FLOOR(1 + RAND() * 2), 'ERP', 'CRM'),
                        ELT(FLOOR(1 + RAND() * 3), 'Manual', 'System', 'Import'),
                        CONCAT('SRC_', LEFT(MD5(RAND()), 8)),
                        NULL,
                        NOW(),
                        NOW(),
                        client_id,
                        offer_id
                    );
                    
                    SET i = i + 1;
                    SET client_id = NULL; -- Reset for next iteration
                END IF;
            END IF;
            
            -- If no new unique clients are found, exit the loop
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        -- Cleanup
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_clients;
    END main_loop;
END$$

DELIMITER ;

CALL insert_random_invoices(10);
----------------------------------------------------------------INVOICES LINES----------------------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_invoice_lines;
DELIMITER $$

CREATE PROCEDURE insert_random_invoice_lines(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_offers INT;
    DECLARE max_invoices INT;
    DECLARE offer_id INT;
    DECLARE invoice_id INT;
    DECLARE unique_ext_id VARCHAR(250);
    DECLARE rows_affected INT;
    
    -- Label for the WHILE loop
    main_loop: BEGIN
    
        -- Get counts of available records
        SELECT COUNT(*) INTO max_offers FROM offers;
        SELECT COUNT(*) INTO max_invoices FROM invoices;
        
        -- Check if required tables have data
        IF max_offers = 0 OR max_invoices = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Offers or invoices table is empty';
        END IF;
        
        -- Adjust total if it exceeds available unique invoices
        IF total > max_invoices THEN
            SET total = max_invoices;
        END IF;
        
        -- Create temporary table to track used invoice_ids
        DROP TEMPORARY TABLE IF EXISTS temp_used_invoices;
        CREATE TEMPORARY TABLE temp_used_invoices (
            invoice_id INT,
            PRIMARY KEY (invoice_id)
        );
        
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            -- Select an unused invoice_id and a random offer_id
            SELECT 
                inv.id,
                o.id
            INTO 
                invoice_id,
                offer_id
            FROM invoices inv
            LEFT JOIN temp_used_invoices tui ON tui.invoice_id = inv.id
            CROSS JOIN (
                SELECT id FROM offers ORDER BY RAND() LIMIT 1
            ) o
            WHERE tui.invoice_id IS NULL
            ORDER BY RAND()
            LIMIT 1;
            
            -- If we found an unused invoice_id
            IF invoice_id IS NOT NULL THEN
                SET unique_ext_id = CONCAT('INVL_', LEFT(UUID(), 8), '_', i);
                
                -- Insert into temp table first
                INSERT IGNORE INTO temp_used_invoices (invoice_id)
                VALUES (invoice_id);
                
                SET rows_affected = ROW_COUNT();
                
                IF rows_affected > 0 THEN
                    -- Insert the invoice line with the unique invoice_id
                    INSERT INTO invoice_lines (
                        external_id,
                        title,
                        comment,
                        price,
                        type,
                        quantity,
                        product_id,
                        created_at,
                        updated_at,
                        deleted_at,
                        offer_id,
                        invoice_id
                    ) VALUES (
                        unique_ext_id,
                        CONCAT('Item_', i),
                        CONCAT('Comment_', i),
                        ROUND(RAND() * 1000, 2),
                        ELT(FLOOR(1 + RAND() * 3), 'Service', 'Product', 'Subscription'),
                        FLOOR(1 + RAND() * 10),
                        CONCAT('PROD_', LEFT(MD5(RAND()), 8)),
                        NOW(),
                        NOW(),
                        NULL,
                        offer_id,
                        invoice_id
                    );
                    
                    SET i = i + 1;
                    SET invoice_id = NULL; -- Reset for next iteration
                END IF;
            END IF;
            
            -- If no new unique invoices are found, exit the loop
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        -- Cleanup
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_invoices;
    END main_loop;
END$$

DELIMITER ;

CALL insert_random_invoice_lines(10);
--------------------------------------payments--------------------------------
DROP PROCEDURE IF EXISTS insert_random_payments;
DELIMITER $$

CREATE PROCEDURE insert_random_payments(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE max_invoices INT;
    DECLARE invoice_id INT;
    DECLARE unique_ext_id VARCHAR(250);
    DECLARE rows_affected INT;
    
    main_loop: BEGIN
        SELECT COUNT(*) INTO max_invoices FROM invoices;
        
        IF max_invoices = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoices table is empty';
        END IF;
        
        IF total > max_invoices THEN
            SET total = max_invoices;
        END IF;
        
        DROP TEMPORARY TABLE IF EXISTS temp_used_invoices;
        CREATE TEMPORARY TABLE temp_used_invoices (
            invoice_id INT,
            PRIMARY KEY (invoice_id)
        );
        
        SET @old_sql_mode = @@sql_mode;
        SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
        
        WHILE i < total DO
            SET rows_affected = 0;
            
            SELECT 
                inv.id
            INTO 
                invoice_id
            FROM invoices inv
            LEFT JOIN temp_used_invoices tui ON tui.invoice_id = inv.id
            WHERE tui.invoice_id IS NULL
            ORDER BY RAND()
            LIMIT 1;
            
            IF invoice_id IS NOT NULL THEN
                SET unique_ext_id = CONCAT('PAY_', LEFT(UUID(), 8), '_', i);
                
                INSERT IGNORE INTO temp_used_invoices (invoice_id)
                VALUES (invoice_id);
                
                SET rows_affected = ROW_COUNT();
                
                IF rows_affected > 0 THEN
                    INSERT INTO payments (
                        external_id,
                        amount,
                        description,
                        payment_source,
                        payment_date,
                        integration_payment_id,
                        integration_type,
                        deleted_at,
                        created_at,
                        updated_at,
                        invoice_id
                    ) VALUES (
                        unique_ext_id,
                        ROUND(RAND() * 1000, 2),
                        CONCAT('Payment_', i),
                        ELT(FLOOR(1 + RAND() * 3), 'Bank', 'Card', 'Cash'),
                        -- Modification ici pour une plage de dates plus large (par exemple 12 mois)
                        DATE_SUB(
                            CURDATE(), 
                            INTERVAL FLOOR(RAND() * 365) DAY
                        ),
                        CONCAT('INT_', LEFT(MD5(RAND()), 8)),
                        ELT(FLOOR(1 + RAND() * 3), 'Bank', 'Card', 'Cash'),
                        NULL,
                        NOW(),
                        NOW(),
                        invoice_id
                    );
                    
                    SET i = i + 1;
                    SET invoice_id = NULL;
                END IF;
            END IF;
            
            IF rows_affected = 0 THEN
                LEAVE main_loop;
            END IF;
        END WHILE;
        
        SET @@sql_mode = @old_sql_mode;
        DROP TEMPORARY TABLE IF EXISTS temp_used_invoices;
    END main_loop;
END$$

DELIMITER ;

CALL insert_random_payments(10);
-----------------------------------------------------PRODUCTS--------------------------------
DROP PROCEDURE IF EXISTS insert_random_products;
DELIMITER $$

CREATE PROCEDURE insert_random_products(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE unique_ext_id VARCHAR(50);
    
    SET @old_sql_mode = @@sql_mode;
    SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
    
    WHILE i < total DO
        SET unique_ext_id = CONCAT('PROD_', LEFT(UUID(), 8), '_', i);
        
        INSERT IGNORE INTO products (
            id,
            name,
            external_id,
            description,
            number,
            default_type,
            archived,
            integration_type,
            integration_id,
            price,
            deleted_at,
            created_at,
            updated_at
        )
        VALUES (
            NULL,
            CONCAT('Product_', i),
            unique_ext_id,
            CONCAT('Desc_', i),
            CONCAT('NUM_', LPAD(i, 6, '0')),
            ELT(FLOOR(1 + RAND() * 3), 'Service', 'Goods', 'Subscription'),
            FLOOR(RAND() * 2), 
            ELT(FLOOR(1 + RAND() * 2), 'ERP', 'Manual'),
            FLOOR(RAND() * 10000),
            ROUND(RAND() * 1000, 2),
            NULL, 
            NOW(),
            NOW()
        );
        
        IF ROW_COUNT() > 0 THEN
            SET i = i + 1;
        END IF;
    END WHILE;
    
    SET @@sql_mode = @old_sql_mode;
END$$

DELIMITER ;

CALL insert_random_products(10);


--------------------------------------------------------DOCUMENTS--------------------------------------------------------
DROP PROCEDURE IF EXISTS insert_random_documents;
DELIMITER $$

CREATE PROCEDURE insert_random_documents(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE unique_ext_id VARCHAR(50);
    
    SET @old_sql_mode = @@sql_mode;
    SET @@sql_mode = 'NO_AUTO_VALUE_ON_ZERO';
    
    WHILE i < total DO
        SET unique_ext_id = CONCAT('DOC_', LEFT(UUID(), 8), '_', i);
        
        INSERT IGNORE INTO documents (
            id,
            external_id,
            size,
            path,
            original_filename,
            mime,
            integration_id,
            integration_type,
            source_type,
            source_id,
            deleted_at,
            created_at,
            updated_at
        )
        VALUES (
            NULL, 
            unique_ext_id,
            CONCAT(ROUND(RAND() * 1024), ' KB'), 
            CONCAT('/docs/doc_', i, '.pdf'),
            CONCAT('file_', i, '.pdf'),
            ELT(FLOOR(1 + RAND() * 3), 'application/pdf', 'image/jpeg', 'text/plain'),
            CONCAT('INT_', LEFT(MD5(RAND()), 8)),
            ELT(FLOOR(1 + RAND() * 2), 'Cloud', 'Local'),
            ELT(FLOOR(1 + RAND() * 3), 'Upload', 'Generated', 'Imported'),
            FLOOR(RAND() * 10000),
            NULL, 
            NOW(),
            NOW()
        );
        
        IF ROW_COUNT() > 0 THEN
            SET i = i + 1;
        END IF;
    END WHILE;
    
    SET @@sql_mode = @old_sql_mode;
END$$

DELIMITER ;

CALL insert_random_documents(10);

