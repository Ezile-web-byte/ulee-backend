-- ============================================================
-- Seeds 35 properties (5 per category) under landlordID=1 (ezile@gmail.com)
-- Rules applied:
--   - On Campus: Single/Sharing only, capacity 20-30, commute = Walking distance
--   - Summerstrand: 2 Single, 2 Sharing, 1 Commune, commute varies
--   - Humewood: 1 Single, 2 Sharing, 2 Commune, commute varies
--   - Town / North End / Central / Pier 14: Single/Sharing only,
--     capacity 20-30, commute = Shuttle required (always, off-campus)
--   - Commune capacity is always 4 or 5, regardless of category
-- Titles are GENERIC placeholders — rename them yourself in the app.
-- Rent values are PLACEHOLDER DEFAULTS (not specified by you) — edit these
-- to your actual pricing whenever you like.
-- ============================================================

-- Clears out any earlier partial seed for this landlord's categorized
-- properties, so re-running this script never creates duplicates.
DELETE FROM property WHERE landlordID = 1
  AND suburb IN ('On Campus','Summerstrand','Humewood','Town','North End','Central','Pier 14');

-- ── On Campus (Single/Sharing only, capacity 20-30, walking distance) ──
INSERT INTO property (landlordID, rent, capacity, status, isAvailable, title, description, deposit, address, city, suburb, type, bathrooms, commuteType) VALUES
(1, 3200.00, 20, 'Active', 1, 'On Campus Residence 1', 'Placeholder listing — edit title, description, and photos.', 3200.00, 'Campus Address 1', 'Gqeberha', 'On Campus', 'Single Room', 4, 'Walking distance'),
(1, 3400.00, 22, 'Active', 1, 'On Campus Residence 2', 'Placeholder listing — edit title, description, and photos.', 3400.00, 'Campus Address 2', 'Gqeberha', 'On Campus', 'Sharing', 4, 'Walking distance'),
(1, 3300.00, 25, 'Active', 1, 'On Campus Residence 3', 'Placeholder listing — edit title, description, and photos.', 3300.00, 'Campus Address 3', 'Gqeberha', 'On Campus', 'Single Room', 5, 'Walking distance'),
(1, 3600.00, 28, 'Active', 1, 'On Campus Residence 4', 'Placeholder listing — edit title, description, and photos.', 3600.00, 'Campus Address 4', 'Gqeberha', 'On Campus', 'Sharing', 5, 'Walking distance'),
(1, 3500.00, 30, 'Active', 1, 'On Campus Residence 5', 'Placeholder listing — edit title, description, and photos.', 3500.00, 'Campus Address 5', 'Gqeberha', 'On Campus', 'Single Room', 6, 'Walking distance');

-- ── Summerstrand (2 Single, 2 Sharing, 1 Commune; commute varies) ──
INSERT INTO property (landlordID, rent, capacity, status, isAvailable, title, description, deposit, address, city, suburb, type, bathrooms, commuteType) VALUES
(1, 4200.00, 20, 'Active', 1, 'Summerstrand Residence 1', 'Placeholder listing — edit title, description, and photos.', 4200.00, 'Summerstrand Address 1', 'Gqeberha', 'Summerstrand', 'Single Room', 4, 'Walking distance'),
(1, 4000.00, 22, 'Active', 1, 'Summerstrand Residence 2', 'Placeholder listing — edit title, description, and photos.', 4000.00, 'Summerstrand Address 2', 'Gqeberha', 'Summerstrand', 'Sharing', 5, 'Shuttle required'),
(1, 3800.00, 4, 'Active', 1, 'Summerstrand Residence 3', 'Placeholder listing — edit title, description, and photos.', 3800.00, 'Summerstrand Address 3', 'Gqeberha', 'Summerstrand', 'Commune', 2, 'Walking distance'),
(1, 4300.00, 25, 'Active', 1, 'Summerstrand Residence 4', 'Placeholder listing — edit title, description, and photos.', 4300.00, 'Summerstrand Address 4', 'Gqeberha', 'Summerstrand', 'Single Room', 6, 'Shuttle required'),
(1, 4100.00, 28, 'Active', 1, 'Summerstrand Residence 5', 'Placeholder listing — edit title, description, and photos.', 4100.00, 'Summerstrand Address 5', 'Gqeberha', 'Summerstrand', 'Sharing', 5, 'Walking distance');

-- ── Humewood (1 Single, 2 Sharing, 2 Commune; commute varies) ──
INSERT INTO property (landlordID, rent, capacity, status, isAvailable, title, description, deposit, address, city, suburb, type, bathrooms, commuteType) VALUES
(1, 3900.00, 22, 'Active', 1, 'Humewood Residence 1', 'Placeholder listing — edit title, description, and photos.', 3900.00, 'Humewood Address 1', 'Gqeberha', 'Humewood', 'Sharing', 5, 'Shuttle required'),
(1, 3700.00, 5, 'Active', 1, 'Humewood Residence 2', 'Placeholder listing — edit title, description, and photos.', 3700.00, 'Humewood Address 2', 'Gqeberha', 'Humewood', 'Commune', 2, 'Walking distance'),
(1, 4000.00, 25, 'Active', 1, 'Humewood Residence 3', 'Placeholder listing — edit title, description, and photos.', 4000.00, 'Humewood Address 3', 'Gqeberha', 'Humewood', 'Single Room', 6, 'Shuttle required'),
(1, 3800.00, 4, 'Active', 1, 'Humewood Residence 4', 'Placeholder listing — edit title, description, and photos.', 3800.00, 'Humewood Address 4', 'Gqeberha', 'Humewood', 'Commune', 2, 'Shuttle required'),
(1, 4100.00, 20, 'Active', 1, 'Humewood Residence 5', 'Placeholder listing — edit title, description, and photos.', 4100.00, 'Humewood Address 5', 'Gqeberha', 'Humewood', 'Sharing', 4, 'Walking distance');

-- ── Town (Single/Sharing only, capacity 20-30, shuttle required) ──
INSERT INTO property (landlordID, rent, capacity, status, isAvailable, title, description, deposit, address, city, suburb, type, bathrooms, commuteType) VALUES
(1, 3000.00, 20, 'Active', 1, 'Town Residence 1', 'Placeholder listing — edit title, description, and photos.', 3000.00, 'Town Address 1', 'Gqeberha', 'Town', 'Single Room', 4, 'Shuttle required'),
(1, 3200.00, 22, 'Active', 1, 'Town Residence 2', 'Placeholder listing — edit title, description, and photos.', 3200.00, 'Town Address 2', 'Gqeberha', 'Town', 'Sharing', 5, 'Shuttle required'),
(1, 3100.00, 25, 'Active', 1, 'Town Residence 3', 'Placeholder listing — edit title, description, and photos.', 3100.00, 'Town Address 3', 'Gqeberha', 'Town', 'Single Room', 5, 'Shuttle required'),
(1, 3300.00, 28, 'Active', 1, 'Town Residence 4', 'Placeholder listing — edit title, description, and photos.', 3300.00, 'Town Address 4', 'Gqeberha', 'Town', 'Sharing', 6, 'Shuttle required'),
(1, 3000.00, 30, 'Active', 1, 'Town Residence 5', 'Placeholder listing — edit title, description, and photos.', 3000.00, 'Town Address 5', 'Gqeberha', 'Town', 'Single Room', 6, 'Shuttle required');

-- ── North End (Single/Sharing only, capacity 20-30, shuttle required) ──
INSERT INTO property (landlordID, rent, capacity, status, isAvailable, title, description, deposit, address, city, suburb, type, bathrooms, commuteType) VALUES
(1, 2900.00, 20, 'Active', 1, 'North End Residence 1', 'Placeholder listing — edit title, description, and photos.', 2900.00, 'North End Address 1', 'Gqeberha', 'North End', 'Single Room', 4, 'Shuttle required'),
(1, 3000.00, 22, 'Active', 1, 'North End Residence 2', 'Placeholder listing — edit title, description, and photos.', 3000.00, 'North End Address 2', 'Gqeberha', 'North End', 'Sharing', 5, 'Shuttle required'),
(1, 3100.00, 25, 'Active', 1, 'North End Residence 3', 'Placeholder listing — edit title, description, and photos.', 3100.00, 'North End Address 3', 'Gqeberha', 'North End', 'Single Room', 5, 'Shuttle required'),
(1, 2950.00, 28, 'Active', 1, 'North End Residence 4', 'Placeholder listing — edit title, description, and photos.', 2950.00, 'North End Address 4', 'Gqeberha', 'North End', 'Sharing', 6, 'Shuttle required'),
(1, 3050.00, 30, 'Active', 1, 'North End Residence 5', 'Placeholder listing — edit title, description, and photos.', 3050.00, 'North End Address 5', 'Gqeberha', 'North End', 'Single Room', 6, 'Shuttle required');

-- ── Central (Single/Sharing only, capacity 20-30, shuttle required) ──
INSERT INTO property (landlordID, rent, capacity, status, isAvailable, title, description, deposit, address, city, suburb, type, bathrooms, commuteType) VALUES
(1, 3300.00, 20, 'Active', 1, 'Central Residence 1', 'Placeholder listing — edit title, description, and photos.', 3300.00, 'Central Address 1', 'Gqeberha', 'Central', 'Single Room', 4, 'Shuttle required'),
(1, 3400.00, 22, 'Active', 1, 'Central Residence 2', 'Placeholder listing — edit title, description, and photos.', 3400.00, 'Central Address 2', 'Gqeberha', 'Central', 'Sharing', 5, 'Shuttle required'),
(1, 3500.00, 25, 'Active', 1, 'Central Residence 3', 'Placeholder listing — edit title, description, and photos.', 3500.00, 'Central Address 3', 'Gqeberha', 'Central', 'Single Room', 5, 'Shuttle required'),
(1, 3350.00, 28, 'Active', 1, 'Central Residence 4', 'Placeholder listing — edit title, description, and photos.', 3350.00, 'Central Address 4', 'Gqeberha', 'Central', 'Sharing', 6, 'Shuttle required'),
(1, 3450.00, 30, 'Active', 1, 'Central Residence 5', 'Placeholder listing — edit title, description, and photos.', 3450.00, 'Central Address 5', 'Gqeberha', 'Central', 'Single Room', 6, 'Shuttle required');

-- ── Pier 14 (Single/Sharing only, capacity 20-30, shuttle required) ──
INSERT INTO property (landlordID, rent, capacity, status, isAvailable, title, description, deposit, address, city, suburb, type, bathrooms, commuteType) VALUES
(1, 4500.00, 20, 'Active', 1, 'Pier 14 Residence 1', 'Placeholder listing — edit title, description, and photos.', 4500.00, 'Pier 14 Address 1', 'Gqeberha', 'Pier 14', 'Single Room', 4, 'Shuttle required'),
(1, 4600.00, 22, 'Active', 1, 'Pier 14 Residence 2', 'Placeholder listing — edit title, description, and photos.', 4600.00, 'Pier 14 Address 2', 'Gqeberha', 'Pier 14', 'Sharing', 5, 'Shuttle required'),
(1, 4700.00, 25, 'Active', 1, 'Pier 14 Residence 3', 'Placeholder listing — edit title, description, and photos.', 4700.00, 'Pier 14 Address 3', 'Gqeberha', 'Pier 14', 'Single Room', 5, 'Shuttle required'),
(1, 4550.00, 28, 'Active', 1, 'Pier 14 Residence 4', 'Placeholder listing — edit title, description, and photos.', 4550.00, 'Pier 14 Address 4', 'Gqeberha', 'Pier 14', 'Sharing', 6, 'Shuttle required'),
(1, 4650.00, 30, 'Active', 1, 'Pier 14 Residence 5', 'Placeholder listing — edit title, description, and photos.', 4650.00, 'Pier 14 Address 5', 'Gqeberha', 'Pier 14', 'Single Room', 6, 'Shuttle required');

-- Update the landlord's propertiesCount to match reality
UPDATE landlords SET propertiesCount = (SELECT COUNT(*) FROM property WHERE landlordID = 1) WHERE landlordID = 1;

-- Verify: should show 5 rows per category
SELECT suburb, type, COUNT(*) AS total FROM property WHERE landlordID = 1
  AND suburb IN ('On Campus','Summerstrand','Humewood','Town','North End','Central','Pier 14')
  GROUP BY suburb, type ORDER BY suburb, type;
