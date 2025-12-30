package com.example.exp5.repository;

import com.example.exp5.models.PieData;
import com.example.exp5.models.TimeSeriesData;
import com.example.exp5.models.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TransactionRepository repo;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(repo);
    }

    @Test
    // 边界测试：输入为空列表。覆盖路径：空循环路径。
    void testPieByCategory_Empty() {
        when(repo.listTransactions()).thenReturn(Collections.emptyList());
        List<PieData> result = service.pieByCategory();
        assertTrue(result.isEmpty());
    }

    @Test
    // 语句覆盖 & 判定覆盖：测试包含支出和收入的混合数据。覆盖了 "expense" 判断的 True (t1, t2) 和 False (t3) 分支。
    void testPieByCategory_Mixed() {
        Transaction t1 = new Transaction("1", LocalDate.now(), 100.0, "Food", "expense", "Lunch");
        Transaction t2 = new Transaction("2", LocalDate.now(), 50.0, "Food", "expense", "Dinner");
        Transaction t3 = new Transaction("3", LocalDate.now(), 200.0, "Salary", "income", "Job");
        
        when(repo.listTransactions()).thenReturn(Arrays.asList(t1, t2, t3));
        
        List<PieData> result = service.pieByCategory();
        
        // Expect: Food -> 150.0, Salary -> 200.0
        assertEquals(2, result.size());
        
        PieData food = result.stream().filter(p -> p.getLabel().equals("Food")).findFirst().orElse(null);
        assertNotNull(food);
        assertEquals(150.0, food.getValue());
        
        PieData salary = result.stream().filter(p -> p.getLabel().equals("Salary")).findFirst().orElse(null);
        assertNotNull(salary);
        assertEquals(200.0, salary.getValue());
    }

    @Test
    // 边界测试：输入为空列表。
    void testTimeSeriesMonthly_Empty() {
        when(repo.listTransactions()).thenReturn(Collections.emptyList());
        List<TimeSeriesData> result = service.timeSeriesMonthly();
        assertTrue(result.isEmpty());
    }

    @Test
    // 语句覆盖 & 判定覆盖：测试不同月份的数据聚合。覆盖了 "expense" 判断分支。
    void testTimeSeriesMonthly_Mixed() {
        Transaction t1 = new Transaction("1", LocalDate.of(2023, 1, 15), 100.0, "Food", "expense", "Lunch");
        Transaction t2 = new Transaction("2", LocalDate.of(2023, 1, 20), 50.0, "Food", "expense", "Dinner");
        Transaction t3 = new Transaction("3", LocalDate.of(2023, 2, 10), 200.0, "Salary", "income", "Job");
        
        when(repo.listTransactions()).thenReturn(Arrays.asList(t1, t2, t3));
        
        List<TimeSeriesData> result = service.timeSeriesMonthly();
        
        // Expect: 2023-01 -> 150.0, 2023-02 -> 200.0
        assertEquals(2, result.size());
        
        TimeSeriesData jan = result.stream().filter(t -> t.getPeriod().equals("2023-01")).findFirst().orElse(null);
        assertNotNull(jan);
        assertEquals(150.0, jan.getTotal());
        
        TimeSeriesData feb = result.stream().filter(t -> t.getPeriod().equals("2023-02")).findFirst().orElse(null);
        assertNotNull(feb);
        assertEquals(200.0, feb.getTotal());
    }
}
