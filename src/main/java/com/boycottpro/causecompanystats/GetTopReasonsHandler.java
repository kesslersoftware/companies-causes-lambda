package com.boycottpro.causecompanystats;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.boycottpro.causecompanystats.model.CauseListItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class GetTopReasonsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "";
    private final DynamoDbClient dynamoDb;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GetTopReasonsHandler() {
        this.dynamoDb = DynamoDbClient.create();
    }

    public GetTopReasonsHandler(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            Map<String, String> pathParams = event.getPathParameters();
            String companyId = (pathParams != null) ? pathParams.get("company_id") : null;
            if (companyId == null || companyId.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("{\"error\":\"Missing company_id in path\"}");
            }
            List<CauseListItem> topReasonsToBoycottCompany = reasonPeopleAreBoycottingCompany(companyId);
            String responseBody = objectMapper.writeValueAsString(topReasonsToBoycottCompany);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"error\": \"Unexpected server error: " + e.getMessage() + "\"}");
        }
    }
    private List<CauseListItem> reasonPeopleAreBoycottingCompany(String companyId) {
        QueryRequest query = QueryRequest.builder()
                .tableName("cause_company_stats")
                .indexName("company_cause_stats_index")
                .keyConditionExpression("company_id = :cid")
                .expressionAttributeValues(Map.of(
                        ":cid", AttributeValue.fromS(companyId)
                ))
                .projectionExpression("cause_id, cause_desc, boycott_count")
                .scanIndexForward(false) // descending by boycott_count
                .limit(3) // get only top 3 causes
                .build();

        QueryResponse response = dynamoDb.query(query);

        return response.items().stream()
                .filter(item -> item.containsKey("boycott_count"))
                .map(item -> new CauseListItem(item.get("cause_id").s(),
                        item.get("cause_desc").s(),Integer.parseInt(item.get("boycott_count").n()) ))
                .collect(Collectors.toList());
    }
}