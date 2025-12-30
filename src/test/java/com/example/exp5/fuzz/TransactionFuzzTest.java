package com.example.exp5.fuzz;

import com.example.exp5.models.Transaction;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class TransactionFuzzTest {

    @Fuzz
    public void testFromTSV(String input) {
        // JQF will automatically generate various random string inputs
        // We construct a string that definitely has enough fields, but the date field is fuzzed
        String fuzzInput = "id\t" + input + "\t100\tcat\ttype\tdesc";
        Transaction.fromTSV(fuzzInput);
    }
}
