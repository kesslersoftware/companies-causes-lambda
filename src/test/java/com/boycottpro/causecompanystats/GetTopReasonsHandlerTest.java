package com.boycottpro.causecompanystats;

import com.amazonaws.services.lambda.runtime.Context;
import com.boycottpro.causecompanystats.model.CauseListItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

@ExtendWith(MockitoExtension.class)
public class GetTopReasonsHandlerTest {

    @Mock
    private DynamoDbClient dynamoDb;

    @InjectMocks
    private GetTopReasonsHandler handler;

    @Mock
    private Context context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testValidCompanyIdReturnsTopReasons() throws Exception {
        String companyId = "123";
        Map<String, String> pathParams = Map.of("company_id", companyId);

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setPathParameters(pathParams);

        Map<String, AttributeValue> item1 = Map.of(
                "cause_id", AttributeValue.fromS("cause1"),
                "cause_desc", AttributeValue.fromS("Reason A"),
                "boycott_count", AttributeValue.fromN("20")
        );
        Map<String, AttributeValue> item2 = Map.of(
                "cause_id", AttributeValue.fromS("cause2"),
                "cause_desc", AttributeValue.fromS("Reason B"),
                "boycott_count", AttributeValue.fromN("10")
        );

        QueryResponse mockResponse = QueryResponse.builder()
                .items(List.of(item1, item2))
                .build();

        when(dynamoDb.query(argThat((QueryRequest r) ->
                r.tableName().equals("cause_company_stats") &&
                        r.indexName().equals("company_cause_stats_index") &&
                        r.keyConditionExpression().contains("company_id = :cid")
        ))).thenReturn(mockResponse);

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);

        assertEquals(200, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body.contains("Reason A"));
        assertTrue(body.contains("Reason B"));
    }

    @Test
    public void testMissingCompanyIdReturns400() {
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setPathParameters(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing company_id"));
    }

    @Test
    public void testExceptionReturns500() {
        String companyId = "error-case";
        Map<String, String> pathParams = Map.of("company_id", companyId);

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setPathParameters(pathParams);

        when(dynamoDb.query(any(QueryRequest.class))).thenThrow(new RuntimeException("DynamoDB failure"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Unexpected server error"));
    }
}
