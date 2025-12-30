package com.example.exp5.repository;

import com.example.exp5.models.Transaction;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LocalDB {
	private final File file;

	public LocalDB() {
		this("transactions.tsv");
	}

	public LocalDB(String filename) {
		this.file = new File(filename);
		try {
			if (!this.file.exists()) {
				this.file.createNewFile();
			}
		} catch (IOException ignored) {
		}
	}

	public synchronized List<Transaction> loadAll() {
		List<Transaction> out = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(this.file), "UTF-8"))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) continue;
				Transaction t = Transaction.fromTSV(line);
				if (t != null) out.add(t);
			}
		} catch (IOException ignored) {
		}
		return out;
	}

	public synchronized void saveAll(List<Transaction> list) {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.file, false), "UTF-8"))) {
			for (Transaction t : list) {
				bw.write(t.toTSV());
				bw.newLine();
			}
		} catch (IOException ignored) {
		}
	}

	public synchronized void append(Transaction t) {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.file, true), "UTF-8"))) {
			bw.write(t.toTSV());
			bw.newLine();
		} catch (IOException ignored) {
		}
	}
}
