package com.boycottpro.causecompanystats;

import com.amazonaws.services.lambda.runtime.Context;
import com.boycottpro.causecompanystats.model.CauseListItem;
import com.boycottpro.models.ResponseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> claims = Map.of("sub", "11111111-2222-3333-4444-555555555555");
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);

        APIGatewayProxyRequestEvent.ProxyRequestContext rc = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        rc.setAuthorizer(authorizer);
        event.setRequestContext(rc);
        event.setPathParameters(pathParams);

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

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(200, response.getStatusCode());
        String body = response.getBody();
        assertTrue(body.contains("Reason A"));
        assertTrue(body.contains("Reason B"));
    }

    @Test
    public void testMissingCompanyIdReturns400() throws JsonProcessingException {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> claims = Map.of("sub", "11111111-2222-3333-4444-555555555555");
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);

        APIGatewayProxyRequestEvent.ProxyRequestContext rc = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        rc.setAuthorizer(authorizer);
        event.setRequestContext(rc);
        event.setPathParameters(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        ResponseMessage message = objectMapper.readValue(response.getBody(), ResponseMessage.class);
        assertTrue(message.getMessage().contains("sorry, there was an error processing your request"));
    }

    @Test
    public void testExceptionReturns500() throws JsonProcessingException {
        String companyId = "error-case";
        Map<String, String> pathParams = Map.of("company_id", companyId);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> claims = Map.of("sub", "11111111-2222-3333-4444-555555555555");
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);

        APIGatewayProxyRequestEvent.ProxyRequestContext rc = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        rc.setAuthorizer(authorizer);
        event.setRequestContext(rc);
        event.setPathParameters(pathParams);

        when(dynamoDb.query(any(QueryRequest.class))).thenThrow(new RuntimeException("DynamoDB failure"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Unexpected server error"));
    }

    @Test
    public void testDefaultConstructor() {
        // Test the default constructor coverage
        // Note: This may fail in environments without AWS credentials/region configured
        try {
            GetTopReasonsHandler handler = new GetTopReasonsHandler();
            assertNotNull(handler);

            // Verify DynamoDbClient was created (using reflection to access private field)
            try {
                Field dynamoDbField = GetTopReasonsHandler.class.getDeclaredField("dynamoDb");
                dynamoDbField.setAccessible(true);
                DynamoDbClient dynamoDb = (DynamoDbClient) dynamoDbField.get(handler);
                assertNotNull(dynamoDb);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                fail("Failed to access DynamoDbClient field: " + e.getMessage());
            }
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            // AWS SDK can't initialize due to missing region configuration
            // This is expected in Jenkins without AWS credentials - test passes
            System.out.println("Skipping DynamoDbClient verification due to AWS SDK configuration: " + e.getMessage());
        }
    }

    @Test
    public void testUnauthorizedUser() {
        // Test the unauthorized block coverage
        handler = new GetTopReasonsHandler(dynamoDb);

        // Create event without JWT token (or invalid token that returns null sub)
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        // No authorizer context, so JwtUtility.getSubFromRestEvent will return null

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Unauthorized"));
    }

    @Test
    public void testJsonProcessingExceptionInResponse() throws Exception {
        // Test JsonProcessingException coverage in response method by using reflection
        handler = new GetTopReasonsHandler(dynamoDb);

        // Use reflection to access the private response method
        java.lang.reflect.Method responseMethod = GetTopReasonsHandler.class.getDeclaredMethod("response", int.class, Object.class);
        responseMethod.setAccessible(true);

        // Create an object that will cause JsonProcessingException
        Object problematicObject = new Object() {
            public Object writeReplace() throws java.io.ObjectStreamException {
                throw new java.io.NotSerializableException("Not serializable");
            }
        };

        // Create a circular reference object that will cause JsonProcessingException
        Map<String, Object> circularMap = new HashMap<>();
        circularMap.put("self", circularMap);

        // This should trigger the JsonProcessingException -> RuntimeException path
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            try {
                responseMethod.invoke(handler, 500, circularMap);
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e.getCause());
            }
        });

        // Verify it's ultimately caused by JsonProcessingException
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof JsonProcessingException,
                "Expected JsonProcessingException, got: " + cause.getClass().getSimpleName());
    }

    @Test
    public void testEmptyCompanyIdReturns400() throws JsonProcessingException {
        // Test the branch where company_id is empty string (line 47: companyId.isEmpty())
        Map<String, String> pathParams = Map.of("company_id", "");
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> claims = Map.of("sub", "11111111-2222-3333-4444-555555555555");
        Map<String, Object> authorizer = new HashMap<>();
        authorizer.put("claims", claims);

        APIGatewayProxyRequestEvent.ProxyRequestContext rc = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        rc.setAuthorizer(authorizer);
        event.setRequestContext(rc);
        event.setPathParameters(pathParams);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        ResponseMessage message = objectMapper.readValue(response.getBody(), ResponseMessage.class);
        assertTrue(message.getMessage().contains("sorry, there was an error processing your request"));
    }

    @Test
    public void testCauseListItemModel() {
        // Test the CauseListItem model class to achieve 100% coverage
        CauseListItem item = new CauseListItem();
        item.setCause_id("test-id");
        item.setCause_desc("test-desc");
        item.setBoycott_count(42);

        assertEquals("test-id", item.getCause_id());
        assertEquals("test-desc", item.getCause_desc());
        assertEquals(42, item.getBoycott_count());

        CauseListItem item2 = new CauseListItem("id2", "desc2", 100);
        assertEquals("id2", item2.getCause_id());
        assertEquals("desc2", item2.getCause_desc());
        assertEquals(100, item2.getBoycott_count());
    }

}
