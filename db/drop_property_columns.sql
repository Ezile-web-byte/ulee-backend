-- Removes unused columns from the property table.
-- Back up the database first.
ALTER TABLE property
  DROP COLUMN distanceFromUniversity,
  DROP COLUMN latitude,
  DROP COLUMN longitude,
  DROP COLUMN bathrooms,
  DROP COLUMN furnished,
  DROP COLUMN area,
  DROP COLUMN bedrooms,
  DROP COLUMN municipality,
  DROP COLUMN studyFriendly;
