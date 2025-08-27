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
        String sub = null;
        try {
            sub = JwtUtility.getSubFromRestEvent(event);
            if (sub == null) return response(401, Map.of("message", "Unauthorized"));
            Map<String, String> pathParams = event.getPathParameters();
            String companyId = (pathParams != null) ? pathParams.get("company_id") : null;
            if (companyId == null || companyId.isEmpty()) {
                ResponseMessage message = new ResponseMessage(400,
                        "sorry, there was an error processing your request",
                        "company_id is missing!");
                return response(400,message);
            }
            List<CauseListItem> topReasonsToBoycottCompany = reasonPeopleAreBoycottingCompany(companyId);
            return response(200,topReasonsToBoycottCompany);
        } catch (Exception e) {
            System.out.println(e.getMessage() + " for user " + sub);
            return response(500,Map.of("error", "Unexpected server error: " + e.getMessage()) );
        }
    }
    private APIGatewayProxyResponseEvent response(int status, Object body) {
        String responseBody = null;
        try {
            responseBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(status)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(responseBody);
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