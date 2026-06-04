INSERT INTO pallet_inputs (id, output_pallet_id, input_type, input_id, input_batch_id_hex, created_at)
SELECT gen_random_uuid()::text, p.id, 'RAW_BATCH', r.id, r.batch_id_hex,
       COALESCE(p.created_at, CURRENT_TIMESTAMP)
FROM pallets p
CROSS JOIN LATERAL regexp_split_to_table(p.parent_raw_batch_id_hexes, '\s*,\s*') AS parent_hex
JOIN raw_batches r ON lower(r.batch_id_hex) = lower(parent_hex)
WHERE p.parent_raw_batch_id_hexes IS NOT NULL
  AND btrim(p.parent_raw_batch_id_hexes) <> ''
ON CONFLICT (output_pallet_id, input_type, input_id) DO NOTHING;
