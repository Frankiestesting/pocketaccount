<script>
	import { onMount } from 'svelte';

	/**
	 * @typedef {Object} Account
	 * @property {string} id
	 * @property {string} name
	 * @property {string} accountNo
	 */
	/**
	 * @typedef {Object} Transaction
	 * @property {string} id
	 * @property {string} [bookingDate]
	 * @property {string} [description]
	 * @property {number} [amount]
	 * @property {string} [currency]
	 * @property {string} accountName
	 * @property {string} accountNo
	 */
	/**
	 * @typedef {Object} TransactionLinks
	 * @property {string} [statementTransactionId]
	 * @property {string} [receiptId]
	 */

	/** @type {Account[]} */
	let accounts = [];
	/** @type {Transaction[]} */
	let transactions = [];
	/** @type {Record<string, TransactionLinks | null>} */
	let linksById = {};
	let loading = true;
	/** @type {string|null} */
	let error = null;
	/** @type {Set<string>} */
	let expandedMonths = new Set();

	/** @type {Map<string, { transactions: Transaction[], income: number, expense: number, expenseCount: number, approvedExpenseCount: number }>} */
	let monthGroups = new Map();
	/** @type {string[]} */
	let monthKeys = [];

	/** @param {unknown} err */
	function getErrorMessage(err) {
		return err instanceof Error ? err.message : String(err);
	}

	onMount(async () => {
		await loadData();
	});

	async function loadData() {
		loading = true;
		error = null;
		transactions = [];
		linksById = {};

		try {
			const accountsRes = await fetch('/api/v1/accounts');
			if (!accountsRes.ok) {
				throw new Error(`Failed to fetch accounts: ${accountsRes.status}`);
			}
			accounts = await accountsRes.json();

			const perAccount = await Promise.all(
				accounts.map(async (account) => {
					const res = await fetch(`/api/v1/bank-transactions?accountId=${account.id}&page=0&size=200`);
					if (!res.ok) {
						throw new Error(`Failed to fetch transactions: ${res.status}`);
					}
					const rows = /** @type {Array<Record<string, unknown>>} */ (await res.json());
					return rows.map((tx) =>
						/** @type {Transaction} */ ({
							...tx,
							accountName: account.name,
							accountNo: account.accountNo
						})
					);
				})
			);

			transactions = perAccount.flat().sort((a, b) => {
				const aDate = a.bookingDate ? new Date(a.bookingDate).getTime() : 0;
				const bDate = b.bookingDate ? new Date(b.bookingDate).getTime() : 0;
				return bDate - aDate;
			});

			const linkEntries = await Promise.all(
				transactions.map(async (tx) => {
					const res = await fetch(`/api/v1/bank-transactions/${tx.id}/links`);
					if (!res.ok) {
						return [tx.id, null];
					}
					const links = await res.json();
					return [tx.id, links];
				})
			);

			linksById = Object.fromEntries(linkEntries);
		} catch (err) {
			error = getErrorMessage(err);
		} finally {
			loading = false;
		}
	}

	$: monthGroups = buildMonthGroups(transactions, linksById);
	$: monthKeys = Array.from(monthGroups.keys()).sort((a, b) => {
		if (a === 'unknown') return 1;
		if (b === 'unknown') return -1;
		return b.localeCompare(a);
	});

	/** @param {string|undefined} dateString */
	function formatDate(dateString) {
		if (!dateString) return '-';
		const date = new Date(dateString);
		if (Number.isNaN(date.getTime())) return dateString;
		return date.toLocaleDateString('nb-NO');
	}

	/** @param {number|undefined|null} amount */
	function formatAmount(amount) {
		if (amount === null || amount === undefined) return '-';
		return Number(amount).toLocaleString('nb-NO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
	}

	/** @param {string|undefined} dateString */
	function getMonthKey(dateString) {
		if (!dateString) return 'unknown';
		const date = new Date(dateString);
		if (Number.isNaN(date.getTime())) return 'unknown';
		const year = date.getFullYear();
		const month = String(date.getMonth() + 1).padStart(2, '0');
		return `${year}-${month}`;
	}

	/** @param {string} monthKey */
	function formatMonthLabel(monthKey) {
		if (monthKey === 'unknown') return 'Ukjent måned';
		const [year, month] = monthKey.split('-').map(Number);
		const date = new Date(year, month - 1, 1);
		return date.toLocaleDateString('nb-NO', { month: 'long', year: 'numeric' });
	}

	/** @param {Transaction[]} rows */
	/** @param {Record<string, TransactionLinks | null>} linkMap */
	function buildMonthGroups(rows, linkMap) {
		const groups = new Map();
		for (const tx of rows) {
			const key = getMonthKey(tx.bookingDate);
			if (!groups.has(key)) {
				groups.set(key, {
					transactions: [],
					income: 0,
					expense: 0,
					expenseCount: 0,
					approvedExpenseCount: 0
				});
			}
			const group = groups.get(key);
			group.transactions.push(tx);
			const amount = Number(tx.amount ?? 0);
			if (amount >= 0) {
				group.income += amount;
			} else {
				group.expense += Math.abs(amount);
				group.expenseCount += 1;
				if (linkMap?.[tx.id]?.receiptId) {
					group.approvedExpenseCount += 1;
				}
			}
		}

		for (const group of groups.values()) {
			group.transactions.sort((a, b) => {
				const aDate = a.bookingDate ? new Date(a.bookingDate).getTime() : 0;
				const bDate = b.bookingDate ? new Date(b.bookingDate).getTime() : 0;
				return bDate - aDate;
			});
		}

		return groups;
	}

	/** @param {string} monthKey */
	function toggleMonth(monthKey) {
		const next = new Set(expandedMonths);
		if (next.has(monthKey)) {
			next.delete(monthKey);
		} else {
			next.add(monthKey);
		}
		expandedMonths = next;
	}

	function expandAllMonths() {
		expandedMonths = new Set(monthKeys);
	}

	function collapseAllMonths() {
		expandedMonths = new Set();
	}

	function areAllMonthsExpanded() {
		if (monthKeys.length === 0) {
			return false;
		}
		return monthKeys.every((key) => expandedMonths.has(key));
	}

	/** @param {string} txId */
	function getLinks(txId) {
		return linksById[txId] || null;
	}

	/** @param {string} txId */
	function hasBothLinks(txId) {
		const links = getLinks(txId);
		return Boolean(links?.statementTransactionId && links?.receiptId);
	}
</script>

<svelte:head>
	<title>Banktransaksjoner</title>
</svelte:head>

<div class="container">
	<h1>Banktransaksjoner</h1>
	<p>Alle banktransaksjoner med lenker til statement transactions og kvitteringer.</p>

	{#if loading}
		<div class="loading">Laster banktransaksjoner...</div>
	{:else if error}
		<div class="alert error">{error}</div>
	{:else if transactions.length === 0}
		<div class="alert info">Ingen banktransaksjoner funnet.</div>
	{:else}
		<div class="month-controls">
			<div class="summary">{monthKeys.length} måneder</div>
			<div class="controls">
				<button
					type="button"
					class="control-button"
					disabled={areAllMonthsExpanded()}
					on:click={expandAllMonths}
				>
					Ekspander alle
				</button>
				<button
					type="button"
					class="control-button secondary"
					disabled={expandedMonths.size === 0}
					on:click={collapseAllMonths}
				>
					Kollaps alle
				</button>
			</div>
		</div>
		<div class="month-list">
			{#each monthKeys as monthKey}
				{@const group = monthGroups.get(monthKey)}
				<button
					type="button"
					class="month-toggle"
					aria-expanded={expandedMonths.has(monthKey)}
					on:click={() => toggleMonth(monthKey)}
				>
					<div class="month-title">
						<div class="month-title-row">
							{#if group.expenseCount > 0 && group.approvedExpenseCount === group.expenseCount}
								<span class="status-check" aria-label="Alle utgifter har godkjent kvittering">
									✓
								</span>
							{/if}
							<span class="month-label">{formatMonthLabel(monthKey)}</span>
						</div>
						<span class="month-count">{group.transactions.length} transaksjoner</span>
					</div>
					<div class="month-sums">
						<span class="income">Inntekter: {formatAmount(group.income)}</span>
						<span class="expense">Utgifter: {formatAmount(group.expense)}</span>
						<span class="expense-count">
							Utgifter godkjent: {group.approvedExpenseCount}/{group.expenseCount}
						</span>
					</div>
					<span class="month-state" aria-hidden="true">
						{expandedMonths.has(monthKey) ? '−' : '+'}
					</span>
				</button>

				{#if expandedMonths.has(monthKey)}
					<table class="transactions-table">
						<thead>
							<tr>
								<th>Dato</th>
								<th>Beskrivelse</th>
								<th>Belop</th>
								<th>Valuta</th>
								<th>Konto</th>
								<th>Statement</th>
								<th>Kvittering</th>
							</tr>
						</thead>
						<tbody>
							{#each group.transactions as tx}
								{@const links = getLinks(tx.id)}
								<tr class:row-linked={hasBothLinks(tx.id)}>
									<td>{formatDate(tx.bookingDate)}</td>
									<td>{tx.description || '-'}</td>
									<td class:negative={(tx.amount ?? 0) < 0}>{formatAmount(tx.amount)}</td>
									<td>{tx.currency || '-'}</td>
									<td>{tx.accountName} ({tx.accountNo})</td>
									<td>
										{#if links?.statementTransactionId}
											<a href={`/statement-transaction/${links.statementTransactionId}`} class="link">
												{links.statementTransactionId}
											</a>
										{:else}
											<span class="muted">-</span>
										{/if}
									</td>
									<td>
										{#if links?.receiptId}
											<a href={`/receipt/${links.receiptId}`} class="link">
												{links.receiptId}
											</a>
										{:else}
											<span class="muted">-</span>
										{/if}
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				{/if}
			{/each}
		</div>
	{/if}
</div>

<style>
	.container {
		max-width: 1200px;
	}

	h1 {
		color: #2c3e50;
		margin-bottom: 10px;
	}

	p {
		color: #5b6b7b;
		margin-bottom: 16px;
	}

	.loading {
		text-align: center;
		padding: 32px;
		color: #666;
	}

	.alert {
		margin-top: 16px;
		padding: 12px 14px;
		border-radius: 6px;
	}

	.alert.error {
		background: #fee;
		border: 1px solid #fcc;
		color: #c33;
	}

	.alert.info {
		background: #eef6ff;
		border: 1px solid #cfe4ff;
		color: #28527a;
	}

	.transactions-table {
		width: 100%;
		border-collapse: collapse;
		margin-top: 12px;
		background: #ffffff;
		border-radius: 12px;
		overflow: hidden;
		box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
	}

	.month-list {
		display: flex;
		flex-direction: column;
		gap: 16px;
		margin-top: 12px;
	}

	.month-controls {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		padding: 12px 14px;
		border-radius: 10px;
		background: linear-gradient(135deg, #f5f8ff, #eef2ff);
		border: 1px solid #e1e7f0;
	}

	.month-controls .summary {
		font-weight: 600;
		color: #1f3b63;
	}

	.month-controls .controls {
		display: flex;
		gap: 10px;
	}

	.control-button {
		border: none;
		border-radius: 8px;
		padding: 8px 12px;
		font-weight: 600;
		cursor: pointer;
		background: #1f3b63;
		color: #fff;
		transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
		box-shadow: 0 6px 16px rgba(31, 59, 99, 0.18);
	}

	.control-button.secondary {
		background: #f1f5f9;
		color: #1f3b63;
		box-shadow: none;
		border: 1px solid #d7e2f3;
	}

	.control-button:hover:enabled {
		transform: translateY(-1px);
		background: #173153;
	}

	.control-button.secondary:hover:enabled {
		background: #e2e8f0;
	}

	.control-button:disabled {
		cursor: not-allowed;
		opacity: 0.6;
	}

	.month-toggle {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16px;
		padding: 16px 18px;
		border-radius: 12px;
		border: 1px solid #e1e7f0;
		background: #ffffff;
		cursor: pointer;
		font-size: 16px;
		font-weight: 600;
		text-align: left;
		box-shadow: 0 10px 24px rgba(30, 64, 175, 0.08);
		transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
	}

	.month-toggle:hover {
		transform: translateY(-1px);
		border-color: #c7d7f5;
		box-shadow: 0 12px 30px rgba(30, 64, 175, 0.12);
	}

	.month-title {
		display: flex;
		flex-direction: column;
		gap: 4px;
		min-width: 220px;
	}

	.month-title-row {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.status-check {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 22px;
		height: 22px;
		border-radius: 50%;
		background: #1b7f3a;
		color: #fff;
		font-size: 14px;
		font-weight: 700;
		box-shadow: 0 6px 12px rgba(27, 127, 58, 0.24);
	}

	.month-label {
		text-transform: capitalize;
		font-size: 17px;
		color: #1f3b63;
	}

	.month-count {
		font-size: 13px;
		font-weight: 500;
		color: #64748b;
	}

	.month-sums {
		display: grid;
		gap: 6px 16px;
		grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
		font-weight: 500;
		font-size: 14px;
		color: #334155;
	}

	.month-sums .income {
		color: #1b7f3a;
	}

	.month-sums .expense {
		color: #b91c1c;
	}

	.expense-count {
		color: #64748b;
		font-size: 13px;
	}

	.month-state {
		font-size: 24px;
		font-weight: 700;
		color: #1f3b63;
	}

	.transactions-table th,
	.transactions-table td {
		text-align: left;
		padding: 10px 12px;
		border-bottom: 1px solid #eee;
		vertical-align: top;
	}

	.transactions-table th {
		background: #f1f5f9;
		color: #1f3b63;
		font-weight: 600;
		font-size: 13px;
		letter-spacing: 0.02em;
		text-transform: uppercase;
	}

	.transactions-table tbody tr:nth-child(even) {
		background: #f8fafc;
	}

	.transactions-table tbody tr:hover {
		background: #e8f0ff;
	}

	.negative {
		color: #c0392b;
	}

	.link {
		color: #2c7be5;
		text-decoration: none;
	}

	.link:hover {
		text-decoration: underline;
	}

	.muted {
		color: #888;
	}

	.transactions-table tbody tr.row-linked,
	.transactions-table tbody tr.row-linked:nth-child(even) {
		background: #cbf3d5;
	}

	.transactions-table tbody tr.row-linked:hover {
		background: #b6e9c5;
	}
</style>
