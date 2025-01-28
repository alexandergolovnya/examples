package com.example;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.Base64;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class LoadTestSimulation extends Simulation {

    private final String BASE_URL = "http://localhost:8090/api/1.0";

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL);

    private String basicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    private ScenarioBuilder clientTier1Scenario = scenario("Client Tier 1")
            .during(Duration.ofSeconds(30))
            .on(
                    exec(http("Client Tier 1 Request")
                            .get("/example")
                            .header("Authorization", basicAuthHeader("test_client_tier_1", "testpassword"))
                    )
            );

    private ScenarioBuilder clientTier2Scenario = scenario("Client Tier 2")
            .during(Duration.ofSeconds(30))
            .on(
                    exec(http("Client Tier 2 Request")
                            .get("/example")
                            .header("Authorization", basicAuthHeader("test_client_tier_2", "testpassword"))
                    )
            );

    private ScenarioBuilder clientTier3Scenario = scenario("Client Tier 3 with Spike")
            .during(Duration.ofSeconds(30))
            .on(
                    exec(http("Client Tier 3 Request")
                            .get("/example")
                            .header("Authorization", basicAuthHeader("test_client_tier_3", "testpassword"))
                    )
            );

    {
        setUp(
//                clientTier3Scenario.injectOpen(
//                        incrementUsersPerSec(1)
//                                .times(4)
//                                .eachLevelLasting(Duration.ofSeconds(14))
//                                .separatedByRampsLasting(Duration.ofSeconds(1)
//                                .startingFrom(1)
//                ).protocols(httpProtocol),

                clientTier3Scenario.injectOpen(constantUsersPerSec(5).during(Duration.ofSeconds(30))
                ).protocols(httpProtocol),
                clientTier1Scenario.injectOpen(rampUsers(5).during(Duration.ofSeconds(30))
                ).protocols(httpProtocol),
                clientTier2Scenario.injectOpen(rampUsers(5).during(Duration.ofSeconds(30))
                ).protocols(httpProtocol)
        );
    }
}