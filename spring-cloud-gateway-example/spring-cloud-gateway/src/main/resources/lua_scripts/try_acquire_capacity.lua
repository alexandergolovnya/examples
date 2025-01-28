-- KEYS[1] - the Redis hash key representing the capacity store
-- ARGV[1] - the tier number to attempt to acquire capacity from
-- Returns: table with acquired status and source tier number
-- Note: Tiers are stored in descending order (higher number = lower priority)

-- Local functions:

-- Function to validate input parameters:
-- check if the capacity Redis key and the tier number are provided and if the tier number is valid
local function validate_inputs(key, tier)
    if not key or not tier then
        return false, "Missing the capacity key or the tier number"
    end

    local tier_number = tonumber(tier)
    if not tier_number or tier_number < 1 then
        return false, "Invalid tier number: " .. tostring(tier)
    end

    return true, tier_number
end

-- Function to find capacity for a specific tier
--
-- The loop uses i = 1, #tiers_map, 2 because Redis HGETALL returns a flat array where:
-- Even indices (1,3,5...) contain keys (tier numbers)
-- Odd indices (2,4,6...) contain values (capacities)
-- The step of 2 helps us skip through keys and values correctly
-- #tiers_map returns the array length
local function get_tier_capacity(tiers_map, target_tier)
    for i = 1, #tiers_map, 2 do
        if tonumber(tiers_map[i]) == target_tier then
            return tonumber(tiers_map[i + 1]) or 0
        end
    end
    return 0
end

-- Function to find and acquire capacity from lower priority tiers
-- Since tiers are stored in descending order, we look for higher numbers to borrow from the lowest tier first
local function try_borrow_capacity(capacity_key, tiers_map, requested_tier)

    -- The loop uses i = 1, #tiers_map, 2 because Redis HGETALL returns a flat array where:
    -- Even indices (1,3,5...) contain keys (tier numbers)
    -- Odd indices (2,4,6...) contain values (capacities)
    -- The step of 2 helps us skip through keys and values correctly
    -- #tiers_map returns the array length
    for i = 1, #tiers_map, 2 do
        local current_tier = tonumber(tiers_map[i])
        local current_tier_capacity = tonumber(tiers_map[i + 1]) or 0

        -- Only check tiers with lower priority (higher numbers)
        if current_tier > requested_tier and current_tier_capacity > 0 then
            redis.call('hincrby', capacity_key, current_tier, -1)
            return true, current_tier
        end
    end
    return false, nil
end

-- Main script execution:

local function main()
    -- Input validation
    local is_valid, result = validate_inputs(KEYS[1], ARGV[1])
    if not is_valid then
        -- Return as a flat list for RedisUtils.toMap()
        return {
            "acquired", "false",
            "error", result,
            "requested_tier", ARGV[1]
        }
    end

    local capacity_key = KEYS[1]
    local tier_number = result

    -- Get all tiers and their capacities
    local tiers_map = redis.call('hgetall', capacity_key)

    -- Check if we have any data
    if #tiers_map == 0 then
        return {
            "acquired", "false",
            "error", "No capacity data found at key: " .. capacity_key,
            "requested_tier", tostring(tier_number)
        }
    end

    -- Try to acquire capacity from the requested tier
    local capacity = get_tier_capacity(tiers_map, tier_number)
    if capacity > 0 then
        redis.call('hincrby', capacity_key, tier_number, -1)
        return {
            "acquired", "true",
            "source_tier", tostring(tier_number),
            "requested_tier", tostring(tier_number)
        }
    end

    -- If no capacity in requested tier, try to borrow from lower priority tiers
    local borrowed, source_tier = try_borrow_capacity(capacity_key, tiers_map, tier_number)
    if borrowed then
        return {
            "acquired", "true",
            "source_tier", tostring(source_tier),
            "requested_tier", tostring(tier_number)
        }
    end

    -- If we get here, no capacity was available
    return {
        "acquired", "false",
        "error", "No capacity available",
        "requested_tier", tostring(tier_number)
    }
end

-- Script entry point
return main()