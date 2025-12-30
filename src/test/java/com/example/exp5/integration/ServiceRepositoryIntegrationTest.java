package com.example.exp5.integration;

import com.example.exp5.models.PieData;
import com.example.exp5.models.Transaction;
import com.example.exp5.repository.AnalyticsService;
import com.example.exp5.repository.LocalDB;
import com.example.exp5.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceRepositoryIntegrationTest {

    private static final String TEST_DB_FILE = "test_service_repo.tsv";
    private TransactionRepository repo;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        // Ensure clean state
        new File(TEST_DB_FILE).delete();
        
        LocalDB db = new LocalDB(TEST_DB_FILE);
        repo = new TransactionRepository(db);
        service = new AnalyticsService(repo);
    }

    @AfterEach
    void tearDown() {
        new File(TEST_DB_FILE).delete();
    }

    @Test
    void testAddTransactionAndAnalyze() {
        // 1. Add transactions via Repository
        Transaction t1 = new Transaction("1", LocalDate.now(), 100.0, "Food", "expense", "Lunch");
        Transaction t2 = new Transaction("2", LocalDate.now(), 200.0, "Salary", "income", "Job");
        repo.addTransaction(t1);
        repo.addTransaction(t2);

        // 2. Verify Repository State
        List<Transaction> all = repo.listTransactions();
        assertEquals(2, all.size());

        // 3. Verify Analytics Service Integration
        // Check Pie Data
        List<PieData> pieData = service.pieByCategory();
        assertFalse(pieData.isEmpty());
        
        PieData foodData = pieData.stream().filter(p -> p.getLabel().equals("Food")).findFirst().orElse(null);
        assertNotNull(foodData);
        assertEquals(100.0, foodData.getValue());

        PieData salaryData = pieData.stream().filter(p -> p.getLabel().equals("Salary")).findFirst().orElse(null);
        assertNotNull(salaryData);
        assertEquals(200.0, salaryData.getValue());
    }
    
    @Test
    void testPersistenceIntegration() {
        // 1. Add data
        Transaction t1 = new Transaction("1", LocalDate.now(), 50.0, "Transport", "expense", "Bus");
        repo.addTransaction(t1);
        
        // 2. Re-initialize Repository (Simulate app restart)
        LocalDB newDb = new LocalDB(TEST_DB_FILE);
        TransactionRepository newRepo = new TransactionRepository(newDb);
        AnalyticsService newService = new AnalyticsService(newRepo);
        
        // 3. Verify data persists and service works
        List<Transaction> loaded = newRepo.listTransactions();
        assertEquals(1, loaded.size());
        assertEquals("Transport", loaded.get(0).getCategory());
        
        List<PieData> pie = newService.pieByCategory();
        assertEquals(1, pie.size());
        assertEquals(50.0, pie.get(0).getValue());
    }
}
