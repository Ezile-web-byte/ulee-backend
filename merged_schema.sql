CREATE DATABASE  IF NOT EXISTS `ulee_db_merged` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `ulee_db_merged`;
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

-- Dump completed on 2026-09-12 16:22:04

