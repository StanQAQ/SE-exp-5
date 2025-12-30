package com.example.exp5.integration;

import com.example.exp5.MainApplication;
import com.example.exp5.models.Transaction;
import com.example.exp5.repository.LocalDB;
import com.example.exp5.repository.TransactionRepository;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class APIHandlerIntegrationTest {

    private static final String TEST_DB_FILE = "test_api_repo.tsv";
    private TransactionRepository repo;
    private HttpHandler transactionsHandler;

    @BeforeEach
    void setUp() throws Exception {
        new File(TEST_DB_FILE).delete();
        LocalDB db = new LocalDB(TEST_DB_FILE);
        repo = new TransactionRepository(db);
        
        // Instantiate the private inner class TransactionsHandler via reflection or just assume it's package-private if modified.
        // Since MainApplication.TransactionsHandler is static package-private (default), we can instantiate it if we are in the same package.
        // But we are in com.example.exp5.integration, so we need to use reflection or move the test.
        // Actually, MainApplication.TransactionsHandler is 'static class', and package-private.
        // To make testing easier, I'll use reflection to create the instance.
        
        Class<?> clazz = Class.forName("com.example.exp5.MainApplication$TransactionsHandler");
        Constructor<?> ctor = clazz.getDeclaredConstructor(TransactionRepository.class);
        ctor.setAccessible(true);
        transactionsHandler = (HttpHandler) ctor.newInstance(repo);
    }

    @AfterEach
    void tearDown() {
        new File(TEST_DB_FILE).delete();
    }

    @Test
    void testPostAndGetTransactionFlow() throws IOException {
        // 1. Simulate POST request to add transaction
        HttpExchange postExchange = mock(HttpExchange.class);
        when(postExchange.getRequestMethod()).thenReturn("POST");
        
        String formData = "id=101&date=2023-12-01&amount=500&category=Bonus&type=income&desc=YearEnd";
        ByteArrayInputStream reqBody = new ByteArrayInputStream(formData.getBytes(StandardCharsets.UTF_8));
        when(postExchange.getRequestBody()).thenReturn(reqBody);
        
        Headers headers = new Headers();
        when(postExchange.getResponseHeaders()).thenReturn(headers);
        ByteArrayOutputStream postResp = new ByteArrayOutputStream();
        when(postExchange.getResponseBody()).thenReturn(postResp);

        transactionsHandler.handle(postExchange);

        // Verify POST response
        verify(postExchange).sendResponseHeaders(eq(200), anyLong());
        String postOutput = postResp.toString("UTF-8");
        assertTrue(postOutput.contains("\"status\":\"ok\""));

        // 2. Verify Data in Repository
        List<Transaction> list = repo.listTransactions();
        assertEquals(1, list.size());
        assertEquals("Bonus", list.get(0).getCategory());

        // 3. Simulate GET request to retrieve data
        HttpExchange getExchange = mock(HttpExchange.class);
        when(getExchange.getRequestMethod()).thenReturn("GET");
        when(getExchange.getRequestURI()).thenReturn(java.net.URI.create("/api/transactions"));
        
        when(getExchange.getResponseHeaders()).thenReturn(new Headers());
        ByteArrayOutputStream getResp = new ByteArrayOutputStream();
        when(getExchange.getResponseBody()).thenReturn(getResp);

        transactionsHandler.handle(getExchange);

        // Verify GET response
        verify(getExchange).sendResponseHeaders(eq(200), anyLong());
        String jsonOutput = getResp.toString("UTF-8");
        assertTrue(jsonOutput.contains("\"category\":\"Bonus\""));
        assertTrue(jsonOutput.contains("\"amount\":500.0"));
    }
}
