-- VoltGrid (Member 3). Safe to re-run; Main also creates these automatically.
/*M!999999\- enable the sandbox mode */ 
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `email` varchar(120) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `stations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `power_kw` decimal(6,1) NOT NULL,
  `type` varchar(20) NOT NULL,
  `price_per_kwh` decimal(6,2) NOT NULL,
  `distance_km` decimal(6,1) NOT NULL DEFAULT 0.0,
  `available` tinyint(1) NOT NULL DEFAULT 1,
  `rating` decimal(2,1) NOT NULL DEFAULT 0.0,
  `queue` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `vehicles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `model` varchar(80) NOT NULL,
  `battery_capacity_kwh` decimal(6,1) NOT NULL,
  `max_charging_speed_kw` decimal(6,1) NOT NULL,
  `efficiency_kwh_per_km` decimal(5,3) NOT NULL DEFAULT 0.160,
  `current_soc` decimal(5,2) NOT NULL DEFAULT 20.00,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `bookings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `station_id` int(11) NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `status` varchar(12) NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_station_time` (`station_id`,`start_time`),
  KEY `idx_user` (`user_id`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `charging_sessions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `station_id` int(11) NOT NULL,
  `session_date` datetime NOT NULL,
  `start_soc` decimal(5,2) NOT NULL,
  `end_soc` decimal(5,2) NOT NULL,
  `target_soc` decimal(5,2) NOT NULL,
  `energy_kwh` decimal(8,3) NOT NULL,
  `cost` decimal(10,2) NOT NULL,
  `duration_sec` int(11) NOT NULL,
  `status` varchar(12) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`session_date`)
);
/*!40101 SET character_set_client = @saved_cs_client */;
