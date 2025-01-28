-- KEYS[1] - the Redis hash key representing the capacity store
-- ARGV[1] - the tier number to release capacity back to
-- Returns: table with release status and new capacity value

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

-- Main script execution:

local function main()
    -- Input validation
    local is_valid, result = validate_inputs(KEYS[1], ARGV[1])
    if not is_valid then
        return {
            "released", "false",
            "error", result,
            "tier", ARGV[1]
        }
    end

    local capacity_key = KEYS[1]
    local tier_number = result

    -- Get current capacity and verify tier exists
    local current_capacity = redis.call('hget', capacity_key, tier_number)
    if current_capacity == nil then
        return {
            "released", "false",
            "error", "Tier " .. tier_number .. " does not exist in capacity store",
            "tier", tostring(tier_number)
        }
    end

    -- Release one unit of capacity back to the tier
    local new_capacity = redis.call('hincrby', capacity_key, tier_number, 1)

    return {
        "released", "true",
        "tier", tostring(tier_number),
        "previous_capacity", tostring(current_capacity),
        "new_capacity", tostring(new_capacity)
    }
end

-- Script entry point
return main()