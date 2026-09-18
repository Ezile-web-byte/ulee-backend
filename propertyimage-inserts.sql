
INSERT INTO propertyimage (propertyID, url, category, caption, isMain, displayOrder, hasWatermark, isVR) VALUES
(3, '/uploads/admiralty/admiralty1.png', NULL, NULL, 1, 0, 0, 0),
(3, '/uploads/admiralty/admiralty10.png', NULL, NULL, 0, 1, 0, 0),
(3, '/uploads/admiralty/admiralty2.png', NULL, NULL, 0, 2, 0, 0),
(3, '/uploads/admiralty/admiralty3.png', NULL, NULL, 0, 3, 0, 0),
(3, '/uploads/admiralty/admiralty4.png', NULL, NULL, 0, 4, 0, 0),
(3, '/uploads/admiralty/main.png', NULL, NULL, 0, 5, 0, 0),
(2, '/uploads/Gomery/gomery1.png', NULL, NULL, 1, 0, 0, 0),
(2, '/uploads/Gomery/gomery2.png', NULL, NULL, 0, 1, 0, 0),
(2, '/uploads/Gomery/gomery3.png', NULL, NULL, 0, 2, 0, 0),
(2, '/uploads/Gomery/gomery4.png', NULL, NULL, 0, 3, 0, 0),
(2, '/uploads/Gomery/main.png', NULL, NULL, 0, 4, 0, 0),
(1, '/uploads/Dunes/11dune1.png', NULL, NULL, 1, 0, 0, 0),
(1, '/uploads/Dunes/11dune2.png', NULL, NULL, 0, 1, 0, 0),
(1, '/uploads/Dunes/11dune3.png', NULL, NULL, 0, 2, 0, 0),
(1, '/uploads/Dunes/11dune4.png', NULL, NULL, 0, 3, 0, 0),
(1, '/uploads/Dunes/main.png', NULL, NULL, 0, 4, 0, 0);

-- Cleanup: make sure only main.png is flagged as the cover image per property,
-- since the block above marks the *1.png files as isMain instead


SET SQL_SAFE_UPDATES = 0;

UPDATE propertyimage SET isMain = 0;

UPDATE propertyimage
SET isMain = 1
WHERE url LIKE '%/main.png';

SELECT propertyID, url, isMain
FROM propertyimage
WHERE propertyID IN (1, 2, 3)
ORDER BY propertyID, isMain DESC;

SET SQL_SAFE_UPDATES = 1;