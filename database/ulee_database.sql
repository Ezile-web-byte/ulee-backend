CREATE DATABASE  IF NOT EXISTS `ulee_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `ulee_db`;
-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: ulee_db
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admins`
--

DROP TABLE IF EXISTS `admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admins` (
                          `adminID` int NOT NULL,
                          PRIMARY KEY (`adminID`),
                          CONSTRAINT `admins_ibfk_1` FOREIGN KEY (`adminID`) REFERENCES `users` (`userID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admins`
--

LOCK TABLES `admins` WRITE;
/*!40000 ALTER TABLE `admins` DISABLE KEYS */;
/*!40000 ALTER TABLE `admins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `application`
--

DROP TABLE IF EXISTS `application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `application` (
                               `applicationID` int NOT NULL AUTO_INCREMENT,
                               `studentID` int NOT NULL,
                               `propertyID` int NOT NULL,
                               `status` varchar(255) DEFAULT NULL,
                               `applicationDate` datetime DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (`applicationID`),
                               KEY `studentID` (`studentID`),
                               KEY `propertyID` (`propertyID`),
                               CONSTRAINT `application_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `students` (`studentID`),
                               CONSTRAINT `application_ibfk_2` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `application`
--

LOCK TABLES `application` WRITE;
/*!40000 ALTER TABLE `application` DISABLE KEYS */;
/*!40000 ALTER TABLE `application` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `application_document`
--

DROP TABLE IF EXISTS `application_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `application_document` (
                                        `documentID` int NOT NULL AUTO_INCREMENT,
                                        `applicationID` int NOT NULL,
                                        `fileName` varchar(255) NOT NULL,
                                        `filePath` varchar(255) DEFAULT NULL,
                                        `uploadedAt` datetime DEFAULT CURRENT_TIMESTAMP,
                                        PRIMARY KEY (`documentID`),
                                        KEY `applicationID` (`applicationID`),
                                        CONSTRAINT `application_document_ibfk_1` FOREIGN KEY (`applicationID`) REFERENCES `application` (`applicationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `application_document`
--

LOCK TABLES `application_document` WRITE;
/*!40000 ALTER TABLE `application_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `application_document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `checklistitem`
--

DROP TABLE IF EXISTS `checklistitem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `checklistitem` (
                                 `checklistID` int NOT NULL AUTO_INCREMENT,
                                 `studentID` int NOT NULL,
                                 `task` varchar(150) DEFAULT NULL,
                                 `completionStatus` tinyint(1) DEFAULT '0',
                                 PRIMARY KEY (`checklistID`),
                                 KEY `studentID` (`studentID`),
                                 CONSTRAINT `checklistitem_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `students` (`studentID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `checklistitem`
--

LOCK TABLES `checklistitem` WRITE;
/*!40000 ALTER TABLE `checklistitem` DISABLE KEYS */;
/*!40000 ALTER TABLE `checklistitem` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inquiry`
--

DROP TABLE IF EXISTS `inquiry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inquiry` (
                           `inquiryID` int NOT NULL AUTO_INCREMENT,
                           `adminID` int DEFAULT NULL,
                           `studentName` varchar(100) DEFAULT NULL,
                           `propertyTitle` varchar(150) DEFAULT NULL,
                           `inquiryMessage` text,
                           `inquiryDate` datetime DEFAULT CURRENT_TIMESTAMP,
                           `status` varchar(50) DEFAULT NULL,
                           `urgencyFlag` tinyint(1) DEFAULT '0',
                           PRIMARY KEY (`inquiryID`),
                           KEY `adminID` (`adminID`),
                           CONSTRAINT `inquiry_ibfk_1` FOREIGN KEY (`adminID`) REFERENCES `admins` (`adminID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inquiry`
--

LOCK TABLES `inquiry` WRITE;
/*!40000 ALTER TABLE `inquiry` DISABLE KEYS */;
/*!40000 ALTER TABLE `inquiry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `landlords`
--

DROP TABLE IF EXISTS `landlords`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `landlords` (
                             `landlordID` int NOT NULL,
                             `companyName` varchar(255) DEFAULT NULL,
                             `propertiesCount` int DEFAULT '0',
                             PRIMARY KEY (`landlordID`),
                             CONSTRAINT `landlords_ibfk_1` FOREIGN KEY (`landlordID`) REFERENCES `users` (`userID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `landlords`
--

LOCK TABLES `landlords` WRITE;
/*!40000 ALTER TABLE `landlords` DISABLE KEYS */;
INSERT INTO `landlords` VALUES (3,'16 Cardiff',0);
/*!40000 ALTER TABLE `landlords` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
                           `messageID` int NOT NULL AUTO_INCREMENT,
                           `senderName` varchar(100) DEFAULT NULL,
                           `messageSubject` varchar(150) DEFAULT NULL,
                           `messagePreview` varchar(255) DEFAULT NULL,
                           `sentDate` datetime DEFAULT CURRENT_TIMESTAMP,
                           `readStatus` tinyint(1) DEFAULT '0',
                           `type` varchar(50) DEFAULT NULL,
                           PRIMARY KEY (`messageID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message`
--

LOCK TABLES `message` WRITE;
/*!40000 ALTER TABLE `message` DISABLE KEYS */;
/*!40000 ALTER TABLE `message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property`
--

DROP TABLE IF EXISTS `property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `property` (
                            `propertyID` int NOT NULL AUTO_INCREMENT,
                            `landlordID` int NOT NULL,
                            `title` varchar(255) DEFAULT NULL,
                            `description` text,
                            `rent` decimal(38,2) DEFAULT NULL,
                            `deposit` decimal(38,2) DEFAULT NULL,
                            `address` varchar(255) DEFAULT NULL,
                            `city` varchar(255) DEFAULT NULL,
                            `municipality` varchar(255) DEFAULT NULL,
                            `suburb` varchar(255) DEFAULT NULL,
                            `latitude` decimal(38,2) DEFAULT NULL,
                            `longitude` decimal(38,2) DEFAULT NULL,
                            `type` varchar(255) DEFAULT NULL,
                            `bedrooms` int DEFAULT NULL,
                            `bathrooms` int DEFAULT NULL,
                            `area` decimal(38,2) DEFAULT NULL,
                            `furnished` tinyint(1) DEFAULT '0',
                            `studyFriendly` tinyint(1) DEFAULT '0',
                            `isAvailable` tinyint(1) DEFAULT '1',
                            `availableFrom` date DEFAULT NULL,
                            `distanceFromUniversity` decimal(38,2) DEFAULT NULL,
                            `rating` decimal(38,2) DEFAULT NULL,
                            `reviewCount` int DEFAULT '0',
                            `createdAt` datetime DEFAULT CURRENT_TIMESTAMP,
                            `updatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `isReported` bit(1) DEFAULT NULL,
                            `reportReason` varchar(255) DEFAULT NULL,
                            `status` varchar(255) DEFAULT NULL,
                            `commuteType` varchar(50) DEFAULT NULL,
                            PRIMARY KEY (`propertyID`),
                            KEY `landlordID` (`landlordID`),
                            CONSTRAINT `property_ibfk_1` FOREIGN KEY (`landlordID`) REFERENCES `landlords` (`landlordID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property`
--

LOCK TABLES `property` WRITE;
/*!40000 ALTER TABLE `property` DISABLE KEYS */;
/*!40000 ALTER TABLE `property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `propertyimage`
--

DROP TABLE IF EXISTS `propertyimage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `propertyimage` (
                                 `imageID` int NOT NULL AUTO_INCREMENT,
                                 `propertyID` int NOT NULL,
                                 `url` varchar(255) DEFAULT NULL,
                                 `category` varchar(255) DEFAULT NULL,
                                 `caption` varchar(255) DEFAULT NULL,
                                 `isMain` tinyint(1) DEFAULT '0',
                                 `displayOrder` int DEFAULT NULL,
                                 `uploadedAt` datetime DEFAULT CURRENT_TIMESTAMP,
                                 `hasWatermark` tinyint(1) DEFAULT '0',
                                 `isVR` tinyint(1) DEFAULT '0',
                                 PRIMARY KEY (`imageID`),
                                 KEY `propertyID` (`propertyID`),
                                 CONSTRAINT `propertyimage_ibfk_1` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `propertyimage`
--

LOCK TABLES `propertyimage` WRITE;
/*!40000 ALTER TABLE `propertyimage` DISABLE KEYS */;
/*!40000 ALTER TABLE `propertyimage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
                          `reviewID` int NOT NULL AUTO_INCREMENT,
                          `studentID` int NOT NULL,
                          `propertyID` int NOT NULL,
                          `rating` int NOT NULL,
                          `comment` varchar(255) DEFAULT NULL,
                          `reviewDate` datetime DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (`reviewID`),
                          KEY `studentID` (`studentID`),
                          KEY `propertyID` (`propertyID`),
                          CONSTRAINT `review_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `students` (`studentID`),
                          CONSTRAINT `review_ibfk_2` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
UNLOCK TABLES;


--
-- Table structure for table `savedproperty`
--

DROP TABLE IF EXISTS `savedproperty`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `savedproperty` (
                                 `savedID` int NOT NULL AUTO_INCREMENT,
                                 `studentID` int NOT NULL,
                                 `propertyID` int NOT NULL,
                                 `savedStatus` tinyint(1) DEFAULT '1',
                                 PRIMARY KEY (`savedID`),
                                 KEY `studentID` (`studentID`),
                                 KEY `propertyID` (`propertyID`),
                                 CONSTRAINT `savedproperty_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `students` (`studentID`),
                                 CONSTRAINT `savedproperty_ibfk_2` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `savedproperty`
--

LOCK TABLES `savedproperty` WRITE;
/*!40000 ALTER TABLE `savedproperty` DISABLE KEYS */;
/*!40000 ALTER TABLE `savedproperty` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `searchfilter`
--

DROP TABLE IF EXISTS `searchfilter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `searchfilter` (
                                `searchID` int NOT NULL AUTO_INCREMENT,
                                `studentID` int NOT NULL,
                                `queryText` varchar(255) DEFAULT NULL,
                                `location` varchar(100) DEFAULT NULL,
                                `priceRange` varchar(50) DEFAULT NULL,
                                `propertyRange` varchar(50) DEFAULT NULL,
                                `distanceFromCampus` decimal(5,2) DEFAULT NULL,
                                `securityLevel` varchar(50) DEFAULT NULL,
                                `furnished` tinyint(1) DEFAULT NULL,
                                `petFriendly` tinyint(1) DEFAULT NULL,
                                `searchDate` datetime DEFAULT CURRENT_TIMESTAMP,
                                `resultsCount` int DEFAULT NULL,
                                PRIMARY KEY (`searchID`),
                                KEY `studentID` (`studentID`),
                                CONSTRAINT `searchfilter_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `students` (`studentID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `searchfilter`
--

LOCK TABLES `searchfilter` WRITE;
/*!40000 ALTER TABLE `searchfilter` DISABLE KEYS */;
/*!40000 ALTER TABLE `searchfilter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `students`
--

DROP TABLE IF EXISTS `students`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `students` (
                            `studentID` int NOT NULL,
                            `yearOfStudy` int DEFAULT NULL,
                            `budgetMin` decimal(38,2) DEFAULT NULL,
                            `budgetMax` decimal(38,2) DEFAULT NULL,
                            PRIMARY KEY (`studentID`),
                            CONSTRAINT `students_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `users` (`userID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (1,3,3000.00,6000.00),(2,2,1000.00,3500.00);
/*!40000 ALTER TABLE `students` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
                         `userID` int NOT NULL AUTO_INCREMENT,
                         `password` varchar(255) DEFAULT NULL,
                         `firstName` varchar(255) DEFAULT NULL,
                         `lastName` varchar(255) DEFAULT NULL,
                         `dateOfBirth` date DEFAULT NULL,
                         `email` varchar(255) DEFAULT NULL,
                         `phone` varchar(255) DEFAULT NULL,
                         `avatar` varchar(255) DEFAULT NULL,
                         `isActive` bit(1) DEFAULT NULL,
                         `warningCount` int DEFAULT NULL,
                         PRIMARY KEY (`userID`),
                         UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'KarPai','Ezile','Hlomane','2004-08-09','s227676637@mandela.ac.za','0678864663',NULL,NULL,NULL),(2,'123KarPai','Aya','Brown','2026-07-01','s22767663@mandela.ac.za','0678864663',NULL,NULL,NULL),(3,'azania@gmail','azania','Moses','2026-07-01','azania@gmail.com','0678864663',NULL,NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-04 10:01:14

USE ulee_db;

-- ─────────────────────────────────────────────
-- 1. USERS (landlords) — userID 4, 5, 6 (next after existing AUTO_INCREMENT=4)
--    Passwords are BCrypt-hashed. Landlords type their plain password below when logging in.
-- ─────────────────────────────────────────────
INSERT INTO users (userID, password, firstName, lastName, dateOfBirth, email, phone, avatar, isActive, warningCount) VALUES
                                                                                                                         (4, '$2b$10$AE1vUHgSoeusahFvPd7Zs.FyPOIQTJM0AufLIfxksmYASjDiXmvhm', 'Sipho', 'Job', NULL, 'siphojob@gmail.com', NULL, NULL, b'1', 0),
                                                                                                                         (5, '$2b$10$PjmwoGkJe3.dYohoG6yFgOdIsWb3ATQdge0IEcgxCEuyG0b2UsmKe', 'Karabelo', 'Isaac', NULL, 'hlmezi001@gmail.com', NULL, NULL, b'1', 0),
                                                                                                                         (6, '$2b$10$tYTucwgrV5PgGA.byEeqku1nXfGAO9SHjFvCzfhuF5ZArcXgFFNAq', 'Ezile', 'Hlomane', NULL, 'ezile.ulee@gmail.com', NULL, NULL, b'1', 0);



-- Plaintext reference for you only — landlords only ever type these, never the hash above:
--   siphojob@gmail.com   / siphoj
--   hlmezi001@gmail.com  / hlmezi001
--   ezile.ulee@gmail.com / KarPai85

-- ─────────────────────────────────────────────
-- 2. LANDLORDS — landlordID must equal the matching users.userID
-- ─────────────────────────────────────────────
INSERT INTO landlords (landlordID, companyName, propertiesCount) VALUES
                                                                     (4, 'The Dunes', 1),
                                                                     (5, 'The Gomery', 1),
                                                                     (6, 'The admiralty', 1);

-- ─────────────────────────────────────────────
-- 3. PROPERTIES — propertyID 1, 2, 3 (table was empty)
--    All singles, 1 bed / 1 bath, Summerstrand, prices in the R2,000–R3,500 range as agreed
-- ─────────────────────────────────────────────
INSERT INTO property
(propertyID, landlordID, title, description, rent, deposit, address, city, municipality, suburb,
 latitude, longitude, type, bedrooms, bathrooms, area, furnished, studyFriendly, isAvailable,
 availableFrom, distanceFromUniversity, rating, reviewCount, isReported, reportReason, status)
VALUES
    (1, 4, 'The Dunes',
     'A quiet single-room stay in Summerstrand, close to campus and the beach.',
     2800.00, 2800.00, '69 Zenios Place Nelson Mandela Bay', 'Summerstrand', 'Nelson Mandela Bay', 'Summerstrand',
     NULL, NULL, 'Single Room', 1, 1, NULL, 1, 1, 1,
     CURDATE(), NULL, NULL, 0, b'0', NULL, 'Active'),

    (2, 5, 'The Gomery',
     'Comfortable single room right by campus in Summerstrand.',
     2500.00, 2500.00, 'Gomery Avenue, Nelson Mandela Metropolitan University, Nelson Mandela Bay, 6031', 'Summerstrand', 'Nelson Mandela Bay', 'Summerstrand',
     NULL, NULL, 'Single Room', 1, 1, NULL, 1, 1, 1,
     CURDATE(), NULL, NULL, 0, b'0', NULL, 'Active'),

    (3, 6, 'The admiralty',
     'Modern single room in a secure Summerstrand residence.',
     3200.00, 3200.00, '12 Admiralty Way, Gqeberha, Nelson Mandela Bay, 6001', 'Summerstrand', 'Nelson Mandela Bay', 'Summerstrand',
     NULL, NULL, 'Single Room', 1, 1, NULL, 1, 1, 1,
     CURDATE(), NULL, NULL, 0, b'0', NULL, 'Active');

-- ─────────────────────────────────────────────
-- 4. PROPERTY IMAGES — main.png first (isMain = 1), rest in displayOrder
--    URLs point to the uploads folder exactly as it exists in your project
-- ─────────────────────────────────────────────
INSERT INTO propertyimage (propertyID, url, category, caption, isMain, displayOrder, hasWatermark, isVR) VALUES
-- Dunes
(1, '/uploads/Dunes/main.png',   'exterior', 'The Dunes — front view', 1, 0, 0, 0),
(1, '/uploads/Dunes/11dune1.png', NULL, NULL, 0, 1, 0, 0),
(1, '/uploads/Dunes/11dune2.png', NULL, NULL, 0, 2, 0, 0),
(1, '/uploads/Dunes/11dune3.png', NULL, NULL, 0, 3, 0, 0),
(1, '/uploads/Dunes/11dune4.png', NULL, NULL, 0, 4, 0, 0),

-- Gomery
(2, '/uploads/Gomery/main.png',    'exterior', 'The Gomery — front view', 1, 0, 0, 0),
(2, '/uploads/Gomery/gomery1.png', NULL, NULL, 0, 1, 0, 0),
(2, '/uploads/Gomery/gomery2.png', NULL, NULL, 0, 2, 0, 0),
(2, '/uploads/Gomery/gomery3.png', NULL, NULL, 0, 3, 0, 0),
(2, '/uploads/Gomery/gomery4.png', NULL, NULL, 0, 4, 0, 0),

-- admiralty
(3, '/uploads/admiralty/main.png',        'exterior', 'The admiralty — front view', 1, 0, 0, 0),
(3, '/uploads/admiralty/admiralty1.png',  NULL, NULL, 0, 1, 0, 0),
(3, '/uploads/admiralty/admiralty2.png',  NULL, NULL, 0, 2, 0, 0),
(3, '/uploads/admiralty/admiralty3.png',  NULL, NULL, 0, 3, 0, 0),
(3, '/uploads/admiralty/admiralty4.png',  NULL, NULL, 0, 4, 0, 0),
(3, '/uploads/admiralty/admiralty10.png', NULL, NULL, 0, 5, 0, 0);

-- ─────────────────────────────────────────────
-- 5. AMENITIES & PROPERTY FEATURES (landlord module — edit-property page)
--    Run this whole file top-to-bottom on a fresh database and everything is set up.
--    If you already have ulee_db loaded and are just adding this new section,
--    running the whole file again is still safe — every table uses DROP TABLE IF EXISTS
--    above, and this section only creates tables that don't already exist elsewhere.
-- ─────────────────────────────────────────────

-- Amenities: fixed checklist, grouped by category for the edit-property page UI
DROP TABLE IF EXISTS property_amenity;
DROP TABLE IF EXISTS amenity;
CREATE TABLE amenity (
                         amenityID INT NOT NULL AUTO_INCREMENT,
                         name VARCHAR(100) NOT NULL,
                         category VARCHAR(50) NOT NULL,   -- groups checkboxes on screen, e.g. "Room", "Utilities", "Facilities"
                         PRIMARY KEY (amenityID),
                         UNIQUE KEY name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE property_amenity (
                                  propertyID INT NOT NULL,
                                  amenityID INT NOT NULL,
                                  PRIMARY KEY (propertyID, amenityID),
                                  KEY amenityID (amenityID),
                                  CONSTRAINT property_amenity_ibfk_1 FOREIGN KEY (propertyID) REFERENCES property (propertyID),
                                  CONSTRAINT property_amenity_ibfk_2 FOREIGN KEY (amenityID) REFERENCES amenity (amenityID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO amenity (name, category) VALUES
-- Room
('Furnished', 'Room'),
('Study Desk & Chair', 'Room'),
('Private Fridge', 'Room'),
('Panel Heater', 'Room'),
('Study Lamp', 'Room'),
('Wardrobe', 'Room'),
-- Kitchen & bathroom
('Private Kitchen', 'Kitchen & Bathroom'),
('Shared Kitchen', 'Kitchen & Bathroom'),
('Ensuite Bathroom', 'Kitchen & Bathroom'),
('Shared Bathroom', 'Kitchen & Bathroom'),
-- Utilities included
('Uncapped Wi-Fi', 'Utilities'),
('Unlimited Electricity', 'Utilities'),
('Unlimited Laundry Cycles', 'Utilities'),
-- Facilities
('Flatscreen TV', 'Facilities'),
('Gym', 'Facilities'),
('Parking', 'Facilities'),
('24/7 Security', 'Facilities');

-- Special features: landlord-authored extras (Study Hub, Braai Area, Game Room, etc.)
-- Free-text name, not a fixed list, each with its own photo gallery
DROP TABLE IF EXISTS property_feature_image;
DROP TABLE IF EXISTS property_feature;
CREATE TABLE property_feature (
                                  featureID INT NOT NULL AUTO_INCREMENT,
                                  propertyID INT NOT NULL,
                                  name VARCHAR(150) NOT NULL,          -- e.g. "Study Hub", "Braai Area"
                                  description VARCHAR(255) DEFAULT NULL,
                                  displayOrder INT DEFAULT NULL,
                                  PRIMARY KEY (featureID),
                                  KEY propertyID (propertyID),
                                  CONSTRAINT property_feature_ibfk_1 FOREIGN KEY (propertyID) REFERENCES property (propertyID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE property_feature_image (
                                        imageID INT NOT NULL AUTO_INCREMENT,
                                        featureID INT NOT NULL,
                                        url VARCHAR(255) NOT NULL,
                                        displayOrder INT DEFAULT NULL,
                                        PRIMARY KEY (imageID),
                                        KEY featureID (featureID),
                                        CONSTRAINT property_feature_image_ibfk_1 FOREIGN KEY (featureID) REFERENCES property_feature (featureID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;




-- 3. Optional: if you want to double check nothing got missed,
--    run this afterward to see current status/capacity per property.
SELECT propertyID, title, status, isAvailable, capacity
FROM property
ORDER BY propertyID;

SET SQL_SAFE_UPDATES = 0;

-- Normalize any status your app doesn't actually use (Active, NULL, etc.)
-- into Approved — leaves Draft and Inactive alone on purpose.
UPDATE property
SET status = 'Approved', isAvailable = 1
WHERE status NOT IN ('Draft', 'Inactive', 'Approved');

-- Fix capacity for anything still stuck at the old default
UPDATE property
SET capacity = 5
WHERE capacity IS NULL OR capacity = 1;

-- Full picture, including which landlord owns each row
SELECT propertyID, landlordID, title, status, isAvailable, capacity
FROM property
ORDER BY propertyID;