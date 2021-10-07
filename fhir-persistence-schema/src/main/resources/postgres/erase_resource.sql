-------------------------------------------------------------------------------
-- (C) Copyright IBM Corp. 2021
--
-- SPDX-License-Identifier: Apache-2.0
-------------------------------------------------------------------------------

-- ----------------------------------------------------------------------------
-- Procedure to remove a resource, history and parameters values
-- 
-- p_resource_type: the resource type
-- p_logical_id: the resource logical id
-- o_deleted: the total number of resource versions that are deleted
-- ----------------------------------------------------------------------------
    ( IN p_resource_type                VARCHAR(  36),
      IN p_logical_id                   VARCHAR( 255),
      OUT o_deleted                     BIGINT)
    RETURNS BIGINT
    LANGUAGE plpgsql
     AS $$

  DECLARE
  v_schema_name         VARCHAR(128);
  v_logical_resource_id BIGINT := NULL;
  v_resource_type_id    BIGINT := -1;
  v_total               BIGINT := 0;

BEGIN
  v_schema_name := '{{SCHEMA_NAME}}';

  -- Prep 1: Get the v_resource_type_id
  SELECT resource_type_id INTO v_resource_type_id 
  FROM {{SCHEMA_NAME}}.resource_types
  WHERE resource_type = p_resource_type;

  -- Prep 2: Get the logical from the system-wide logical resource level
  SELECT logical_resource_id INTO v_logical_resource_id 
  FROM {{SCHEMA_NAME}}.logical_resources
  WHERE resource_type_id = v_resource_type_id AND logical_id = p_logical_id
  FOR UPDATE;
  
  IF NOT FOUND
  THEN
    v_total := -1;
  ELSE
    -- Step 1: Delete from resource_change_log
    -- Delete is done before the RESOURCES table entries disappear
    -- This uses the primary_keys of each table to conditional-delete
    EXECUTE 
    'DELETE FROM {{SCHEMA_NAME}}.RESOURCE_CHANGE_LOG'
    || '  WHERE RESOURCE_ID IN ('
    || '    SELECT RESOURCE_ID'
    || '    FROM {{SCHEMA_NAME}}.' || p_resource_type || '_RESOURCES'
    || '    WHERE LOGICAL_RESOURCE_ID = $1)'
    USING v_logical_resource_id;

    -- Step 2: Delete All Versions from Resources Table 
    EXECUTE 'DELETE FROM {{SCHEMA_NAME}}.' || p_resource_type || '_RESOURCES WHERE LOGICAL_RESOURCE_ID = $1'
    USING v_logical_resource_id;
    GET DIAGNOSTICS v_total = ROW_COUNT;

    -- The delete_resource_parameters call is a function, so we have to use a select here, not call 
    EXECUTE 'SELECT {{SCHEMA_NAME}}.delete_resource_parameters($1, $2)'
    USING p_resource_type, v_logical_resource_id;
    
    -- Step 4: Delete from Logical Resources table 
    EXECUTE 'DELETE FROM {{SCHEMA_NAME}}.' || p_resource_type || '_LOGICAL_RESOURCES WHERE LOGICAL_RESOURCE_ID = $1'
    USING v_logical_resource_id;

    -- Step 5: Delete from Global Logical Resources
    EXECUTE 'DELETE FROM {{SCHEMA_NAME}}.LOGICAL_RESOURCES WHERE LOGICAL_RESOURCE_ID = $1 AND RESOURCE_TYPE_ID = $2'
    USING v_logical_resource_id, v_resource_type_id;
  END IF;

  -- Return the total number of deleted versions
  o_deleted := v_total;
END $$;
