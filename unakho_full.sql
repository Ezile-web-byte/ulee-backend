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
-- Table structure for table `adminnotificationreads`
--

DROP TABLE IF EXISTS `adminnotificationreads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `adminnotificationreads` (
  `id` int NOT NULL AUTO_INCREMENT,
  `eventKey` varchar(100) NOT NULL,
  `readAt` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5ygn34mt83nrt2qf1t58bfbf6` (`eventKey`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `adminnotificationreads`
--

LOCK TABLES `adminnotificationreads` WRITE;
/*!40000 ALTER TABLE `adminnotificationreads` DISABLE KEYS */;
INSERT INTO `adminnotificationreads` VALUES (1,'report-6','2026-09-12 12:15:30.501752'),(2,'report-5','2026-09-12 12:15:30.621273'),(3,'report-4','2026-09-12 12:15:30.634277'),(4,'report-3','2026-09-12 12:15:30.646791'),(5,'warning-3','2026-09-12 12:15:30.658790'),(6,'report-1','2026-09-12 12:15:30.668793'),(7,'warning-2','2026-09-12 12:15:30.679792'),(8,'warning-1','2026-09-12 12:15:30.689791');
/*!40000 ALTER TABLE `adminnotificationreads` ENABLE KEYS */;
UNLOCK TABLES;

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
INSERT INTO `admins` VALUES (6);
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
  `landlordResponded` bit(1) DEFAULT NULL,
  `messageToLandlord` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`applicationID`),
  KEY `studentID` (`studentID`),
  KEY `propertyID` (`propertyID`),
  CONSTRAINT `application_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `students` (`studentID`),
  CONSTRAINT `application_ibfk_2` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `application`
--

LOCK TABLES `application` WRITE;
/*!40000 ALTER TABLE `application` DISABLE KEYS */;
INSERT INTO `application` VALUES (2,5,3,'Pending','2026-07-30 14:40:00',NULL,NULL,NULL),(4,5,2,'Rejected','2026-07-22 16:30:00',NULL,NULL,NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
INSERT INTO `landlords` VALUES (1,'Hlomane Properties',37,NULL),(2,'Mnguni Student Lets',2,NULL);
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
  `studentID` int DEFAULT NULL,
  PRIMARY KEY (`notificationID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,'2026-08-19 10:23:19.175782',_binary '\0',5,'Dear Karabelo Isaac,\n\nThis serves as an official warning regarding \"The Gomery\". The following complaint(s) have been raised:\n\n• Cold shower — hot water not working\n\nPlease address these issues promptly. Continued unresolved reports may result in suspension of this listing.',2,'Official Warning: The Gomery',NULL),(2,'2026-08-25 11:09:27.537034',_binary '\0',4,'Dear Sipho Job,\n\nThis serves as an official warning regarding \"The Dunes\". The following complaint(s) have been raised:\n\n• Cold shower\n\nPlease address these issues promptly. Continued unresolved reports may result in suspension of this listing.',1,'Official Warning: The Dunes',NULL),(3,'2026-09-04 00:15:40.802873',_binary '\0',1,'Dear Ezile Hlomane,\n\nThis serves as an official warning regarding \"Hlomane Riverside House\". The following complaint(s) have been raised:\n\n• No furniture / no hot water: Tenant reports the property has no furniture and no hot water.\n\nPlease address these issues promptly. Continued unresolved reports may result in suspension of this listing.',2,'Official Warning: Hlomane Riverside House',NULL),(4,'2026-09-12 12:52:50.439342',_binary '\0',NULL,'Dear Aisha Adams,\n\nYour ULEE account has been deactivated by an administrator. If you believe this was done in error, please contact support.',NULL,'Account Deactivated',4),(5,'2026-09-12 16:27:29.135480',_binary '\0',NULL,'Dear Aisha Adams,\n\nThis serves as an official warning from the ULEE admin team regarding your account. This is warning #1. Continued violations may result in your account being deactivated.',NULL,'Official Warning',4),(6,'2026-09-12 16:27:34.005401',_binary '\0',NULL,'Dear Luyanda Peter,\n\nYour ULEE account has been deactivated by an administrator. If you believe this was done in error, please contact support.',NULL,'Account Deactivated',5);
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
  `commuteType` varchar(255) DEFAULT NULL,
  `capacity` int DEFAULT NULL,
  PRIMARY KEY (`propertyID`),
  KEY `landlordID` (`landlordID`),
  CONSTRAINT `property_ibfk_1` FOREIGN KEY (`landlordID`) REFERENCES `landlords` (`landlordID`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property`
--

LOCK TABLES `property` WRITE;
/*!40000 ALTER TABLE `property` DISABLE KEYS */;
INSERT INTO `property` VALUES (2,1,'Hlomane Riverside House','Shared house, 4 bedrooms, walking distance to res.',3200.00,3200.00,'10 River Road','Gqeberha','Nelson Mandela Bay','Summerstrand',NULL,NULL,'Shared House',4,2,140.00,1,1,1,'2026-01-15',2.00,4.00,0,'2026-09-02 15:59:25','2026-09-04 01:43:34',_binary '\0',NULL,'Approved',NULL,NULL),(3,2,'The Hub Gqeberha','Purpose-built student block, 24h security guard.',3800.00,3800.00,'15 Strand Street','Gqeberha','Nelson Mandela Bay','Central',NULL,NULL,'Purpose-built Block',1,1,22.00,1,1,1,'2026-02-01',3.50,4.20,1,'2026-09-02 15:59:25','2026-09-04 00:48:05',_binary '\0',NULL,'Approved',NULL,NULL),(4,2,'Mnguni En-suite Rooms','Private en-suite room in a quiet complex.',4100.00,4100.00,'5 Park Lane','Gqeberha','Nelson Mandela Bay','Central',NULL,NULL,'En-suite Room',1,1,20.00,1,0,0,'2026-01-01',4.00,3.80,0,'2026-09-02 15:59:25','2026-09-02 15:59:25',NULL,NULL,'Approved',NULL,NULL),(7,1,'On Campus Residence 1','Placeholder listing — edit title, description, and photos.',3200.00,3200.00,'Campus Address 1','Gqeberha',NULL,'On Campus',NULL,NULL,'Single Room',NULL,4,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',20),(8,1,'On Campus Residence 2','Placeholder listing — edit title, description, and photos.',3400.00,3400.00,'Campus Address 2','Gqeberha',NULL,'On Campus',NULL,NULL,'Sharing',NULL,4,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',22),(9,1,'On Campus Residence 3','Placeholder listing — edit title, description, and photos.',3300.00,3300.00,'Campus Address 3','Gqeberha',NULL,'On Campus',NULL,NULL,'Single Room',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',25),(10,1,'On Campus Residence 4','Placeholder listing — edit title, description, and photos.',3600.00,3600.00,'Campus Address 4','Gqeberha',NULL,'On Campus',NULL,NULL,'Sharing',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',28),(11,1,'On Campus Residence 5','Placeholder listing — edit title, description, and photos.',3500.00,3500.00,'Campus Address 5','Gqeberha',NULL,'On Campus',NULL,NULL,'Single Room',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',30),(12,1,'Summerstrand Residence 1','Placeholder listing — edit title, description, and photos.',4200.00,4200.00,'Summerstrand Address 1','Gqeberha',NULL,'Summerstrand',NULL,NULL,'Single Room',NULL,4,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',20),(13,1,'Summerstrand Residence 2','Placeholder listing — edit title, description, and photos.',4000.00,4000.00,'Summerstrand Address 2','Gqeberha',NULL,'Summerstrand',NULL,NULL,'Sharing',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',22),(14,1,'Summerstrand Residence 3','Placeholder listing — edit title, description, and photos.',3800.00,3800.00,'Summerstrand Address 3','Gqeberha',NULL,'Summerstrand',NULL,NULL,'Commune',NULL,2,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',4),(15,1,'Summerstrand Residence 4','Placeholder listing — edit title, description, and photos.',4300.00,4300.00,'Summerstrand Address 4','Gqeberha',NULL,'Summerstrand',NULL,NULL,'Single Room',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',25),(16,1,'Summerstrand Residence 5','Placeholder listing — edit title, description, and photos.',4100.00,4100.00,'Summerstrand Address 5','Gqeberha',NULL,'Summerstrand',NULL,NULL,'Sharing',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',28),(17,1,'Humewood Residence 1','Placeholder listing — edit title, description, and photos.',3900.00,3900.00,'Humewood Address 1','Gqeberha',NULL,'Humewood',NULL,NULL,'Sharing',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',22),(18,1,'Humewood Residence 2','Placeholder listing — edit title, description, and photos.',3700.00,3700.00,'Humewood Address 2','Gqeberha',NULL,'Humewood',NULL,NULL,'Commune',NULL,2,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',5),(19,1,'Humewood Residence 3','Placeholder listing — edit title, description, and photos.',4000.00,4000.00,'Humewood Address 3','Gqeberha',NULL,'Humewood',NULL,NULL,'Single Room',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',25),(20,1,'Humewood Residence 4','Placeholder listing — edit title, description, and photos.',3800.00,3800.00,'Humewood Address 4','Gqeberha',NULL,'Humewood',NULL,NULL,'Commune',NULL,2,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',4),(21,1,'Humewood Residence 5','Placeholder listing — edit title, description, and photos.',4100.00,4100.00,'Humewood Address 5','Gqeberha',NULL,'Humewood',NULL,NULL,'Sharing',NULL,4,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Walking distance',20),(22,1,'Town Residence 1','Placeholder listing — edit title, description, and photos.',3000.00,3000.00,'Town Address 1','Gqeberha',NULL,'Town',NULL,NULL,'Single Room',NULL,4,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',20),(23,1,'Town Residence 2','Placeholder listing — edit title, description, and photos.',3200.00,3200.00,'Town Address 2','Gqeberha',NULL,'Town',NULL,NULL,'Sharing',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',22),(24,1,'Town Residence 3','Placeholder listing — edit title, description, and photos.',3100.00,3100.00,'Town Address 3','Gqeberha',NULL,'Town',NULL,NULL,'Single Room',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',25),(25,1,'Town Residence 4','Placeholder listing — edit title, description, and photos.',3300.00,3300.00,'Town Address 4','Gqeberha',NULL,'Town',NULL,NULL,'Sharing',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',28),(26,1,'Town Residence 5','Placeholder listing — edit title, description, and photos.',3000.00,3000.00,'Town Address 5','Gqeberha',NULL,'Town',NULL,NULL,'Single Room',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',30),(27,1,'North End Residence 1','Placeholder listing — edit title, description, and photos.',2900.00,2900.00,'North End Address 1','Gqeberha',NULL,'North End',NULL,NULL,'Single Room',NULL,4,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',20),(28,1,'North End Residence 2','Placeholder listing — edit title, description, and photos.',3000.00,3000.00,'North End Address 2','Gqeberha',NULL,'North End',NULL,NULL,'Sharing',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',22),(29,1,'North End Residence 3','Placeholder listing — edit title, description, and photos.',3100.00,3100.00,'North End Address 3','Gqeberha',NULL,'North End',NULL,NULL,'Single Room',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',25),(30,1,'North End Residence 4','Placeholder listing — edit title, description, and photos.',2950.00,2950.00,'North End Address 4','Gqeberha',NULL,'North End',NULL,NULL,'Sharing',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',28),(31,1,'North End Residence 5','Placeholder listing — edit title, description, and photos.',3050.00,3050.00,'North End Address 5','Gqeberha',NULL,'North End',NULL,NULL,'Single Room',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',30),(32,1,'Central Residence 1','Placeholder listing — edit title, description, and photos.',3300.00,3300.00,'Central Address 1','Gqeberha',NULL,'Central',NULL,NULL,'Single Room',NULL,4,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',20),(33,1,'Central Residence 2','Placeholder listing — edit title, description, and photos.',3400.00,3400.00,'Central Address 2','Gqeberha',NULL,'Central',NULL,NULL,'Sharing',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',22),(34,1,'Central Residence 3','Placeholder listing — edit title, description, and photos.',3500.00,3500.00,'Central Address 3','Gqeberha',NULL,'Central',NULL,NULL,'Single Room',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',25),(35,1,'Central Residence 4','Placeholder listing — edit title, description, and photos.',3350.00,3350.00,'Central Address 4','Gqeberha',NULL,'Central',NULL,NULL,'Sharing',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',28),(36,1,'Central Residence 5','Placeholder listing — edit title, description, and photos.',3450.00,3450.00,'Central Address 5','Gqeberha',NULL,'Central',NULL,NULL,'Single Room',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',30),(37,1,'Pier 14 Residence 1','Placeholder listing — edit title, description, and photos.',4500.00,4500.00,'Pier 14 Address 1','Gqeberha',NULL,'Pier 14',NULL,NULL,'Single Room',NULL,4,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',20),(38,1,'Pier 14 Residence 2','Placeholder listing — edit title, description, and photos.',4600.00,4600.00,'Pier 14 Address 2','Gqeberha',NULL,'Pier 14',NULL,NULL,'Sharing',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',22),(39,1,'Pier 14 Residence 3','Placeholder listing — edit title, description, and photos.',4700.00,4700.00,'Pier 14 Address 3','Gqeberha',NULL,'Pier 14',NULL,NULL,'Single Room',NULL,5,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',25),(40,1,'Pier 14 Residence 4','Placeholder listing — edit title, description, and photos.',4550.00,4550.00,'Pier 14 Address 4','Gqeberha',NULL,'Pier 14',NULL,NULL,'Sharing',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',28),(41,1,'Pier 14 Residence 5','Placeholder listing — edit title, description, and photos.',4650.00,4650.00,'Pier 14 Address 5','Gqeberha',NULL,'Pier 14',NULL,NULL,'Single Room',NULL,6,NULL,0,0,1,NULL,NULL,NULL,0,'2026-09-02 16:01:10','2026-09-02 16:01:10',NULL,NULL,'Active','Shuttle required',30);
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
  `caption` varchar(255) DEFAULT NULL,
  `isMain` tinyint(1) DEFAULT '0',
  `displayOrder` int DEFAULT NULL,
  `uploadedAt` datetime DEFAULT CURRENT_TIMESTAMP,
  `hasWatermark` tinyint(1) DEFAULT '0',
  `isVR` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`imageID`),
  KEY `propertyID` (`propertyID`),
  CONSTRAINT `propertyimage_ibfk_1` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `propertyimage`
--

LOCK TABLES `propertyimage` WRITE;
/*!40000 ALTER TABLE `propertyimage` DISABLE KEYS */;
INSERT INTO `propertyimage` VALUES (2,2,'/uploads/placeholder-riverside.jpg','exterior',NULL,0,1,'2026-09-02 15:59:25',0,0),(3,3,'/uploads/placeholder-hub.jpg','exterior',NULL,0,1,'2026-09-02 15:59:25',0,0),(4,4,'/uploads/placeholder-ensuite.jpg','exterior',NULL,0,1,'2026-09-02 15:59:25',0,0),(7,3,'/uploads/admiralty/admiralty1.png',NULL,NULL,0,0,'2026-09-02 16:03:25',0,0),(8,3,'/uploads/admiralty/admiralty10.png',NULL,NULL,0,1,'2026-09-02 16:03:25',0,0),(9,3,'/uploads/admiralty/admiralty2.png',NULL,NULL,0,2,'2026-09-02 16:03:25',0,0),(10,3,'/uploads/admiralty/admiralty3.png',NULL,NULL,0,3,'2026-09-02 16:03:25',0,0),(11,3,'/uploads/admiralty/admiralty4.png',NULL,NULL,0,4,'2026-09-02 16:03:25',0,0),(12,3,'/uploads/admiralty/main.png',NULL,NULL,1,5,'2026-09-02 16:03:25',0,0),(13,2,'/uploads/Gomery/gomery1.png',NULL,NULL,0,0,'2026-09-02 16:03:25',0,0),(14,2,'/uploads/Gomery/gomery2.png',NULL,NULL,0,1,'2026-09-02 16:03:25',0,0),(15,2,'/uploads/Gomery/gomery3.png',NULL,NULL,0,2,'2026-09-02 16:03:25',0,0),(16,2,'/uploads/Gomery/gomery4.png',NULL,NULL,0,3,'2026-09-02 16:03:25',0,0),(17,2,'/uploads/Gomery/main.png',NULL,NULL,1,4,'2026-09-02 16:03:25',0,0),(23,3,'/uploads/admiralty/admiralty1.png',NULL,NULL,0,0,'2026-09-02 16:04:15',0,0),(24,3,'/uploads/admiralty/admiralty10.png',NULL,NULL,0,1,'2026-09-02 16:04:15',0,0),(25,3,'/uploads/admiralty/admiralty2.png',NULL,NULL,0,2,'2026-09-02 16:04:15',0,0),(26,3,'/uploads/admiralty/admiralty3.png',NULL,NULL,0,3,'2026-09-02 16:04:15',0,0),(27,3,'/uploads/admiralty/admiralty4.png',NULL,NULL,0,4,'2026-09-02 16:04:15',0,0),(28,3,'/uploads/admiralty/main.png',NULL,NULL,1,5,'2026-09-02 16:04:15',0,0),(29,2,'/uploads/Gomery/gomery1.png',NULL,NULL,0,0,'2026-09-02 16:04:15',0,0),(30,2,'/uploads/Gomery/gomery2.png',NULL,NULL,0,1,'2026-09-02 16:04:15',0,0),(31,2,'/uploads/Gomery/gomery3.png',NULL,NULL,0,2,'2026-09-02 16:04:15',0,0),(32,2,'/uploads/Gomery/gomery4.png',NULL,NULL,0,3,'2026-09-02 16:04:15',0,0),(33,2,'/uploads/Gomery/main.png',NULL,NULL,1,4,'2026-09-02 16:04:15',0,0),(39,3,'/uploads/admiralty/admiralty1.png',NULL,NULL,0,0,'2026-09-02 16:04:42',0,0),(40,3,'/uploads/admiralty/admiralty10.png',NULL,NULL,0,1,'2026-09-02 16:04:42',0,0),(41,3,'/uploads/admiralty/admiralty2.png',NULL,NULL,0,2,'2026-09-02 16:04:42',0,0),(42,3,'/uploads/admiralty/admiralty3.png',NULL,NULL,0,3,'2026-09-02 16:04:42',0,0),(43,3,'/uploads/admiralty/admiralty4.png',NULL,NULL,0,4,'2026-09-02 16:04:42',0,0),(44,3,'/uploads/admiralty/main.png',NULL,NULL,1,5,'2026-09-02 16:04:42',0,0),(45,2,'/uploads/Gomery/gomery1.png',NULL,NULL,0,0,'2026-09-02 16:04:42',0,0),(46,2,'/uploads/Gomery/gomery2.png',NULL,NULL,0,1,'2026-09-02 16:04:42',0,0),(47,2,'/uploads/Gomery/gomery3.png',NULL,NULL,0,2,'2026-09-02 16:04:42',0,0),(48,2,'/uploads/Gomery/gomery4.png',NULL,NULL,0,3,'2026-09-02 16:04:42',0,0),(49,2,'/uploads/Gomery/main.png',NULL,NULL,1,4,'2026-09-02 16:04:42',0,0);
/*!40000 ALTER TABLE `propertyimage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report`
--

DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `reportID` int NOT NULL AUTO_INCREMENT,
  `propertyID` int NOT NULL,
  `reporterID` int NOT NULL,
  `reason` varchar(255) NOT NULL,
  `details` text,
  `reportDate` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(50) DEFAULT 'Open',
  `reviewedBy` int DEFAULT NULL,
  `reviewedAt` datetime DEFAULT NULL,
  PRIMARY KEY (`reportID`),
  KEY `propertyID` (`propertyID`),
  KEY `reporterID` (`reporterID`),
  KEY `reviewedBy` (`reviewedBy`),
  CONSTRAINT `report_ibfk_1` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`),
  CONSTRAINT `report_ibfk_2` FOREIGN KEY (`reporterID`) REFERENCES `users` (`userID`),
  CONSTRAINT `report_ibfk_3` FOREIGN KEY (`reviewedBy`) REFERENCES `admins` (`adminID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report`
--

LOCK TABLES `report` WRITE;
/*!40000 ALTER TABLE `report` DISABLE KEYS */;
INSERT INTO `report` VALUES (3,3,1,'No electricity','Property currently has no electricity.','2026-08-25 09:33:49','Pending',NULL,NULL),(4,4,1,'Unsafe / security concern',NULL,'2026-08-25 09:43:46','Pending',NULL,NULL);
/*!40000 ALTER TABLE `report` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reports`
--

LOCK TABLES `reports` WRITE;
/*!40000 ALTER TABLE `reports` DISABLE KEYS */;
INSERT INTO `reports` VALUES (1,'Tenant reports the property has no furniture and no hot water.',2,'No furniture / no hot water','2026-09-03 23:27:05.000000',NULL,4),(3,'Tenant reports the photos are fake and the property does not actually exist.',3,'Fake listing / property does not exist','2026-09-04 00:32:50.000000',NULL,5),(4,'A second tenant also confirms this address does not match any real property and the listing photos appear to be stock images.',3,'Fake listing / property does not exist','2026-09-04 00:34:17.000000',NULL,4),(5,'A second tenant also confirms this address does not match any real property and the listing photos appear to be stock images.',3,'Fake listing / property does not exist','2026-09-04 00:34:19.000000',NULL,4),(6,'Tenant reports the photos are fake and the property does not actually exist.',3,'Fake listing / property does not exist','2026-09-04 00:48:05.000000',NULL,5);
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
  `cleanlinessRating` int DEFAULT NULL,
  `isReported` bit(1) DEFAULT NULL,
  `residencyStatus` varchar(255) DEFAULT NULL,
  `safetyRating` int DEFAULT NULL,
  `studyAreaRating` int DEFAULT NULL,
  `wifiRating` int DEFAULT NULL,
  `yearOfStudy` int DEFAULT NULL,
  PRIMARY KEY (`reviewID`),
  KEY `studentID` (`studentID`),
  KEY `propertyID` (`propertyID`),
  CONSTRAINT `review_ibfk_1` FOREIGN KEY (`studentID`) REFERENCES `students` (`studentID`),
  CONSTRAINT `review_ibfk_2` FOREIGN KEY (`propertyID`) REFERENCES `property` (`propertyID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
INSERT INTO `review` VALUES (3,4,3,4,'Security is excellent, felt very safe here all year.','2026-05-22 09:45:00',NULL,NULL,NULL,NULL,NULL,NULL,NULL);
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
  `comment` text,
  `propertyID` int DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `reviewDate` datetime(6) DEFAULT NULL,
  `studentID` int DEFAULT NULL,
  `cleanlinessRating` int DEFAULT NULL,
  `residencyStatus` varchar(255) DEFAULT NULL,
  `safetyRating` int DEFAULT NULL,
  `studyAreaRating` int DEFAULT NULL,
  `wifiRating` int DEFAULT NULL,
  `yearOfStudy` int DEFAULT NULL,
  `isReported` bit(1) DEFAULT NULL,
  `landlordResponse` varchar(255) DEFAULT NULL,
  `reportReason` varchar(255) DEFAULT NULL,
  `responseDate` datetime(6) DEFAULT NULL,
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
INSERT INTO `students` VALUES (4,2,2000.00,4500.00,NULL,NULL),(5,3,3000.00,6000.00,NULL,NULL);
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
  `role` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`userID`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'$2a$10$1SD1ofodj/m1MK.9zlv/XerVtfUZuQhGlJL7zsYpnGl1eHWC2LTT2','Ezile','Hlomane','2001-03-14','ezile-ulee@gmail.com','0821234567',NULL,_binary '',2,NULL),(2,'$2a$10$1SD1ofodj/m1MK.9zlv/XerVtfUZuQhGlJL7zsYpnGl1eHWC2LTT2','Thandeka','Mnguni','1985-07-02','thandeka.landlord@ulee.co.za','0827654321',NULL,NULL,NULL,NULL),(4,'$2a$10$1SD1ofodj/m1MK.9zlv/XerVtfUZuQhGlJL7zsYpnGl1eHWC2LTT2','Aisha','Adams','2004-05-09','aisha.student@ulee.co.za','0741122334',NULL,_binary '',1,NULL),(5,'$2a$10$1SD1ofodj/m1MK.9zlv/XerVtfUZuQhGlJL7zsYpnGl1eHWC2LTT2','Luyanda','Peter','2003-09-30','luyanda.student@ulee.co.za','0765544332',NULL,_binary '',NULL,NULL),(6,'$2b$10$lJV9kuQbZCCwAaeDJ0ZCd.ko.GtTSHxU3.KAYHPyjry5qB4/3kN9e','Unakho','Gadavu',NULL,'admin@ulee.co.za',NULL,NULL,_binary '',NULL,'ADMIN'),(7,'Super1010','Emihle','Mzongwana','2006-02-24','s225639335@mandela.ac.za','0795998143',NULL,NULL,NULL,NULL);
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

-- Dump completed on 2026-09-12 17:32:26
