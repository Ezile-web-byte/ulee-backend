-- Removes unused columns from the property table.
-- latitude and longitude are intentionally KEPT: the Geoapify
-- nearby-places feature needs them.
-- Back up the database first.
ALTER TABLE property
  DROP COLUMN distanceFromUniversity,
  DROP COLUMN bathrooms,
  DROP COLUMN furnished,
  DROP COLUMN area,
  DROP COLUMN bedrooms,
  DROP COLUMN municipality,
  DROP COLUMN studyFriendly;