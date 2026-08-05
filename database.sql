-- ==========================================================
-- ULEE — Seed data for local development/testing
-- Assumes a FRESH database (empty tables). Explicit IDs are used
-- so relationships are predictable — do not run twice without
-- clearing tables first, or you'll get duplicate-key errors.
-- ==========================================================

-- ── USERS (shared login table) ──
INSERT INTO users (userID, firstName, lastName, email, password, phone, dateOfBirth) VALUES
                                                                                         (1, 'Ezile', 'Hlomane', 'ezile-ulee@gmail.com', '$2a$10$1SD1ofodj/m1MK.9zlv/XerVtfUZuQhGlJL7zsYpnGl1eHWC2LTT2', '0821234567', '2001-03-14'),
                                                                                         (2, 'Thandeka', 'Mnguni', 'thandeka.landlord@ulee.co.za', '$2a$10$1SD1ofodj/m1MK.9zlv/XerVtfUZuQhGlJL7zsYpnGl1eHWC2LTT2', '0827654321', '1985-07-02'),
                                                                                         (3, 'Bongani', 'Sithole', 'bongani.landlord@ulee.co.za', '$2a$10$1SD1ofodj/m1MK.9zlv/XerVtfUZuQhGlJL7zsYpnGl1eHWC2LTT2', '0839988776', '1990-11-20'),
                                                                                         (4, 'Aisha', 'Adams', 'aisha.student@ulee.co.za', '$2a$10$1SD1ofodj/m1MK.9zlv/XerVtfUZuQhGlJL7zsYpnGl1eHWC2LTT2', '0741122334', '2004-05-09'),
                                                                                         (5, 'Luyanda', 'Peter', 'luyanda.student@ulee.co.za', '$2a$10$1SD1ofodj/m1MK.9zlv/XerVtfUZuQhGlJL7zsYpnGl1eHWC2LTT2', '0765544332', '2003-09-30');

-- ── LANDLORDS ──
INSERT INTO landlords (landlordID, companyName, propertiesCount) VALUES
                                                                     (1, 'Hlomane Properties', 2),
                                                                     (2, 'Mnguni Student Lets', 2),
                                                                     (3, 'Sithole Housing', 2);

-- ── STUDENTS ──
INSERT INTO students (studentID, yearOfStudy, budgetMin, budgetMax) VALUES
                                                                        (4, 2, 2000.00, 4500.00),
                                                                        (5, 3, 3000.00, 6000.00);

-- ── PROPERTIES (2 per landlord, spread across Gqeberha suburbs) ──
INSERT INTO property
(propertyID, landlordID, title, description, rent, deposit, address, city, municipality, suburb,
 type, bedrooms, bathrooms, area, furnished, studyFriendly, isAvailable, availableFrom,
 distanceFromUniversity, rating, reviewCount)
VALUES
    (1, 1, 'Arteria Parktown', 'Modern student studio close to campus, secure parking included.',
     4500.00, 4500.00, '24 University Way', 'Gqeberha', 'Nelson Mandela Bay', 'Summerstrand',
     'Studio Apartment', 1, 1, 28.00, TRUE, TRUE, TRUE, '2026-02-01', 1.20, 4.5, 2),

    (2, 1, 'Hlomane Riverside House', 'Shared house, 4 bedrooms, walking distance to res.',
     3200.00, 3200.00, '10 River Road', 'Gqeberha', 'Nelson Mandela Bay', 'Summerstrand',
     'Shared House', 4, 2, 140.00, TRUE, TRUE, TRUE, '2026-01-15', 2.00, 4.0, 0),

    (3, 2, 'The Hub Gqeberha', 'Purpose-built student block, 24h security guard.',
     3800.00, 3800.00, '15 Strand Street', 'Gqeberha', 'Nelson Mandela Bay', 'Central',
     'Purpose-built Block', 1, 1, 22.00, TRUE, TRUE, TRUE, '2026-02-01', 3.50, 4.2, 1),

    (4, 2, 'Mnguni En-suite Rooms', 'Private en-suite room in a quiet complex.',
     4100.00, 4100.00, '5 Park Lane', 'Gqeberha', 'Nelson Mandela Bay', 'Central',
     'En-suite Room', 1, 1, 20.00, TRUE, FALSE, FALSE, '2026-01-01', 4.00, 3.8, 0),

    (5, 3, 'Campus Bay View', 'Beachfront apartment with shuttle service to campus.',
     5200.00, 5200.00, '8 Beachfront Rd', 'Gqeberha', 'Nelson Mandela Bay', 'Humewood',
     'Studio Apartment', 1, 1, 30.00, TRUE, TRUE, TRUE, '2026-03-01', 5.50, 4.7, 1),

    (6, 3, 'Sithole Terraced House', 'Terraced house, 3 bedrooms, garden, pet friendly.',
     3600.00, 3600.00, '20 Beach Road', 'Gqeberha', 'Nelson Mandela Bay', 'Humewood',
     'Terraced House', 3, 1, 110.00, FALSE, TRUE, TRUE, '2026-02-15', 6.00, 4.1, 0);

-- ── PROPERTY IMAGES (one main/front image per property — placeholders) ──
INSERT INTO propertyimage (propertyID, url, category, isMain, displayOrder) VALUES
                                                                                (1, '/uploads/placeholder-arteria.jpg', 'exterior', TRUE, 1),
                                                                                (2, '/uploads/placeholder-riverside.jpg', 'exterior', TRUE, 1),
                                                                                (3, '/uploads/placeholder-hub.jpg', 'exterior', TRUE, 1),
                                                                                (4, '/uploads/placeholder-ensuite.jpg', 'exterior', TRUE, 1),
                                                                                (5, '/uploads/placeholder-bayview.jpg', 'exterior', TRUE, 1),
                                                                                (6, '/uploads/placeholder-terraced.jpg', 'exterior', TRUE, 1);

-- ── APPLICATIONS (mixed statuses so every dashboard view has something to show) ──
INSERT INTO application (studentID, propertyID, status, applicationDate) VALUES
                                                                             (4, 1, 'Pending', '2026-07-28 09:15:00'),
                                                                             (5, 3, 'Pending', '2026-07-30 14:40:00'),
                                                                             (4, 5, 'Accepted', '2026-07-20 11:00:00'),
                                                                             (5, 2, 'Rejected', '2026-07-22 16:30:00');

-- ── REVIEWS (so my-property-reviews and property ratings aren't empty) ──
INSERT INTO review (studentID, propertyID, rating, comment, reviewDate) VALUES
                                                                            (4, 1, 5, 'Great location, walking distance to everything. Landlord is very responsive.', '2026-06-10 10:00:00'),
                                                                            (5, 1, 4, 'Solid studio, a bit noisy on weekends but overall good value.', '2026-06-15 18:30:00'),
                                                                            (4, 3, 4, 'Security is excellent, felt very safe here all year.', '2026-05-22 09:45:00'),
                                                                            (5, 5, 5, 'Best view in Gqeberha, shuttle service was a lifesaver during exams.', '2026-06-01 14:20:00');












--
--
--
--
-- CREATE DATABASE ulee_db;
-- USE ulee_db;
-- CREATE TABLE users (
--     userID       INT AUTO_INCREMENT PRIMARY KEY,
--     password     VARCHAR(60) NOT NULL,
--     firstName    VARCHAR(50) NOT NULL,
--     lastName     VARCHAR(50) NOT NULL,
--     dateOfBirth  DATE,
--     email        VARCHAR(100) UNIQUE NOT NULL,
--     phone        VARCHAR(20),
--     avatar       VARCHAR(255)
-- );
-- CREATE TABLE students (
--     studentID   INT PRIMARY KEY,
--     yearOfStudy INT,
--     budgetMin   DECIMAL(10,2),
--     budgetMax   DECIMAL(10,2),
--     FOREIGN KEY (studentID) REFERENCES users(userID)
-- );
--
-- CREATE TABLE landlords (
--     landlordID      INT PRIMARY KEY,
--     companyName     VARCHAR(100),
--     propertiesCount INT DEFAULT 0,
--     FOREIGN KEY (landlordID) REFERENCES users(userID)
-- );
--
-- CREATE TABLE admins (
--     adminID INT PRIMARY KEY,
--     FOREIGN KEY (adminID) REFERENCES users(userID)
-- );
-- CREATE TABLE property (
--     propertyID              INT AUTO_INCREMENT PRIMARY KEY,
--     landlordID              INT NOT NULL,
--     title                   VARCHAR(150),
--     description             TEXT,
--     rent                    DECIMAL(10,2),
--     deposit                 DECIMAL(10,2),
--     address                 VARCHAR(255),
--     city                    VARCHAR(100),
--     municipality            VARCHAR(100),
--     suburb                  VARCHAR(100),
--     latitude                DECIMAL(9,6),
--     longitude               DECIMAL(9,6),
--     type                    VARCHAR(50),
--     bedrooms                INT,
--     bathrooms               INT,
--     area                    DECIMAL(8,2),
--     furnished               BOOLEAN DEFAULT FALSE,
--     studyFriendly           BOOLEAN DEFAULT FALSE,
--     isAvailable             BOOLEAN DEFAULT TRUE,
--     availableFrom           DATE,
--     distanceFromUniversity  DECIMAL(5,2),
--     rating                  DECIMAL(2,1) DEFAULT 0,
--     reviewCount             INT DEFAULT 0,
--     createdAt               DATETIME DEFAULT CURRENT_TIMESTAMP,
--     updatedAt               DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     FOREIGN KEY (landlordID) REFERENCES landlords(landlordID)
-- );
--
-- CREATE TABLE propertyimage (
--     imageID       INT AUTO_INCREMENT PRIMARY KEY,
--     propertyID    INT NOT NULL,
--     url           VARCHAR(255),
--     category      VARCHAR(50),
--     caption       VARCHAR(150),
--     isMain        BOOLEAN DEFAULT FALSE,
--     displayOrder  INT,
--     uploadedAt    DATETIME DEFAULT CURRENT_TIMESTAMP,
--     hasWatermark  BOOLEAN DEFAULT FALSE,
--     isVR          BOOLEAN DEFAULT FALSE,
--     FOREIGN KEY (propertyID) REFERENCES property(propertyID)
-- );
--
-- CREATE TABLE application (
--     applicationID   INT AUTO_INCREMENT PRIMARY KEY,
--     studentID       INT NOT NULL,
--     propertyID      INT NOT NULL,
--     status          VARCHAR(20) DEFAULT 'Pending',
--     applicationDate DATETIME DEFAULT CURRENT_TIMESTAMP,
--     FOREIGN KEY (studentID) REFERENCES students(studentID),
--     FOREIGN KEY (propertyID) REFERENCES property(propertyID)
-- );
--
-- CREATE TABLE message (
--     messageID      INT AUTO_INCREMENT PRIMARY KEY,
--     senderName     VARCHAR(100),
--     messageSubject VARCHAR(150),
--     messagePreview VARCHAR(255),
--     sentDate       DATETIME DEFAULT CURRENT_TIMESTAMP,
--     readStatus     BOOLEAN DEFAULT FALSE,
--     type           VARCHAR(50)
-- );
--
-- CREATE TABLE savedproperty (
--     savedID      INT AUTO_INCREMENT PRIMARY KEY,
--     studentID    INT NOT NULL,
--     propertyID   INT NOT NULL,
--     savedStatus  BOOLEAN DEFAULT TRUE,
--     FOREIGN KEY (studentID) REFERENCES students(studentID),
--     FOREIGN KEY (propertyID) REFERENCES property(propertyID)
-- );
--
-- CREATE TABLE searchfilter (
--     searchID             INT AUTO_INCREMENT PRIMARY KEY,
--     studentID            INT NOT NULL,
--     queryText            VARCHAR(255),
--     location             VARCHAR(100),
--     priceRange           VARCHAR(50),
--     propertyRange        VARCHAR(50),
--     distanceFromCampus   DECIMAL(5,2),
--     securityLevel        VARCHAR(50),
--     furnished            BOOLEAN,
--     petFriendly          BOOLEAN,
--     searchDate           DATETIME DEFAULT CURRENT_TIMESTAMP,
--     resultsCount         INT,
--     FOREIGN KEY (studentID) REFERENCES students(studentID)
-- );
--
-- CREATE TABLE checklistitem (
--     checklistID      INT AUTO_INCREMENT PRIMARY KEY,
--     studentID        INT NOT NULL,
--     task             VARCHAR(150),
--     completionStatus BOOLEAN DEFAULT FALSE,
--     FOREIGN KEY (studentID) REFERENCES students(studentID)
-- );
--
-- CREATE TABLE inquiry (
--     inquiryID       INT AUTO_INCREMENT PRIMARY KEY,
--     adminID         INT,
--     studentName     VARCHAR(100),
--     propertyTitle   VARCHAR(150),
--     inquiryMessage  TEXT,
--     inquiryDate     DATETIME DEFAULT CURRENT_TIMESTAMP,
--     status          VARCHAR(50),
--     urgencyFlag     BOOLEAN DEFAULT FALSE,
--     FOREIGN KEY (adminID) REFERENCES admins(adminID)
-- );
--
