package com.example.exp5.repository;

import com.example.exp5.models.Transaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionRepository {
	private final LocalDB db;
	private final List<Transaction> cache;

	public TransactionRepository() {
		this(new LocalDB());
	}

	public TransactionRepository(LocalDB db) {
		this.db = db;
		this.cache = new ArrayList<>(db.loadAll());
		// 保持最新的在最前面
		this.cache.sort(Comparator.comparing(Transaction::getDate).reversed());
	}

	public synchronized List<Transaction> listTransactions() {
		return new ArrayList<>(cache);
	}

	public synchronized void addTransaction(Transaction t) {
		cache.add(0, t);
		db.append(t);
	}

	public synchronized void saveAll(List<Transaction> all) {
		cache.clear(); cache.addAll(all);
		db.saveAll(cache);
	}

	public synchronized boolean deleteTransaction(String id) {
		boolean removed = cache.removeIf(t -> t.getId() != null && t.getId().equals(id));
		if (removed) {
			db.saveAll(cache);
		}
		return removed;
	}
}
