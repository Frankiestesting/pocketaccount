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
				{#each transactions as tx}
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
</div>

<style>
	.container {
		max-width: 1200px;
	}

	h1 {
		color: #2c3e50;
		margin-bottom: 10px;
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
	}

	.transactions-table th,
	.transactions-table td {
		text-align: left;
		padding: 10px 12px;
		border-bottom: 1px solid #eee;
		vertical-align: top;
	}

	.transactions-table tbody tr:nth-child(even) {
		background: #f8fafc;
	}

	.transactions-table tbody tr:hover {
		background: #cfe1ff;
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
		background: #50C878;
	}

	.transactions-table tbody tr.row-linked:hover {
		background: #4F7942;
	}
</style>
