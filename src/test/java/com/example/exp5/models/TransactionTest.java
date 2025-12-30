package com.example.exp5.models;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    // 语句覆盖：测试正常对象的序列化。
    void testToTSV() {
        Transaction t = new Transaction("id1", LocalDate.of(2023, 10, 1), 100.5, "Food", "expense", "Lunch");
        String tsv = t.toTSV();
        // id, date, amount, category, type, desc, addedAt
        assertTrue(tsv.startsWith("id1\t2023-10-01\t100.5\tFood\texpense\tLunch"));
    }

    @Test
    // 语句覆盖：测试标准格式字符串的反序列化。
    void testFromTSV_Valid() {
        String line = "id1\t2023-10-01\t100.5\tFood\texpense\tLunch\t2023-10-01T12:00";
        Transaction t = Transaction.fromTSV(line);
        assertNotNull(t);
        assertEquals("id1", t.getId());
        assertEquals(LocalDate.of(2023, 10, 1), t.getDate());
        assertEquals(100.5, t.getAmount());
        assertEquals("Food", t.getCategory());
        assertEquals("expense", t.getType());
        assertEquals("Lunch", t.getDescription());
    }

    @Test
    // 边界测试 & 判定覆盖：测试非法格式字符串。覆盖 parts.length < 6 的异常分支。
    void testFromTSV_Invalid() {
        assertNull(Transaction.fromTSV("invalid"));
        assertNull(Transaction.fromTSV("a\tb\tc"));
    }

    @Test
    // 语句覆盖：测试 JSON 格式化。
    void testToJson() {
        Transaction t = new Transaction("id1", LocalDate.of(2023, 10, 1), 100.5, "Food", "expense", "Lunch");
        String json = t.toJson();
        assertTrue(json.contains("\"id\":\"id1\""));
        assertTrue(json.contains("\"amount\":100.5"));
    }
    
    @Test
    // 语句覆盖：测试从 Map 构建对象。
    void testFromMap() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "id2");
        map.put("date", "2023-11-11");
        map.put("amount", "50");
        map.put("category", "Transport");
        map.put("type", "expense");
        
        Transaction t = Transaction.fromMap(map);
        assertEquals("id2", t.getId());
        assertEquals(50.0, t.getAmount());
    }
}
