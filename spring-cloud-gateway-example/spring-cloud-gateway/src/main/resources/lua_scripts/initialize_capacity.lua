--[[
Initialize Dynamic Capacities
KEYS[1]: the Redis hash key where capacities are stored
KEYS[2]: the Redis hash key where default capacity values are stored
ARGV: array of alternating tier and capacity values [tier1, cap1, tier2, cap2, ...]
--]]

-- Capacity keys
local CAPACITY_KEY = KEYS[1]
local DEFAULT_CAPACITY_KEY = KEYS[2]

-- Local functions:

-- Validates the number of (tier, capacity) pairs.
-- Checks if we have at least one tier-capacity pair and even number of arguments
local function validate_argument_pairs()
    if #ARGV < 2 or #ARGV % 2 ~= 0 then
        return false, "Invalid number of arguments. Must be pairs of (tier, capacity)."
    end
    return true, nil
end

-- Validates a single tier-capacity pair is numeric and positive
local function validate_tier_and_capacity(tier, capacity)
    -- Check if both values can be converted to numbers
    if not tonumber(tier) or not tonumber(capacity) then
        return false, "Tier or capacity is not a valid number"
    end

    -- Check if both values are positive
    if tonumber(tier) <= 0 or tonumber(capacity) < 0 then
        return false, "Tier and capacity must be positive"
    end

    return true, nil
end

-- Iterates through ARGV in pairs (tier, capacity) to validate them
local function validate_tier_capacity_list()
    for i = 1, #ARGV, 2 do
        local tier = ARGV[i]
        local capacity = ARGV[i + 1]

        -- Validate each pair before setting
        local is_valid, error_message = validate_tier_and_capacity(tier, capacity)
        if not is_valid then
            return false, error_message
        end
    end

    -- All tier-capacity pairs are valid numbers
    return true, nil
end

-- Helper to parse ARGV into a map { [tier] = capacity }
local function build_input_map()
    local input_map = {}
    for i = 1, #ARGV, 2 do
        local tier = ARGV[i]
        local capacity = ARGV[i + 1]
        input_map[tier] = tonumber(capacity)
    end
    return input_map
end

-- Clears and sets new capacities in a hash for the given key
-- It assumes that the validation of all tier-capacity pairs has already been done
-- and all pairs are valid numbers
local function set_capacities(capacity_hash_key, capacities_map)
    -- Clear existing capacities
    redis.call('del', capacity_hash_key)

    -- Iterate through arguments in pairs (tier, capacity) and set each tier
    for tier, capacity in pairs(capacities_map) do
        redis.call('hset', capacity_hash_key, tier, tostring(capacity))
    end

    return true, nil
end

-- Retrieves a capacities hash as a Lua table { [tier] = capacity }
local function get_capacities(capacity_hash_key)
    local raw_list = redis.call('hgetall', capacity_hash_key)
    local result = {}

    for i = 1, #raw_list, 2 do
        local tier = raw_list[i]
        local capacity = tonumber(raw_list[i + 1])
        result[tier] = capacity
    end

    return result
end

-- Update the current "live" capacities with the adjusted values, accounting
-- for the in-flight acquire and release operations
-- in_flight_diff = current_capacity_value - old_default_capacity_value
-- new_live_capacity  = new_default_capacity + in_flight_diff
local function update_current_capacities(input_map)
    -- Maps with current and default capacities operations tier
    local current_map = get_capacities(CAPACITY_KEY)
    local default_map = get_capacities(DEFAULT_CAPACITY_KEY)

    -- Map to store the updated capacities
    local updated_map = {}

    -- Calculate in-flight differences and new live capacities
    for tier, new_default_capacity in pairs(input_map) do
        local current_capacity_value = current_map[tier] or 0
        local old_default_capacity_value = default_map[tier] -- may be nil

        local in_flight_diff

        if old_default_capacity_value == nil then
            -- No old default value => assume no in-flight
            in_flight_diff = 0
        else
            in_flight_diff = current_capacity_value - old_default_capacity_value
        end

        local new_live_capacity = new_default_capacity + in_flight_diff
        updated_map[tier] = new_live_capacity
    end

    -- Overwrite the live capacities with the new values
    set_capacities(CAPACITY_KEY, updated_map)

    -- Update the default capacities with the brand-new values
    set_capacities(DEFAULT_CAPACITY_KEY, input_map)
end

-- Main script execution:

local function main()
    -- Validate argument structure
    local is_valid, error_message = validate_argument_pairs()
    if not is_valid then
        return {
            "success", "false",
            "error", error_message,
            "capacities", {}
        }
    end

    is_valid, error_message = validate_tier_capacity_list()
    if not is_valid then
        return {
            "success", "false",
            "error", error_message,
            "capacities", {}
        }
    end

    -- Parse ARGV into a map
    local input_map = build_input_map()

    -- Check if environment is brand new
    local current_map = get_capacities(CAPACITY_KEY)
    local default_map = get_capacities(DEFAULT_CAPACITY_KEY)
    local is_current_empty = (next(current_map) == nil)
    local is_default_empty = (next(default_map) == nil)

    if is_current_empty and is_default_empty then
        -- First run => no in-flight logic. We simply set to the new capacities.
        set_capacities(CAPACITY_KEY, input_map)
        set_capacities(DEFAULT_CAPACITY_KEY, input_map)
    else
        -- Normal update with in-flight logic
        update_current_capacities(input_map)
    end

    -- Fetch the final live capacities
    local final_cap_list = redis.call('hgetall', CAPACITY_KEY)

    -- Return success
    return {
        "success", "true",
        "capacities", final_cap_list,
        "error", ""
    }
end

-- Script entry point
return main()