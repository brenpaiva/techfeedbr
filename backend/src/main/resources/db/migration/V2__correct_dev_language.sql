-- The DEV articles endpoint does not provide a language field. Earlier imports
-- assumed English for every DEV article, including Spanish and Portuguese posts.
UPDATE content_item
SET language = 'und'
WHERE primary_source = 'DEV' AND language = 'en';
