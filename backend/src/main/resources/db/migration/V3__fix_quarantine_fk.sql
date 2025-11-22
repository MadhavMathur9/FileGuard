ALTER TABLE quarantine_item DROP CONSTRAINT quarantine_item_file_entry_id_fkey;
ALTER TABLE quarantine_item ALTER COLUMN file_entry_id DROP NOT NULL;
ALTER TABLE quarantine_item ADD CONSTRAINT quarantine_item_file_entry_id_fkey FOREIGN KEY (file_entry_id) REFERENCES file_entry(id) ON DELETE SET NULL;
