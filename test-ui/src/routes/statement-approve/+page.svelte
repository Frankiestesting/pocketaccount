<script>
	import { onMount } from 'svelte';

	/**
	 * @typedef {Object} Job
	 * @property {string} jobId
	 * @property {string} documentId
	 * @property {string} [documentType]
	 * @property {string} [status]
	 * @property {string} [originalFilename]
	 * @property {string} [finishedAt]
	 */
	/**
	 * @typedef {Object} StatementTransaction
	 * @property {string|number} [id]
	 * @property {string|number} [statementTransactionId]
	 * @property {string|number} [transactionId]
	 * @property {string} [date]
	 * @property {string} [description]
	 * @property {string} [currency]
	 * @property {number} [amount]
	 * @property {string} [accountNo]
	 * @property {boolean} [approved]
	 */
	/**
	 * @typedef {Object} Account
	 * @property {string} id
	 * @property {string} accountNo
	 * @property {string} currency
	 * @property {string} name
	 */

	/** @type {Job[]} */
	let jobs = [];
	let loading = true;
	/** @type {string|null} */
	let error = null;
	/** @type {Record<string, { total: number, approved: number, allApproved: boolean } | null>} */
	let jobStats = {};
	/** @type {Job|null} */
	let selectedJob = null;
	/** @type {StatementTransaction[]|null} */
	let transactions = null;
	/** @type {Account[]} */
	let accounts = [];
	/** @type {string|null} */
	let resultError = null;
	/** @type {string|null} */
	let approveError = null;
	/** @type {Set<string>} */
	let approvingIds = new Set();
	/** @type {Record<string, string>} */
	let selectedAccountByTxId = {};

	onMount(async () => {
		await Promise.all([loadJobs(), loadAccounts()]);
	});

	async function loadJobs() {
		loading = true;
		error = null;

		try {
			const res = await fetch('/api/v1/interpretation/jobs');
			if (!res.ok) {
				throw new Error(`Failed to fetch jobs: ${res.status}`);
			}
			const allJobs = /** @type {Job[]} */ (await res.json());
			jobs = allJobs
				.filter(
					(job) =>
						job.status &&
						job.status.toUpperCase() === 'COMPLETED' &&
						job.documentType &&
						job.documentType.toUpperCase() === 'STATEMENT'
				)
				.sort(
					(/** @type {Job} */ a, /** @type {Job} */ b) =>
						new Date(b.finishedAt || 0).getTime() - new Date(a.finishedAt || 0).getTime()
				);
			await loadJobStats(jobs);
		} catch (err) {
			error = `Error loading jobs: ${getErrorMessage(err)}`;
		} finally {
			loading = false;
		}
	}

	/** @param {Job[]} jobList */
	async function loadJobStats(jobList) {
		if (!jobList.length) {
			jobStats = {};
			return;
		}
		const entries = await Promise.all(
			jobList.map(async (job) => {
				try {
					const res = await fetch(`/api/v1/interpretation/jobs/${job.jobId}/statement-transactions`);
					if (!res.ok) {
						return [job.jobId, null];
					}
					const rows = /** @type {StatementTransaction[]} */ (await res.json());
					const total = Array.isArray(rows) ? rows.length : 0;
					const approved = Array.isArray(rows)
						? rows.filter((row) => Boolean(row?.approved)).length
						: 0;
					return [job.jobId, { total, approved, allApproved: total > 0 && approved === total }];
				} catch (err) {
					return [job.jobId, null];
				}
			})
		);
		jobStats = Object.fromEntries(entries);
	}

	/** @param {Job} job */
	function getJobStats(job) {
		return jobStats[job.jobId] || null;
	}

	/** @param {Job} job */
	function handleJobKeydown(job, event) {
		if (event.key === 'Enter' || event.key === ' ') {
			event.preventDefault();
			selectJob(job);
		}
	}

	/** @param {unknown} err */
	function getErrorMessage(err) {
		return err instanceof Error ? err.message : String(err);
	}

	async function loadAccounts() {
		try {
			const res = await fetch('/api/v1/accounts');
			if (!res.ok) {
				throw new Error(`Failed to fetch accounts: ${res.status}`);
			}
			accounts = await res.json();
		} catch (err) {
			accounts = [];
			console.error('Failed to load accounts', err);
		}
	}

	/** @param {Job} job */
	async function selectJob(job) {
		selectedJob = job;
		transactions = null;
		resultError = null;
		approveError = null;
		selectedAccountByTxId = {};

		try {
			const res = await fetch(`/api/v1/interpretation/jobs/${job.jobId}/statement-transactions`);
			if (res.ok) {
				const rows = await res.json();
				transactions = rows;
				selectedAccountByTxId = buildAccountSelections(rows);
			} else if (res.status === 404) {
				resultError = 'Resultat ikke funnet';
			} else {
				const errorText = await res.text();
				resultError = `Error: ${res.status} - ${errorText}`;
			}
		} catch (err) {
			resultError = `Network error: ${getErrorMessage(err)}`;
		}
	}

	function resetSelection() {
		selectedJob = null;
		transactions = null;
		resultError = null;
		approveError = null;
		selectedAccountByTxId = {};
	}

	/** @param {StatementTransaction} tx */
	function getTransactionId(tx) {
		const rawId = tx?.id ?? tx?.statementTransactionId ?? tx?.transactionId ?? null;
		return rawId == null ? null : String(rawId);
	}

	/** @param {StatementTransaction[]} rows */
	function buildAccountSelections(rows) {
		/** @type {Record<string, string>} */
		const selection = {};
		if (!Array.isArray(rows) || accounts.length === 0) {
			return selection;
		}
		for (const tx of rows) {
			const txId = getTransactionId(tx);
			if (!txId) {
				continue;
			}
			if (tx.accountNo) {
				const match = accounts.find((account) => account.accountNo === tx.accountNo);
				if (match) {
					selection[txId] = match.id;
				}
			}
		}
		return selection;
	}

	/** @param {StatementTransaction} tx */
	function getAccountOptions(tx) {
		if (!tx || accounts.length === 0) {
			return [];
		}
		const currency = tx.currency;
		const filtered = currency
			? accounts.filter((account) => account.currency === currency)
			: accounts;
		return filtered.length > 0 ? filtered : accounts;
	}

	/** @param {StatementTransaction} tx */
	async function approveTransaction(tx) {
		if (!tx || tx.approved) {
			return;
		}

		const txId = getTransactionId(tx);
		if (!txId) {
			approveError = 'Kan ikke godkjenne uten transaksjons-ID.';
			return;
		}

		const selectedAccountId = selectedAccountByTxId[txId] || null;
		if (!selectedAccountId && needsAccountSelection(tx)) {
			approveError = 'Velg en konto for aa godkjenne transaksjonen.';
			return;
		}

		const selectedAccount = selectedAccountId
			? accounts.find((account) => account.id === selectedAccountId) || null
			: null;

		approveError = null;
		approvingIds = new Set(approvingIds).add(txId);
		try {
			const res = await fetch(`/api/v1/interpretation/statement-transactions/${txId}/approve`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({
					accountId: selectedAccountId
				})
			});
			if (!res.ok) {
				const errorText = await res.text();
				approveError = `Godkjenning feilet: ${res.status} - ${errorText}`;
				return;
			}
			if (transactions) {
				transactions = transactions.map((item) =>
					getTransactionId(item) === txId
						? {
								...item,
								approved: true,
								accountNo: selectedAccount?.accountNo || item.accountNo
							}
						: item
				);
			}
		} catch (err) {
			approveError = `Network error: ${getErrorMessage(err)}`;
		} finally {
			const next = new Set(approvingIds);
			next.delete(txId);
			approvingIds = next;
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

	/** @param {StatementTransaction} tx */
	function isApproving(tx) {
		const txId = getTransactionId(tx);
		return txId ? approvingIds.has(txId) : false;
	}

	/** @param {StatementTransaction} tx */
	function needsAccountSelection(tx) {
		return !tx?.accountNo && accounts.length > 0;
	}
</script>

<svelte:head>
	<title>Godkjenn transaksjoner</title>
</svelte:head>

<div class="container">
	<h1>Godkjenn statement transactions</h1>
	<p>Velg en fullfort tolking og godkjenn statement transactions.</p>

	{#if loading}
		<div class="loading">Laster tolkingsjobber...</div>
	{:else if error}
		<div class="alert error">{error}</div>
	{:else if !selectedJob}
		<div class="jobs-list">
			<p class="info">Velg en jobb med status COMPLETED.</p>
			<table>
				<thead>
					<tr>
						<th>Dokument</th>
						<th>Type</th>
						<th>Filnavn</th>
						<th>Fullfort</th>
						<th>Transaksjoner</th>
						<th>Godkjent</th>
					</tr>
				</thead>
				<tbody>
					{#each jobs as job}
						{@const stats = getJobStats(job)}
						<tr
							class:job-complete={stats?.allApproved}
							on:click={() => selectJob(job)}
							on:keydown={(event) => handleJobKeydown(job, event)}
							role="button"
							tabindex="0"
						>
							<td>
								<div class="document-cell">
									{#if stats?.allApproved}
										<span class="status-check" aria-label="Alle transaksjoner godkjent">
											✓
										</span>
									{/if}
									<span>{job.originalFilename || job.documentId || '-'}</span>
								</div>
							</td>
							<td>{job.documentType || '-'}</td>
							<td>{job.originalFilename || '-'}</td>
							<td>{job.finishedAt ? new Date(job.finishedAt).toLocaleString('nb-NO') : '-'}</td>
							<td>{stats ? stats.total : '-'}</td>
							<td>{stats ? `${stats.approved}/${stats.total}` : '-'}</td>
						</tr>
					{:else}
						<tr>
							<td colspan="6" class="no-data">Ingen fullforte jobber funnet</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{:else}
		<div class="panel">
			<div class="panel-header">
				<h2>Statement transactions</h2>
				<button class="btn-secondary" on:click={resetSelection}>Tilbake</button>
			</div>
			<div class="job-meta">
				<p><strong>Jobb-ID:</strong> {selectedJob.jobId}</p>
				<p><strong>Dokument:</strong> {selectedJob.originalFilename || selectedJob.documentId}</p>
				<p><strong>Type:</strong> {selectedJob.documentType || '-'}</p>
			</div>

			{#if resultError}
				<div class="alert error">{resultError}</div>
			{:else if !transactions}
				<div class="loading">Laster transaksjoner...</div>
			{:else if transactions.length === 0}
				<div class="alert info">Ingen statement transactions funnet i dette resultatet.</div>
			{:else}
				{#if approveError}
					<div class="alert error">{approveError}</div>
				{/if}
				<table class="transactions-table">
					<thead>
						<tr>
							<th>Dato</th>
							<th>Beskrivelse</th>
							<th>Valuta</th>
							<th>Belop</th>
							<th>Konto</th>
							<th>Status</th>
							<th></th>
						</tr>
					</thead>
					<tbody>
						{#each transactions as tx}
							{@const txId = getTransactionId(tx) || ''}
							<tr data-transaction-id={txId}>
								<td>{formatDate(tx.date)}</td>
								<td>{tx.description || '-'}</td>
								<td>{tx.currency || '-'}</td>
								<td class:negative={(tx.amount ?? 0) < 0}>{formatAmount(tx.amount)}</td>
								<td>
									{#if tx.approved}
										<span class="account-label">{tx.accountNo || '-'}</span>
									{:else}
										<select class="account-select" bind:value={selectedAccountByTxId[txId]}>
											<option value="">Velg konto</option>
											{#each getAccountOptions(tx) as account}
												<option value={account.id}>
													{account.name} ({account.accountNo})
												</option>
											{/each}
										</select>
									{/if}
								</td>
								<td>
									<span class={tx.approved ? 'tag tag-approved' : 'tag tag-pending'}>
										{tx.approved ? 'Approved' : 'Pending'}
									</span>
								</td>
								<td class="action-cell">
									{#if !tx.approved}
										<button
											class="icon-button"
											on:click={() => approveTransaction(tx)}
											disabled={
												isApproving(tx) ||
												(needsAccountSelection(tx) && (!txId || !selectedAccountByTxId[txId]))
											}
											title="Godkjenn"
										>
											<svg viewBox="0 0 24 24" aria-hidden="true">
												<path d="M9.2 16.2L4.8 11.8l1.4-1.4 3 3 8-8 1.4 1.4z" />
											</svg>
										</button>
									{:else}
										<span class="done">OK</span>
									{/if}
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			{/if}
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

	.loading {
		text-align: center;
		padding: 32px;
		color: #666;
	}

	.info {
		color: #555;
		margin-bottom: 14px;
	}

	.panel {
		background: white;
		padding: 20px;
		border-radius: 8px;
		box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
		margin-top: 16px;
	}

	.panel-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 16px;
	}

	.job-meta {
		margin-top: 12px;
		color: #444;
	}

	.jobs-list table,
	.transactions-table {
		width: 100%;
		border-collapse: collapse;
		margin-top: 12px;
	}

	.jobs-list th,
	.jobs-list td,
	.transactions-table th,
	.transactions-table td {
		text-align: left;
		padding: 10px 12px;
		border-bottom: 1px solid #eee;
		vertical-align: top;
	}

	.account-select {
		min-width: 200px;
		padding: 6px 8px;
		border: 1px solid #d7dce2;
		border-radius: 6px;
		background: #fff;
	}

	.account-label {
		color: #4b5563;
		font-weight: 600;
		font-size: 13px;
	}

	.document-cell {
		display: flex;
		align-items: center;
		gap: 8px;
		font-weight: 600;
		color: #1f2937;
	}

	.status-check {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 22px;
		height: 22px;
		border-radius: 50%;
		background: #1f7a3e;
		color: #fff;
		font-size: 14px;
		font-weight: 700;
		box-shadow: 0 6px 12px rgba(31, 122, 62, 0.24);
	}

	.jobs-list tbody tr {
		cursor: pointer;
		transition: background 0.2s ease;
	}

	.jobs-list tbody tr:hover {
		background: #eef6ff;
	}

	.jobs-list tbody tr:focus-visible {
		outline: 2px solid #2c7be5;
		outline-offset: -2px;
	}

	.jobs-list tbody tr.job-complete {
		background: #e6f6ec;
	}

	.jobs-list tbody tr.job-complete:hover {
		background: #d7f0e1;
	}

	.link-button {
		background: none;
		border: none;
		color: #2c7be5;
		cursor: pointer;
		padding: 0;
		font: inherit;
		text-align: left;
	}

	.link-button:hover {
		text-decoration: underline;
	}

	.no-data {
		text-align: center;
		color: #777;
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

	.tag {
		display: inline-block;
		padding: 2px 8px;
		border-radius: 999px;
		font-size: 12px;
		font-weight: 600;
	}

	.tag-approved {
		background: #e6f6ec;
		color: #1f7a3e;
	}

	.tag-pending {
		background: #fff6e1;
		color: #8a5a00;
	}


	.action-cell {
		width: 64px;
	}

	.icon-button {
		background: #2c7be5;
		border: none;
		border-radius: 6px;
		width: 34px;
		height: 34px;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		transition: background 0.2s;
	}

	.icon-button:disabled {
		background: #9bbcf0;
		cursor: not-allowed;
	}

	.icon-button svg {
		width: 18px;
		height: 18px;
		fill: white;
	}

	.done {
		color: #1f7a3e;
		font-weight: 600;
	}

	.negative {
		color: #c0392b;
	}

	.btn-secondary {
		background: #f4f6f8;
		border: 1px solid #ccd2d9;
		color: #2c3e50;
		padding: 8px 12px;
		border-radius: 6px;
		cursor: pointer;
	}

	.btn-secondary:hover {
		background: #e9edf1;
	}
</style>
