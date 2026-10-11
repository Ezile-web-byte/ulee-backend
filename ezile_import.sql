CREATE DATABASE  IF NOT EXISTS `ulee_db_ezile` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `ulee_db_ezile`;
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
-- Table structure for table `amenity`
--

DROP TABLE IF EXISTS `amenity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `amenity` (
  `amenityID` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`amenityID`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `amenity`
--

LOCK TABLES `amenity` WRITE;
/*!40000 ALTER TABLE `amenity` DISABLE KEYS */;
INSERT INTO `amenity` VALUES (1,'Furnished','Room'),(2,'Study Desk & Chair','Room'),(3,'Private Fridge','Room'),(4,'Panel Heater','Room'),(5,'Study Lamp','Room'),(6,'Wardrobe','Room'),(7,'Private Kitchen','Kitchen & Bathroom'),(8,'Shared Kitchen','Kitchen & Bathroom'),(9,'Ensuite Bathroom','Kitchen & Bathroom'),(10,'Shared Bathroom','Kitchen & Bathroom'),(11,'Uncapped Wi-Fi','Utilities'),(12,'Unlimited Electricity','Utilities'),(13,'Unlimited Laundry Cycles','Utilities'),(14,'Flatscreen TV','Facilities'),(15,'Gym','Facilities'),(16,'Parking','Facilities'),(17,'24/7 Security','Facilities');
/*!40000 ALTER TABLE `amenity` ENABLE KEYS */;
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
  `fundingStatus` varchar(255) DEFAULT NULL,
  `messageToLandlord` varchar(255) DEFAULT NULL,
  `landlordResponded` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`applicationID`),
  KEY `studentID` (`studentID`),
  KEY `propertyID` (`propertyID`),
  CONSTRAINT `application_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `students` (`studentID`),
  CONSTRAINT `application_ibfk_2` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `application`
--

LOCK TABLES `application` WRITE;
/*!40000 ALTER TABLE `application` DISABLE KEYS */;
INSERT INTO `application` VALUES (1,2,2,'Accepted','2026-08-29 05:06:57','NSFAS','I would love to stay here, close to campus and great reviews.',0),(2,3,2,'Pending','2026-08-29 05:06:57','Self-funded','Looking for a quiet place to study, hope to hear back soon.',0),(3,6,3,'Accepted','2026-09-04 23:09:33',NULL,NULL,NULL),(4,6,4,'Pending','2026-09-05 00:41:03',NULL,NULL,NULL),(5,6,7,'Pending','2026-09-05 09:15:57','Self-funded','',NULL),(6,7,2,'Pending','2026-09-05 09:27:32','Self-funded','',NULL),(7,7,3,'Pending','2026-09-05 09:39:22','NSFAS','',NULL),(8,7,4,'Pending','2026-09-05 09:40:47','Bursary','',NULL),(9,6,10,'Accepted','2026-09-05 11:28:19','Self-funded','',NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `application_document`
--

LOCK TABLES `application_document` WRITE;
/*!40000 ALTER TABLE `application_document` DISABLE KEYS */;
INSERT INTO `application_document` VALUES (1,1,'proof_of_registration.txt','C:\\Users\\Ezile\\Downloads\\2026\\unakho\\ulee-backend\\uploads\\1_test_proof_of_registration.txt','2026-08-29 10:43:52'),(2,5,'Assignment 2.zip','C:\\Users\\Ezile\\Downloads\\2026\\unakho\\ulee-backend\\uploads\\5_1788592557112_Assignment 2.zip','2026-09-05 09:15:57'),(3,9,'Assignment 2.zip','C:\\Users\\Ezile\\Downloads\\2026\\unakho\\ulee-backend\\uploads\\9_1788600499374_Assignment 2.zip','2026-09-05 11:28:20');
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
  `verified` bit(1) DEFAULT NULL,
  PRIMARY KEY (`landlordID`),
  CONSTRAINT `landlords_ibfk_1` FOREIGN KEY (`landlordID`) REFERENCES `users` (`userID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `landlords`
--

LOCK TABLES `landlords` WRITE;
/*!40000 ALTER TABLE `landlords` DISABLE KEYS */;
INSERT INTO `landlords` VALUES (1,'Hlomane Student Housing',37,NULL),(4,'metro',0,_binary '\0'),(5,'NMU',0,_binary '\0');
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
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `notificationID` int NOT NULL AUTO_INCREMENT,
  `createdAt` datetime(6) DEFAULT NULL,
  `isRead` bit(1) DEFAULT NULL,
  `landlordID` int DEFAULT NULL,
  `message` text,
  `propertyID` int DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`notificationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `panorama_hotspot`
--

DROP TABLE IF EXISTS `panorama_hotspot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `panorama_hotspot` (
  `hotspotID` int NOT NULL AUTO_INCREMENT,
  `label` varchar(255) DEFAULT NULL,
  `pitch` float DEFAULT NULL,
  `sourcePanoramaID` int DEFAULT NULL,
  `targetPanoramaID` int DEFAULT NULL,
  `yaw` float DEFAULT NULL,
  PRIMARY KEY (`hotspotID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `panorama_hotspot`
--

LOCK TABLES `panorama_hotspot` WRITE;
/*!40000 ALTER TABLE `panorama_hotspot` DISABLE KEYS */;
/*!40000 ALTER TABLE `panorama_hotspot` ENABLE KEYS */;
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
  `rent` decimal(38,2) DEFAULT NULL,
  `capacity` int NOT NULL DEFAULT '1',
  `status` varchar(255) DEFAULT NULL,
  `isAvailable` tinyint(1) DEFAULT '1',
  `distanceFromUniversity` decimal(38,2) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `description` text,
  `deposit` decimal(38,2) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `suburb` varchar(255) DEFAULT NULL,
  `latitude` decimal(38,2) DEFAULT NULL,
  `longitude` decimal(38,2) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `bathrooms` int DEFAULT NULL,
  `furnished` tinyint(1) DEFAULT '0',
  `availableFrom` date DEFAULT NULL,
  `rating` decimal(38,2) DEFAULT NULL,
  `reviewCount` int DEFAULT '0',
  `createdAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isReported` bit(1) DEFAULT NULL,
  `reportReason` varchar(255) DEFAULT NULL,
  `commuteType` varchar(255) DEFAULT NULL,
  `area` decimal(38,2) DEFAULT NULL,
  `bedrooms` int DEFAULT NULL,
  `municipality` varchar(255) DEFAULT NULL,
  `studyFriendly` bit(1) DEFAULT NULL,
  PRIMARY KEY (`propertyID`),
  KEY `landlordID` (`landlordID`),
  CONSTRAINT `property_ibfk_1` FOREIGN KEY (`landlordID`) REFERENCES `landlords` (`landlordID`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property`
--

LOCK TABLES `property` WRITE;
/*!40000 ALTER TABLE `property` DISABLE KEYS */;
INSERT INTO `property` VALUES (1,1,4500.00,4,'Inactive',0,NULL,'Gomery','Modern student residence, 5 min from campus.',4500.00,'12 Park Lane','Gqeberha','Summerstrand',NULL,NULL,'Sharing',2,0,'2027-01-01',NULL,0,'2026-08-29 04:59:52','2026-08-29 04:59:52',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(2,1,0.00,2,'Active',1,NULL,'The Dunes','Cozy single rooms near the beach.',0.00,'12 Park Lane','','Humewood',NULL,NULL,'Single Room',1,0,'2027-01-01',NULL,0,'2026-08-29 04:59:52','2026-08-29 04:59:52',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(3,1,3200.00,20,'Active',1,NULL,'Roch House','Simple. Safe. Student-Friendly. — Affordable accommodation in a comfortable environment you can call home.',3200.00,'17 Cardiff Street, Gqeberha, Nelson Mandela Bay, 6001','Gqeberha','On Campus',NULL,NULL,'Single Room',4,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(4,1,3400.00,22,'Active',1,NULL,'Summerstrand Student Village','Live Close, Live Better — Conveniently located accommodation that keeps you close to campus and essentials.',3400.00,'Campus Address 2','Gqeberha','On Campus',NULL,NULL,'Sharing',4,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(5,1,3300.00,25,'Active',1,NULL,'Sanlam Student Village','Move-In Ready — A well-maintained student space with everything you need to settle in quickly.',1000.00,'Campus Address 3','Gqeberha','On Campus',NULL,NULL,'Single Room',5,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(6,1,3600.00,28,'Active',1,NULL,'New Residence','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3600.00,'Campus Address 4','Gqeberha','On Campus',NULL,NULL,'Sharing',5,0,NULL,NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(7,1,3500.00,30,'Active',1,NULL,'Hector Pieterson Residence','Your Home Away From Home — Clean, comfortable, and designed to make student life easier.',3500.00,'Campus Address 5','Gqeberha','On Campus',NULL,NULL,'Single Room',6,0,'2027-07-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(8,1,4200.00,15,'Active',1,NULL,'15 Admiralty','Peaceful Student Living — Enjoy a quiet, welcoming space perfect for studying and relaxing.',4200.00,'Summerstrand Address 1','Gqeberha','Summerstrand',NULL,NULL,'Single Room',4,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(9,1,4000.00,22,'Active',1,NULL,'Ivana Drive','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',4000.00,'Summerstrand Address 2','Gqeberha','Summerstrand',NULL,NULL,'Sharing',5,0,NULL,NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(10,1,3800.00,4,'Active',1,NULL,'16 Cardiff','Your Home Away From Home — Clean, comfortable, and designed to make student life easier.',1000.00,'16 Cardiff Street, Gqeberha, Nelson Mandela Bay, 6001','Gqeberha','Summerstrand',NULL,NULL,'Commune',2,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(11,1,4300.00,25,'Active',1,NULL,'Sun Coast','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',4300.00,'Summerstrand Address 4','Gqeberha','Summerstrand',NULL,NULL,'Single Room',6,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(12,1,4100.00,28,'Active',1,NULL,'Campus Key','Modern Student Haven — Comfortable, affordable, and close to campus. Everything you need for stress-free student living.',4100.00,'Summerstrand Address 5','Gqeberha','Summerstrand',NULL,NULL,'Sharing',5,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(13,1,3900.00,22,'Active',1,NULL,'The Tide','Modern Student Haven — Comfortable, affordable, and close to campus. Everything you need for stress-free student living.',3900.00,'Humewood Address 1','Gqeberha','Humewood',NULL,NULL,'Sharing',5,0,NULL,NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(14,1,3700.00,5,'Active',1,NULL,'The wave','Modern Student Haven — Comfortable, affordable, and close to campus. Everything you need for stress-free student living.',3700.00,'Humewood Address 2','Gqeberha','Humewood',NULL,NULL,'Commune',2,0,NULL,NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(15,1,4000.00,25,'Active',1,NULL,'The Blue Sky','Modern Student Haven — Comfortable, affordable, and close to campus. Everything you need for stress-free student living.',4000.00,'Humewood Address 3','Gqeberha','Humewood',NULL,NULL,'Single Room',6,0,NULL,NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(16,1,3800.00,4,'Active',1,NULL,'Jordan River','Modern Student Haven — Comfortable, affordable, and close to campus. Everything you need for stress-free student living.',3800.00,'Humewood Address 4','Gqeberha','Humewood',NULL,NULL,'Commune',2,0,'2027-07-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(17,1,4100.00,20,'Active',1,NULL,'Tyre','Modern Student Haven — Comfortable, affordable, and close to campus. Everything you need for stress-free student living.',4100.00,'Humewood Address 5','Gqeberha','Humewood',NULL,NULL,'Sharing',4,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(18,1,3000.00,20,'Draft',1,NULL,'Mercator','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3000.00,'Town Address 1','Gqeberha','Town',NULL,NULL,'Single Room',4,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(19,1,3200.00,22,'Active',1,NULL,'Gen Z','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3200.00,'Town Address 2','Gqeberha','Town',NULL,NULL,'Sharing',5,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(20,1,3100.00,25,'Active',1,NULL,'The Regent','Your Home Away From Home — Clean, comfortable, and designed to make student life easier.',3100.00,'Town Address 3','Gqeberha','Town',NULL,NULL,'Single Room',5,0,NULL,NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(21,1,3300.00,28,'Active',1,NULL,'Warwick','Your Home Away From Home — Clean, comfortable, and designed to make student life easier.',3300.00,'Town Address 4','Gqeberha','Town',NULL,NULL,'Sharing',6,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(22,1,3000.00,30,'Active',1,NULL,'Cosmopolitan','Your Home Away From Home — Clean, comfortable, and designed to make student life easier.',3000.00,'Town Address 5','Gqeberha','Town',NULL,NULL,'Single Room',6,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(23,1,2900.00,20,'Active',1,NULL,'Winnie Mandela','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',2900.00,'North End Address 1','Gqeberha','North End',NULL,NULL,'Single Room',4,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(24,1,3000.00,22,'Active',1,NULL,'Albert Luthuli','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3000.00,'North End Address 2','Gqeberha','North End',NULL,NULL,'Sharing',5,0,'2027-07-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(25,1,3100.00,25,'Active',1,NULL,'Khaya Lam','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3100.00,'North End Address 3','Gqeberha','North End',NULL,NULL,'Single Room',5,0,'2027-07-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(26,1,2950.00,28,'Active',1,NULL,'New Bright','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',2950.00,'North End Address 4','Gqeberha','North End',NULL,NULL,'Sharing',6,0,NULL,NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(27,1,3050.00,30,'Active',1,NULL,'Salt River','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3050.00,'North End Address 5','Gqeberha','North End',NULL,NULL,'Single Room',6,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(28,1,3300.00,20,'Active',1,NULL,'Sunrise','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3300.00,'Central Address 1','Gqeberha','Central',NULL,NULL,'Single Room',4,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(29,1,3400.00,22,'Active',1,NULL,'Bird Street','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3400.00,'Central Address 2','Gqeberha','Central',NULL,NULL,'Sharing',5,0,'2027-07-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(30,1,3500.00,25,'Active',1,NULL,'Amazing Grace','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3500.00,'Central Address 3','Gqeberha','Central',NULL,NULL,'Single Room',5,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(31,1,3350.00,28,'Active',1,NULL,'Eden','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3350.00,'Central Address 4','Gqeberha','Central',NULL,NULL,'Sharing',6,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(32,1,12.00,30,'Pending',1,NULL,'The Shark','Affordable & Comfortable — Quality student accommodation with convenience, comfort, and great value.',3450.00,'Central Address 5','Gqeberha','Central',NULL,NULL,'Single Room',6,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(33,1,4500.00,20,'Active',1,NULL,'The fields','Study. Relax. Repeat. — A comfortable student-friendly home with a peaceful environment for studying.',4500.00,'Pier 14 Address 1','Gqeberha','Pier 14',NULL,NULL,'Single Room',4,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(34,1,4600.00,22,'Active',1,NULL,'GreenFields Student Village','Study. Relax. Repeat. — A comfortable student-friendly home with a peaceful environment for studying.',4600.00,'Pier 14 Address 2','Gqeberha','Pier 14',NULL,NULL,'Sharing',5,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(35,1,4700.00,25,'Active',1,NULL,'Titanic Student Living','Study. Relax. Repeat. — A comfortable student-friendly home with a peaceful environment for studying.',4700.00,'Pier 14 Address 3','Gqeberha','Pier 14',NULL,NULL,'Single Room',5,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(36,1,4550.00,28,'Active',1,NULL,'Prime Student Living','Study. Relax. Repeat. — A comfortable student-friendly home with a peaceful environment for studying.',4550.00,'Pier 14 Address 4','Gqeberha','Pier 14',NULL,NULL,'Sharing',6,0,'2027-01-01',NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(37,1,4650.00,30,'Active',1,NULL,'Castle House','Placeholder listing ÔÇö edit title, description, and photos.',4650.00,'Pier 14 Address 5','Gqeberha','Pier 14',NULL,NULL,'Single Room',6,0,NULL,NULL,0,'2026-09-01 08:54:06','2026-09-01 08:54:06',NULL,NULL,'Shuttle required',NULL,NULL,NULL,NULL),(38,1,NULL,10,'Inactive',0,NULL,'ewr','',NULL,'','',NULL,NULL,NULL,'Single Room',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(39,1,NULL,12,'Inactive',0,NULL,'tt','',NULL,'','',NULL,NULL,NULL,'Commune',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'',NULL,NULL,NULL,NULL),(40,1,NULL,1,'Inactive',0,NULL,'dd','',NULL,'','',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'',NULL,NULL,NULL,NULL),(41,1,1000.00,1,'Inactive',0,NULL,'ansasa','Hello',100.00,' 168 Govan Mbeki Avenue, Central, Gqeberha','Town',NULL,NULL,NULL,'Single Room',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL),(42,1,NULL,1,'Inactive',0,NULL,'za','',NULL,'','',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'',NULL,NULL,NULL,NULL),(43,1,4800.00,10,'Inactive',0,NULL,'dad','sai',1000.00,'29 Ivana Place Nelson Mandela Bay 6001','Summerstrand',NULL,NULL,NULL,'Single Room',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'Walking distance',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `property` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_amenity`
--

DROP TABLE IF EXISTS `property_amenity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `property_amenity` (
  `propertyID` int NOT NULL,
  `amenityID` int NOT NULL,
  PRIMARY KEY (`propertyID`,`amenityID`),
  KEY `amenityID` (`amenityID`),
  CONSTRAINT `property_amenity_ibfk_1` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`),
  CONSTRAINT `property_amenity_ibfk_2` FOREIGN KEY (`amenityID`) REFERENCES `amenity` (`amenityID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_amenity`
--

LOCK TABLES `property_amenity` WRITE;
/*!40000 ALTER TABLE `property_amenity` DISABLE KEYS */;
INSERT INTO `property_amenity` VALUES (1,1),(2,1),(3,1),(5,1),(6,1),(7,1),(8,1),(9,1),(10,1),(11,1),(12,1),(14,1),(15,1),(16,1),(17,1),(18,1),(19,1),(20,1),(21,1),(22,1),(23,1),(24,1),(25,1),(26,1),(27,1),(29,1),(30,1),(31,1),(32,1),(35,1),(36,1),(37,1),(41,1),(43,1),(1,2),(2,2),(3,2),(4,2),(5,2),(6,2),(7,2),(8,2),(9,2),(10,2),(11,2),(12,2),(13,2),(14,2),(16,2),(18,2),(20,2),(21,2),(22,2),(23,2),(27,2),(31,2),(32,2),(33,2),(34,2),(35,2),(37,2),(41,2),(43,2),(15,3),(19,3),(26,3),(29,3),(30,3),(3,4),(16,4),(23,4),(32,4),(33,4),(34,4),(4,5),(5,5),(6,5),(8,5),(10,5),(11,5),(12,5),(13,5),(14,5),(15,5),(17,5),(18,5),(19,5),(20,5),(21,5),(22,5),(24,5),(25,5),(26,5),(27,5),(28,5),(29,5),(30,5),(31,5),(33,5),(35,5),(36,5),(37,5),(43,5),(2,6),(4,6),(5,6),(6,6),(8,6),(10,6),(12,6),(13,6),(22,6),(25,6),(34,6),(35,6),(37,6),(12,7),(17,7),(23,7),(24,7),(25,7),(27,7),(32,7),(33,7),(36,7),(1,8),(2,8),(3,8),(4,8),(5,8),(6,8),(7,8),(8,8),(9,8),(10,8),(11,8),(13,8),(14,8),(15,8),(16,8),(18,8),(19,8),(20,8),(21,8),(22,8),(23,8),(26,8),(27,8),(29,8),(30,8),(31,8),(33,8),(34,8),(35,8),(36,8),(37,8),(41,8),(43,8),(2,9),(3,9),(11,9),(12,9),(16,9),(17,9),(19,9),(24,9),(25,9),(26,9),(32,9),(1,10),(4,10),(5,10),(6,10),(7,10),(8,10),(9,10),(10,10),(13,10),(14,10),(15,10),(18,10),(20,10),(21,10),(22,10),(29,10),(30,10),(31,10),(34,10),(35,10),(37,10),(41,10),(43,10),(1,11),(2,11),(3,11),(4,11),(5,11),(6,11),(7,11),(9,11),(10,11),(11,11),(12,11),(14,11),(15,11),(16,11),(17,11),(18,11),(19,11),(20,11),(21,11),(24,11),(26,11),(27,11),(28,11),(29,11),(30,11),(31,11),(33,11),(34,11),(35,11),(36,11),(37,11),(41,11),(43,11),(1,12),(2,12),(3,12),(4,12),(5,12),(6,12),(7,12),(8,12),(9,12),(10,12),(11,12),(12,12),(14,12),(17,12),(18,12),(19,12),(20,12),(21,12),(22,12),(23,12),(24,12),(25,12),(26,12),(27,12),(30,12),(31,12),(32,12),(33,12),(34,12),(35,12),(36,12),(37,12),(41,12),(43,12),(1,13),(3,13),(4,13),(8,13),(10,13),(12,13),(15,13),(16,13),(18,13),(20,13),(21,13),(22,13),(25,13),(27,13),(34,13),(35,13),(37,13),(1,14),(2,14),(3,14),(4,14),(8,14),(10,14),(11,14),(12,14),(14,14),(15,14),(16,14),(17,14),(19,14),(20,14),(21,14),(22,14),(25,14),(28,14),(33,14),(34,14),(35,14),(36,14),(37,14),(41,14),(43,14),(12,15),(15,15),(2,16),(5,16),(9,16),(12,16),(13,16),(14,16),(16,16),(18,16),(23,16),(24,16),(26,16),(27,16),(29,16),(30,16),(31,16),(32,16),(41,16),(1,17),(2,17),(3,17),(4,17),(5,17),(6,17),(7,17),(8,17),(9,17),(10,17),(11,17),(12,17),(13,17),(17,17),(18,17),(19,17),(20,17),(21,17),(22,17),(23,17),(24,17),(25,17),(26,17),(27,17),(28,17),(29,17),(30,17),(31,17),(32,17),(33,17),(34,17),(35,17),(36,17),(37,17),(43,17);
/*!40000 ALTER TABLE `property_amenity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_feature`
--

DROP TABLE IF EXISTS `property_feature`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `property_feature` (
  `featureID` int NOT NULL AUTO_INCREMENT,
  `propertyID` int NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `displayOrder` int DEFAULT NULL,
  PRIMARY KEY (`featureID`),
  KEY `propertyID` (`propertyID`),
  CONSTRAINT `property_feature_ibfk_1` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_feature`
--

LOCK TABLES `property_feature` WRITE;
/*!40000 ALTER TABLE `property_feature` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_feature` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_feature_image`
--

DROP TABLE IF EXISTS `property_feature_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `property_feature_image` (
  `imageID` int NOT NULL AUTO_INCREMENT,
  `featureID` int NOT NULL,
  `url` varchar(255) NOT NULL,
  `displayOrder` int DEFAULT NULL,
  PRIMARY KEY (`imageID`),
  KEY `featureID` (`featureID`),
  CONSTRAINT `property_feature_image_ibfk_1` FOREIGN KEY (`featureID`) REFERENCES `property_feature` (`featureID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_feature_image`
--

LOCK TABLES `property_feature_image` WRITE;
/*!40000 ALTER TABLE `property_feature_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_feature_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_panorama`
--

DROP TABLE IF EXISTS `property_panorama`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `property_panorama` (
  `panoramaID` int NOT NULL AUTO_INCREMENT,
  `imageID` int DEFAULT NULL,
  `isEntryPoint` bit(1) DEFAULT NULL,
  `propertyID` int DEFAULT NULL,
  `roomName` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`panoramaID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_panorama`
--

LOCK TABLES `property_panorama` WRITE;
/*!40000 ALTER TABLE `property_panorama` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_panorama` ENABLE KEYS */;
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
  `isMain` tinyint(1) DEFAULT '0',
  `displayOrder` int DEFAULT NULL,
  `uploadedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `hasWatermark` tinyint(1) DEFAULT '0',
  `isVR` tinyint(1) DEFAULT '0',
  `caption` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`imageID`),
  KEY `propertyID` (`propertyID`),
  CONSTRAINT `propertyimage_ibfk_1` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB AUTO_INCREMENT=115 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `propertyimage`
--

LOCK TABLES `propertyimage` WRITE;
/*!40000 ALTER TABLE `propertyimage` DISABLE KEYS */;
INSERT INTO `propertyimage` VALUES (7,1,'/uploads/1_1788246447510_main.png','exterior',1,1,'2026-09-01 09:07:27',NULL,NULL,NULL),(8,1,'/uploads/1_1788246478798_gomery4.png','exterior',0,2,'2026-09-01 09:07:58',NULL,NULL,NULL),(9,1,'/uploads/1_1788246478815_gomery3.png','exterior',0,3,'2026-09-01 09:07:58',NULL,NULL,NULL),(10,1,'/uploads/1_1788246478834_gomery2.png','exterior',0,4,'2026-09-01 09:07:58',NULL,NULL,NULL),(12,2,'/uploads/2_1788246615057_main.png','exterior',1,1,'2026-09-01 09:10:15',NULL,NULL,NULL),(13,2,'/uploads/2_1788246647866_11dune4.png','exterior',0,2,'2026-09-01 09:10:47',NULL,NULL,NULL),(14,2,'/uploads/2_1788246647898_11dune3.png','exterior',0,3,'2026-09-01 09:10:47',NULL,NULL,NULL),(15,2,'/uploads/2_1788246647923_11dune2.png','exterior',0,4,'2026-09-01 09:10:47',NULL,NULL,NULL),(16,2,'/uploads/2_1788246647940_11dune1.png','exterior',0,5,'2026-09-01 09:10:47',NULL,NULL,NULL),(17,3,'/uploads/3_1788246813166_FRONT22.png','exterior',1,1,'2026-09-01 09:13:33',NULL,NULL,NULL),(18,3,'/uploads/3_1788247096565_F1.png','exterior',0,2,'2026-09-01 09:18:16',NULL,NULL,NULL),(19,3,'/uploads/3_1788247096590_F2.png','exterior',0,3,'2026-09-01 09:18:16',NULL,NULL,NULL),(20,3,'/uploads/3_1788247096616_F3.png','exterior',0,4,'2026-09-01 09:18:16',NULL,NULL,NULL),(23,4,'/uploads/4_1788247395019_ssv1.png','exterior',0,3,'2026-09-01 09:23:15',NULL,NULL,NULL),(24,4,'/uploads/4_1788247395038_ssv2.png','exterior',0,4,'2026-09-01 09:23:15',NULL,NULL,NULL),(25,4,'/uploads/4_1788247473187_landry.png','exterior',0,3,'2026-09-01 09:24:33',NULL,NULL,NULL),(26,4,'/uploads/4_1788247473213_studyROOM.png','exterior',0,4,'2026-09-01 09:24:33',NULL,NULL,NULL),(27,4,'/uploads/4_1788247530898_bed5.png','exterior',0,5,'2026-09-01 09:25:30',NULL,NULL,NULL),(28,4,'/uploads/4_1788247582321_bathroom.png','exterior',0,6,'2026-09-01 09:26:22',NULL,NULL,NULL),(29,5,'/uploads/5_1788248752279_SSVFRONT.png','exterior',1,1,'2026-09-01 09:45:52',NULL,NULL,NULL),(30,5,'/uploads/5_1788248805302_SSVchill.png','exterior',0,2,'2026-09-01 09:46:45',NULL,NULL,NULL),(31,5,'/uploads/5_1788248805319_SSVstudyArea.png','exterior',0,3,'2026-09-01 09:46:45',NULL,NULL,NULL),(32,5,'/uploads/5_1788248805338_SSVGate.png','exterior',0,4,'2026-09-01 09:46:45',NULL,NULL,NULL),(33,5,'/uploads/5_1788248805370_SSVKitchen.png','exterior',0,5,'2026-09-01 09:46:45',NULL,NULL,NULL),(34,6,'/uploads/6_1788248906091_NewFRONT.png','exterior',1,1,'2026-09-01 09:48:26',NULL,NULL,NULL),(35,7,'/uploads/7_1788249136683_FRONT.png','exterior',1,1,'2026-09-01 09:52:16',NULL,NULL,NULL),(41,8,'/uploads/8_1788250165888_ADFRONT.png','exterior',1,1,'2026-09-01 10:09:25',NULL,NULL,NULL),(42,9,'/uploads/9_1788250599197_Ivana Drive.png','exterior',1,1,'2026-09-01 10:16:39',NULL,NULL,NULL),(43,9,'/uploads/9_1788250636070_Ivana1.png','exterior',0,2,'2026-09-01 10:17:16',NULL,NULL,NULL),(44,9,'/uploads/9_1788250636138_Ivana2.png','exterior',0,3,'2026-09-01 10:17:16',NULL,NULL,NULL),(45,9,'/uploads/9_1788250636158_Ivana3.png','exterior',0,4,'2026-09-01 10:17:16',NULL,NULL,NULL),(46,9,'/uploads/9_1788250636182_Ivana4.png','exterior',0,5,'2026-09-01 10:17:16',NULL,NULL,NULL),(47,9,'/uploads/9_1788250636205_Ivana5.png','exterior',0,6,'2026-09-01 10:17:16',NULL,NULL,NULL),(48,9,'/uploads/9_1788250636226_Ivana6.png','exterior',0,7,'2026-09-01 10:17:16',NULL,NULL,NULL),(49,9,'/uploads/9_1788250636250_Ivana7.png','exterior',0,8,'2026-09-01 10:17:16',NULL,NULL,NULL),(50,9,'/uploads/9_1788250636273_Ivana8.png','exterior',0,9,'2026-09-01 10:17:16',NULL,NULL,NULL),(52,10,'/uploads/10_1788259602581_16CardiffFRONT.png','exterior',1,1,'2026-09-01 12:46:42',NULL,NULL,NULL),(53,10,'/uploads/10_1788259656500_cardiffSITDOWN.png','exterior',0,2,'2026-09-01 12:47:36',NULL,NULL,NULL),(54,10,'/uploads/10_1788259656522_cardiffKitchen.png','exterior',0,3,'2026-09-01 12:47:36',NULL,NULL,NULL),(55,10,'/uploads/10_1788259656546_cardiffBathroom.png','exterior',0,4,'2026-09-01 12:47:36',NULL,NULL,NULL),(56,10,'/uploads/10_1788259656565_bathroom1Cardiff.png','exterior',0,5,'2026-09-01 12:47:36',NULL,NULL,NULL),(57,10,'/uploads/10_1788259656631_cardiffBEDROOM2.png','exterior',0,6,'2026-09-01 12:47:36',NULL,NULL,NULL),(58,10,'/uploads/10_1788259656649_cardiffBEDROOM1.png','exterior',0,7,'2026-09-01 12:47:36',NULL,NULL,NULL),(60,10,'/uploads/10_1788259656688_cardiif.png','exterior',0,9,'2026-09-01 12:47:36',NULL,NULL,NULL),(61,13,'/uploads/13_1788263809639_humeHoodFRont1.png','exterior',1,1,'2026-09-01 13:56:49',NULL,NULL,NULL),(62,14,'/uploads/14_1788263944947_humeHoodFRont2.png','exterior',1,1,'2026-09-01 13:59:04',NULL,NULL,NULL),(63,15,'/uploads/15_1788264018632_humeHoodFRont3.png','exterior',1,1,'2026-09-01 14:00:18',NULL,NULL,NULL),(64,16,'/uploads/16_1788264098133_humeHoodFRont4.png','exterior',1,1,'2026-09-01 14:01:38',NULL,NULL,NULL),(65,17,'/uploads/17_1788264189694_humeHoodFRont5.png','exterior',1,1,'2026-09-01 14:03:09',NULL,NULL,NULL),(66,23,'/uploads/23_1788264303623_northENDFront1.png','exterior',1,1,'2026-09-01 14:05:03',NULL,NULL,NULL),(67,24,'/uploads/24_1788264366628_northENDFRONT2.png','exterior',1,1,'2026-09-01 14:06:06',NULL,NULL,NULL),(68,25,'/uploads/25_1788264440569_northENDfront3.png','exterior',1,1,'2026-09-01 14:07:20',NULL,NULL,NULL),(69,26,'/uploads/26_1788264507740_northENDfront4.png','exterior',1,1,'2026-09-01 14:08:27',NULL,NULL,NULL),(70,27,'/uploads/27_1788264596462_northENDfront5.png','exterior',1,1,'2026-09-01 14:09:56',NULL,NULL,NULL),(71,28,'/uploads/28_1788264681387_centralFRONT.png','exterior',1,1,'2026-09-01 14:11:21',NULL,NULL,NULL),(72,29,'/uploads/29_1788264751034_centralFRONT2.png','exterior',1,1,'2026-09-01 14:12:31',NULL,NULL,NULL),(73,30,'/uploads/30_1788264817313_centralFRONT3.png','exterior',1,1,'2026-09-01 14:13:37',NULL,NULL,NULL),(74,31,'/uploads/31_1788264948731_centralFRONT4.png','exterior',1,1,'2026-09-01 14:15:48',NULL,NULL,NULL),(75,32,'/uploads/32_1788265027840_centralFRONT5.png','exterior',1,1,'2026-09-01 14:17:07',NULL,NULL,NULL),(76,18,'/uploads/18_1788265345258_MercatorFRONT.png','exterior',1,1,'2026-09-01 14:22:25',NULL,NULL,NULL),(77,18,'/uploads/18_1788265375864_mecartor1.png','exterior',0,2,'2026-09-01 14:22:55',NULL,NULL,NULL),(78,18,'/uploads/18_1788265375896_mercator2.png','exterior',0,3,'2026-09-01 14:22:55',NULL,NULL,NULL),(79,18,'/uploads/18_1788265375912_mercator3.png','exterior',0,4,'2026-09-01 14:22:55',NULL,NULL,NULL),(80,18,'/uploads/18_1788265375943_mercator4.png','exterior',0,5,'2026-09-01 14:22:55',NULL,NULL,NULL),(81,18,'/uploads/18_1788265375962_mercator5.png','exterior',0,6,'2026-09-01 14:22:55',NULL,NULL,NULL),(82,19,'/uploads/19_1788265586208_tebroFRONT.png','exterior',1,1,'2026-09-01 14:26:26',NULL,NULL,NULL),(83,19,'/uploads/19_1788265609784_kitchen4.png','exterior',0,2,'2026-09-01 14:26:49',NULL,NULL,NULL),(84,19,'/uploads/19_1788265609809_tebro1.png','exterior',0,3,'2026-09-01 14:26:49',NULL,NULL,NULL),(85,11,'/uploads/11_1788265772362_HUmewoodFRONT.png','exterior',1,1,'2026-09-01 14:29:32',NULL,NULL,NULL),(86,11,'/uploads/11_1788265804391_computerLab.png','exterior',0,2,'2026-09-01 14:30:04',NULL,NULL,NULL),(87,11,'/uploads/11_1788265804410_gym2.png','exterior',0,3,'2026-09-01 14:30:04',NULL,NULL,NULL),(88,11,'/uploads/11_1788265804452_HUmewoodFRONT.png','exterior',0,4,'2026-09-01 14:30:04',NULL,NULL,NULL),(89,11,'/uploads/11_1788265804477_HumewoodRoom.png','exterior',0,5,'2026-09-01 14:30:04',NULL,NULL,NULL),(90,11,'/uploads/11_1788265804511_studyROOM2.png','exterior',0,6,'2026-09-01 14:30:04',NULL,NULL,NULL),(91,12,'/uploads/12_1788266262692_campusKeyFront2.png','exterior',1,1,'2026-09-01 14:37:42',NULL,NULL,NULL),(92,12,'/uploads/12_1788266292503_bed4.png','exterior',0,2,'2026-09-01 14:38:12',NULL,NULL,NULL),(93,12,'/uploads/12_1788266292537_front.png','exterior',0,3,'2026-09-01 14:38:12',NULL,NULL,NULL),(94,12,'/uploads/12_1788266292562_gym2.png','exterior',0,4,'2026-09-01 14:38:12',NULL,NULL,NULL),(95,12,'/uploads/12_1788266292582_LAUNDRY3.png','exterior',0,5,'2026-09-01 14:38:12',NULL,NULL,NULL),(96,20,'/uploads/20_1788271081768_town11.png','exterior',1,1,'2026-09-01 15:58:01',NULL,NULL,NULL),(97,21,'/uploads/21_1788271202473_town5.png','exterior',1,1,'2026-09-01 16:00:02',NULL,NULL,NULL),(98,22,'/uploads/22_1788271263802_town4.png','exterior',1,1,'2026-09-01 16:01:03',NULL,NULL,NULL),(99,33,'/uploads/33_1788271543750_pier11.png','exterior',1,1,'2026-09-01 16:05:43',NULL,NULL,NULL),(100,34,'/uploads/34_1788271664473_pier12.png','exterior',1,1,'2026-09-01 16:07:44',NULL,NULL,NULL),(101,35,'/uploads/35_1788271754104_pier3.png','exterior',1,1,'2026-09-01 16:09:14',NULL,NULL,NULL),(102,36,'/uploads/36_1788271842269_pier4.png','exterior',1,1,'2026-09-01 16:10:42',NULL,NULL,NULL),(103,37,'/uploads/37_1788271982187_pier1.png','exterior',1,1,'2026-09-01 16:13:02',NULL,NULL,NULL),(104,41,'/uploads/41_1788607716963_bathroom.png','exterior',0,1,'2026-09-05 13:28:36',NULL,NULL,NULL),(105,41,'/uploads/41_1788607717004_chattingROOM.png','exterior',0,2,'2026-09-05 13:28:37',NULL,NULL,NULL),(106,41,'/uploads/41_1788607717024_FRONT1.png','exterior',0,3,'2026-09-05 13:28:37',NULL,NULL,NULL),(107,41,'/uploads/41_1788607717039_kitchen77.png','exterior',0,4,'2026-09-05 13:28:37',NULL,NULL,NULL),(108,41,'/uploads/41_1788607717053_ROOFTOP.png','exterior',0,5,'2026-09-05 13:28:37',NULL,NULL,NULL),(109,41,'/uploads/41_1788607717071_sharingROOM.png','exterior',0,6,'2026-09-05 13:28:37',NULL,NULL,NULL),(110,43,'/uploads/43_1788609813306_russel1.png','exterior',0,1,'2026-09-05 14:03:33',NULL,NULL,NULL),(111,43,'/uploads/43_1788609813543_russel2.png','exterior',0,2,'2026-09-05 14:03:33',NULL,NULL,NULL),(112,43,'/uploads/43_1788609813594_russel3.png','exterior',0,3,'2026-09-05 14:03:33',NULL,NULL,NULL),(113,43,'/uploads/43_1788609813632_russel4.png','exterior',0,4,'2026-09-05 14:03:33',NULL,NULL,NULL),(114,43,'/uploads/43_1788609813661_russel5.png','exterior',0,5,'2026-09-05 14:03:33',NULL,NULL,NULL);
/*!40000 ALTER TABLE `propertyimage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reports`
--

DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `reportID` int NOT NULL AUTO_INCREMENT,
  `description` text,
  `propertyID` int DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `reportedAt` datetime(6) DEFAULT NULL,
  `staffID` int DEFAULT NULL,
  `studentID` int DEFAULT NULL,
  PRIMARY KEY (`reportID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reports`
--

LOCK TABLES `reports` WRITE;
/*!40000 ALTER TABLE `reports` DISABLE KEYS */;
/*!40000 ALTER TABLE `reports` ENABLE KEYS */;
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
  `landlordResponse` varchar(255) DEFAULT NULL,
  `responseDate` datetime DEFAULT NULL,
  `isReported` bit(1) DEFAULT NULL,
  `reportReason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`reviewID`),
  KEY `studentID` (`studentID`),
  KEY `propertyID` (`propertyID`),
  CONSTRAINT `review_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `students` (`studentID`),
  CONSTRAINT `review_ibfk_2` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
INSERT INTO `review` VALUES (1,2,2,5,'Amazing place, walking distance to campus and the landlord is super responsive!','2026-08-29 05:07:17',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `reviewID` int NOT NULL AUTO_INCREMENT,
  `cleanlinessRating` int DEFAULT NULL,
  `comment` text,
  `isReported` bit(1) DEFAULT NULL,
  `landlordResponse` varchar(255) DEFAULT NULL,
  `propertyID` int DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `reportReason` varchar(255) DEFAULT NULL,
  `residencyStatus` varchar(255) DEFAULT NULL,
  `responseDate` datetime(6) DEFAULT NULL,
  `reviewDate` datetime(6) DEFAULT NULL,
  `safetyRating` int DEFAULT NULL,
  `studentID` int DEFAULT NULL,
  `studyAreaRating` int DEFAULT NULL,
  `wifiRating` int DEFAULT NULL,
  `yearOfStudy` int DEFAULT NULL,
  PRIMARY KEY (`reviewID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `savedproperty`
--

LOCK TABLES `savedproperty` WRITE;
/*!40000 ALTER TABLE `savedproperty` DISABLE KEYS */;
INSERT INTO `savedproperty` VALUES (1,2,2,1),(2,7,3,1),(3,8,3,1);
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
  `budgetMax` decimal(38,2) DEFAULT NULL,
  `budgetMin` decimal(38,2) DEFAULT NULL,
  `fundingStatus` varchar(255) DEFAULT NULL,
  `housingPreferences` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`studentID`),
  CONSTRAINT `students_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `users` (`userID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `students`
--

LOCK TABLES `students` WRITE;
/*!40000 ALTER TABLE `students` DISABLE KEYS */;
INSERT INTO `students` VALUES (2,2,NULL,NULL,NULL,NULL),(3,1,NULL,NULL,NULL,NULL),(6,2,NULL,NULL,NULL,NULL),(7,1,NULL,NULL,NULL,NULL),(8,NULL,NULL,NULL,NULL,NULL);
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
  `isActive` bit(1) DEFAULT NULL,
  `warningCount` int DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`userID`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'$2b$12$NhaYikUHyAYHnCQIOVQY/eORgUF.ZmrT57aQdcUxmyqWmItcCYoG2','Ezile','Hlomane',NULL,'ezile@gmail.com','0821234567',_binary '',0,'LANDLORD',NULL),(2,'$2b$12$NhaYikUHyAYHnCQIOVQY/eORgUF.ZmrT57aQdcUxmyqWmItcCYoG2','Lindiwe','Mahlangu',NULL,'student1@test.com','0827654321',_binary '',0,'STUDENT',NULL),(3,'$2b$12$NhaYikUHyAYHnCQIOVQY/eORgUF.ZmrT57aQdcUxmyqWmItcCYoG2','Sipho','Dube',NULL,'student2@test.com','0829998888',_binary '',0,'STUDENT',NULL),(4,'$2a$10$sTEYaQvpFJtsBYChWk5aA.S8UTKnxzEcnJRBlveBGbWGo/FL0MCiK','Ez','Hlomani','2004-07-09','ezile.ulee@gmail.com','0678864663',NULL,NULL,'LANDLORD',NULL),(5,'$2a$10$bHkWFNVPaK0oCwAP2Hvuu.eQOhat9uQSi9MSx/jHErRYMt9Rtb5zW','KB','Rabbit','2004-08-09','kb@gmail.com','0678864663',_binary '',0,'LANDLORD',NULL),(6,'$2a$10$UFmeyAV7MG.jMW4v0hae/ufuiddpZlrMogTM7JiXOTD6L27bJT4Ee','Elsa','Smith','2000-08-09','s228686637@mandela.ac.za','0768863772',_binary '',0,'STUDENT',NULL),(7,'$2a$10$0E6IWL4tkMNxoOg703b6c.dfGj4Yy2W3Nps9/nAlTiasUc3uZ3uii','Amahle','Hlomani','2000-10-09','s227678874@mandela.ac.za','0676676554',_binary '',0,'STUDENT',NULL),(8,'$2a$10$TFZlhskfmse8gx8f4cqGhexMRJGbiVd8CvLsmXAisBX2UHvjNwRvy','Anna','Jacob','2000-09-08','s227676637@mandela.ac.za','06092',_binary '',0,'STUDENT',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'ulee_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-12 17:32:17

