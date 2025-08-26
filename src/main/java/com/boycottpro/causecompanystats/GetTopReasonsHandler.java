package com.boycottpro.causecompanystats;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.boycottpro.causecompanystats.model.CauseListItem;
import com.boycottpro.models.ResponseMessage;
import com.boycottpro.utilities.JwtUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class GetTopReasonsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "cause_company_stats";
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
            String sub = JwtUtility.getSubFromRestEvent(event);
            if (sub == null) return response(401, "Unauthorized");
            Map<String, String> pathParams = event.getPathParameters();
            String companyId = (pathParams != null) ? pathParams.get("company_id") : null;
            if (companyId == null || companyId.isEmpty()) {
                ResponseMessage message = new ResponseMessage(400,
                        "sorry, there was an error processing your request",
                        "company_id is missing!");
                String responseBody = objectMapper.writeValueAsString(message);
                return response(400,responseBody);
            }
            List<CauseListItem> topReasonsToBoycottCompany = reasonPeopleAreBoycottingCompany(companyId);
            String responseBody = objectMapper.writeValueAsString(topReasonsToBoycottCompany);
            return response(200,responseBody);
        } catch (Exception e) {
            ResponseMessage message = new ResponseMessage(500,
                    "sorry, there was an error processing your request",
                    "Unexpected server error: " + e.getMessage());
            String responseBody = null;
            try {
                responseBody = objectMapper.writeValueAsString(message);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
            return response(500,responseBody);
        }
    }
    private APIGatewayProxyResponseEvent response(int status, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }
    private List<CauseListItem> reasonPeopleAreBoycottingCompany(String companyId) {
        QueryRequest query = QueryRequest.builder()
                .tableName(TABLE_NAME)
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